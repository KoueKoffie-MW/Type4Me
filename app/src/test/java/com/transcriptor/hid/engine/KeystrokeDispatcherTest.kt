package com.transcriptor.hid.engine

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeystrokeDispatcherTest {

    private val sentReports = mutableListOf<ByteArray>()
    private lateinit var translator: KeymapTranslator
    private lateinit var dispatcher: DefaultKeystrokeDispatcher

    @Before
    fun setUp() {
        sentReports.clear()
        translator = UsQwertyKeymap()
        dispatcher = DefaultKeystrokeDispatcher(
            translator = translator,
            deltaDiffEngine = DefaultDeltaDiffEngine(),
            reportSender = { report ->
                sentReports.add(report.copyOf())
                true
            }
        )
    }

    @Test
    fun testInitialStateIsEmpty() {
        assertEquals("", dispatcher.currentHostText.value)
    }

    @Test
    fun testDispatchBurstTransmitsDownAndUpReports() = runBlocking {
        dispatcher.dispatchBurst("Hi", delayMs = 0L)

        // "Hi" -> 2 characters -> 4 reports (H down, up, i down, up)
        assertEquals(4, sentReports.size)
        assertEquals("Hi", dispatcher.currentHostText.value)

        // Report 0: 'H' Down (Shift + KEY_H)
        assertEquals(HidConstants.MOD_LSHIFT, sentReports[0][0])
        assertEquals(HidConstants.KEY_H, sentReports[0][2])

        // Report 1: Key Release
        assertEquals(0.toByte(), sentReports[1][0])
        assertEquals(0.toByte(), sentReports[1][2])

        // Report 2: 'i' Down (None + KEY_I)
        assertEquals(HidConstants.MOD_NONE, sentReports[2][0])
        assertEquals(HidConstants.KEY_I, sentReports[2][2])

        // Report 3: Key Release
        assertEquals(0.toByte(), sentReports[3][0])
        assertEquals(0.toByte(), sentReports[3][2])
    }

    @Test
    fun testDispatchBurstLongText() = runBlocking {
        val longText = "a".repeat(500)
        dispatcher.dispatchBurst(longText, delayMs = 0L)

        assertEquals(longText, dispatcher.currentHostText.value)
        assertEquals(1000, sentReports.size) // 500 down + 500 up
    }

    @Test
    fun testDispatchLiveDiffFlow() = runBlocking {
        // Step 1: Initial hypothesis "Hello"
        dispatcher.dispatchLiveDiff("Hello", delayMs = 0L)
        assertEquals("Hello", dispatcher.currentHostText.value)
        assertEquals(10, sentReports.size) // 5 chars * 2 reports = 10

        sentReports.clear()

        // Step 2: Extended hypothesis "Hello world"
        dispatcher.dispatchLiveDiff("Hello world", delayMs = 0L)
        assertEquals("Hello world", dispatcher.currentHostText.value)
        assertEquals(12, sentReports.size) // " world" = 6 chars * 2 = 12

        sentReports.clear()

        // Step 3: Revised hypothesis "Hello earth"
        // LCP is "Hello ", deletes "world" (5 bs), types "earth" (5 chars)
        dispatcher.dispatchLiveDiff("Hello earth", delayMs = 0L)
        assertEquals("Hello earth", dispatcher.currentHostText.value)
        // 5 backspaces * 2 + 5 append chars * 2 = 20 reports
        assertEquals(20, sentReports.size)

        // First 10 reports must be backspaces (5 * (down + up))
        for (i in 0 until 5) {
            val downIndex = i * 2
            val upIndex = downIndex + 1
            assertEquals(HidConstants.MOD_NONE, sentReports[downIndex][0])
            assertEquals(HidConstants.KEY_BACKSPACE, sentReports[downIndex][2])
            assertEquals(0.toByte(), sentReports[upIndex][0])
            assertEquals(0.toByte(), sentReports[upIndex][2])
        }
    }

    @Test
    fun testDispatchLiveDiffClearText() = runBlocking {
        dispatcher.dispatchBurst("Testing", delayMs = 0L)
        assertEquals("Testing", dispatcher.currentHostText.value)
        sentReports.clear()

        dispatcher.dispatchLiveDiff("", delayMs = 0L)
        assertEquals("", dispatcher.currentHostText.value)
        assertEquals(14, sentReports.size) // 7 backspaces * 2 reports = 14
    }

    @Test
    fun testSendRawKeyStrokes() = runBlocking {
        val strokes = listOf(
            HidKeyStroke(HidConstants.MOD_LSHIFT, HidConstants.KEY_A),
            HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_B)
        )
        dispatcher.sendRawKeyStrokes(strokes, delayMs = 0L)

        assertEquals(4, sentReports.size)
        assertEquals(HidConstants.MOD_LSHIFT, sentReports[0][0])
        assertEquals(HidConstants.KEY_A, sentReports[0][2])
        assertEquals(HidConstants.MOD_NONE, sentReports[2][0])
        assertEquals(HidConstants.KEY_B, sentReports[2][2])
    }

    @Test
    fun testResetStateClearsHostText() = runBlocking {
        dispatcher.dispatchBurst("Something", delayMs = 0L)
        assertEquals("Something", dispatcher.currentHostText.value)

        dispatcher.resetState()
        assertEquals("", dispatcher.currentHostText.value)
    }

    @Test
    fun testConcurrentDispatchSerialized() = runBlocking {
        val deferreds = List(5) {
            async {
                dispatcher.dispatchBurst("A", delayMs = 1L)
            }
        }
        deferreds.awaitAll()

        assertEquals("AAAAA", dispatcher.currentHostText.value)
        assertEquals(10, sentReports.size)
    }

    @Test
    fun testGermanKeymapWithDispatcher() = runBlocking {
        dispatcher.translator = GermanQwertzKeymap()
        dispatcher.dispatchBurst("äöü", delayMs = 0L)

        assertEquals("äöü", dispatcher.currentHostText.value)
        assertEquals(6, sentReports.size) // 3 chars * 2 reports = 6
        assertEquals(HidConstants.KEY_APOSTROPHE, sentReports[0][2]) // ä
        assertEquals(HidConstants.KEY_SEMICOLON, sentReports[2][2])  // ö
        assertEquals(HidConstants.KEY_LEFTBRACE, sentReports[4][2])  // ü
    }
}
