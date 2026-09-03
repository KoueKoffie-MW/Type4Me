package com.transcriptor.hid.vision

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * High-performance on-device Screen Lens text extractor powered by Google ML Kit.
 * Extracts code, terminal logs, and error stack traces directly from camera frames or captured bitmaps.
 */
class ScreenLensAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts text from a captured [Bitmap] and applies spatial monospace indentation post-processing.
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): Result<String> = suspendCancellableCoroutine { cont ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val formatted = CodeOcrPostProcessor.process(visionText)
                cont.resume(Result.success(formatted))
            }
            .addOnFailureListener { exc ->
                cont.resume(Result.failure(exc))
            }
    }

    /**
     * Extracts text from a CameraX [ImageProxy] frame.
     */
    @OptIn(ExperimentalGetImage::class)
    suspend fun extractTextFromImageProxy(imageProxy: ImageProxy): Result<String> = suspendCancellableCoroutine { cont ->
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            cont.resume(Result.failure(IllegalArgumentException("Camera frame mediaImage is null.")))
            return@suspendCancellableCoroutine
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                imageProxy.close()
                val formatted = CodeOcrPostProcessor.process(visionText)
                cont.resume(Result.success(formatted))
            }
            .addOnFailureListener { exc ->
                imageProxy.close()
                cont.resume(Result.failure(exc))
            }
    }

    fun close() {
        recognizer.close()
    }
}
