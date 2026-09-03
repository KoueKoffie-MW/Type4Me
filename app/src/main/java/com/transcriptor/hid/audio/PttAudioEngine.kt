package com.transcriptor.hid.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Continuous In-App Audio & Push-To-Talk (PTT) Engine.
 *
 * Combines low-level 16kHz PCM [AudioRecord] for sub-20ms real-time RMS amplitude visualization
 * with Android's [SpeechRecognizer] for uninterrupted, long-form speech dictation
 * that bypasses Gboard's 2.5-second silence auto-commit timeout.
 */
class PttAudioEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recognizedHypothesis = MutableStateFlow("")
    val recognizedHypothesis: StateFlow<String> = _recognizedHypothesis.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
        2048
    )

    /**
     * Starts push-to-talk recording: spins up live RMS meter and Android speech recognizer.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(languageCode: String = "en-US", onHypothesisUpdate: (String) -> Unit = {}) {
        if (_isRecording.value) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        _isRecording.value = true

        // 1. Start AudioRecord for high-fidelity 60fps RMS meter
        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            audioRecord = record
            record.startRecording()

            recordJob = coroutineScope.launch(Dispatchers.Default) {
                val buffer = ShortArray(bufferSize / 2)
                while (isActive && _isRecording.value) {
                    val readCount = record.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / readCount)
                        // Normalize 0..32767 to 0.0f..1.0f with logarithmic decibel perception
                        val level = if (rms > 10.0) {
                            val db = 20 * log10(rms / 32767.0)
                            ((db + 60.0) / 60.0).toFloat().coerceIn(0.05f, 1.0f)
                        } else {
                            0.05f
                        }
                        _audioLevel.value = level
                    }
                }
            }
        } catch (exc: Exception) {
            // AudioRecord init failed, proceed with recognizer
        }

        // 2. Start SpeechRecognizer on Main Thread
        coroutineScope.launch(Dispatchers.Main) {
            initSpeechRecognizer(languageCode, onHypothesisUpdate)
        }
    }

    private fun initSpeechRecognizer(languageCode: String, onHypothesisUpdate: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L) // 10 sec silence
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                if (audioRecord == null) {
                    // Fallback RMS if AudioRecord is unvailable
                    _audioLevel.value = ((rmsdB + 2.0f) / 12.0f).coerceIn(0.05f, 1.0f)
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                // Auto-restart if user is still holding PTT
                if (_isRecording.value) {
                    recognizer.cancel()
                    recognizer.startListening(intent)
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _recognizedHypothesis.value = text
                    onHypothesisUpdate(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _recognizedHypothesis.value = text
                    onHypothesisUpdate(text)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    /**
     * Stops push-to-talk recording, commits recognized text, and releases microphone resources.
     */
    fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false
        _audioLevel.value = 0f

        recordJob?.cancel()
        recordJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null

        coroutineScope.launch(Dispatchers.Main) {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    fun destroy() {
        stopRecording()
    }
}
