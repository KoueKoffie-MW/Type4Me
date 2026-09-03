package com.transcriptor.hid.stress

import com.transcriptor.hid.engine.DefaultDeltaDiffEngine
import com.transcriptor.hid.engine.DefaultKeystrokeDispatcher
import com.transcriptor.hid.engine.GermanQwertzKeymap
import com.transcriptor.hid.engine.HidConstants
import com.transcriptor.hid.engine.HidKeyStroke
import com.transcriptor.hid.engine.KeymapTranslator
import com.transcriptor.hid.engine.NewlineSubmissionMode
import com.transcriptor.hid.engine.UsQwertyKeymap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class KeystrokeDispatcherStressTest {

    @Test
    fun testRapidConcurrentTypingMassiveCoroutines() = runBlocking {
        val sentReports = ConcurrentLinkedQueue<ByteArray>()
        val dispatcher = DefaultKeystrokeDispatcher(
            translator = UsQwertyKeymap(),
            deltaDiffEngine = DefaultDeltaDiffEngine(),
            reportSender = { report ->
                sentReports.add(report.copyOf())
                true
            }
        )

        val concurrentWorkers = 50
        val stringsPerWorker = 10

        val jobs = (1..concurrentWorkers).map { workerId ->
            async(Dispatchers.Default) {
                for (i in 1..stringsPerWorker) {
                    dispatcher.dispatchBurst("w$workerId-$i ", delayMs = 0L)
                }
            }
        }

        withTimeout(15000L) {
            jobs.awaitAll()
        }

        // Verify that all workers contributed to currentHostText and reports were captured
        val finalText = dispatcher.currentHostText.value
        for (workerId in 1..concurrentWorkers) {
            assertTrue("Expected text from worker $workerId", finalText.contains("w$workerId-"))
        }

        // Each burst produced valid reports (at least down and up per char)
        assertTrue(sentReports.size > 0)
    }

    @Test
    fun testRapidConcurrentTypingWithInterleavedResets() = runBlocking {
        val sentReports = ConcurrentLinkedQueue<ByteArray>()
        val dispatcher = DefaultKeystrokeDispatcher(
            translator = UsQwertyKeymap(),
            deltaDiffEngine = DefaultDeltaDiffEngine(),
            reportSender = { report ->
                sentReports.add(report.copyOf())
                true
            }
        )

        val stopFlag = AtomicBoolean(false)
        val resetCount = AtomicInteger(0)

        // Typing coroutines
        val typers = (1..10).map { id ->
            launch(Dispatchers.Default) {
                var counter = 0
                while (!stopFlag.get()) {
                    dispatcher.dispatchBurst("text-$id-${counter++} ", delayMs = 1L)
                    delay(2L)
                }
            }
        }

        // Reset state worker running concurrently on another thread
        val resetWorker = launch(Dispatchers.IO) {
            repeat(30) {
                delay(10L)
                dispatcher.resetState()
                resetCount.incrementAndGet()
            }
        }

        resetWorker.join()
        stopFlag.set(true)
        typers.forEach { it.join() }

        // Post-stress verification: state must not be corrupted or locked
        dispatcher.resetState()
        assertEquals("", dispatcher.currentHostText.value)

        dispatcher.dispatchBurst("PostStressValidation", delayMs = 0L)
        assertEquals("PostStressValidation", dispatcher.currentHostText.value)
    }

    @Test
    fun testDynamicKeymapAndNewlineDelayContention() = runBlocking {
        val dispatcher = DefaultKeystrokeDispatcher(
            translator = UsQwertyKeymap(),
            deltaDiffEngine = DefaultDeltaDiffEngine(),
            reportSender = { true }
        )

        val stopFlag = AtomicBoolean(false)

        // Coroutines rapidly mutating translator and newlineDelayMs
        val reconfigJob = launch(Dispatchers.Default) {
            var toggle = false
            while (!stopFlag.get()) {
                dispatcher.translator = if (toggle) {
                    GermanQwertzKeymap(NewlineSubmissionMode.CHAT_SOFT_ENTER)
                } else {
                    UsQwertyKeymap(NewlineSubmissionMode.TERMINAL_ENTER)
                }
                dispatcher.newlineDelayMs = if (toggle) 10L else 40L
                toggle = !toggle
                delay(1L)
            }
        }

        // Coroutines typing concurrently
        val typers = (1..10).map { id ->
            async(Dispatchers.Default) {
                repeat(20) {
                    dispatcher.dispatchBurst("line\n", delayMs = 0L)
                }
            }
        }

        typers.awaitAll()
        stopFlag.set(true)
        reconfigJob.join()

        assertNotNull(dispatcher.translator)
        assertTrue(dispatcher.currentHostText.value.isNotEmpty())
    }

    @Test
    fun testConcurrentBurstCancellationLeavesZeroRelease() = runBlocking {
        val sentReports = Collections.synchronizedList(mutableListOf<ByteArray>())
        val dispatcher = DefaultKeystrokeDispatcher(
            translator = UsQwertyKeymap(),
            deltaDiffEngine = DefaultDeltaDiffEngine(),
            reportSender = { report ->
                sentReports.add(report.copyOf())
                true
            }
        )

        // Launch 20 coroutines that start long bursts with delay and get cancelled mid-flight
        val jobs = (1..20).map {
            launch(Dispatchers.Default) {
                dispatcher.dispatchBurst("super-long-burst-sequence-abcdefghijklmnopqrstuvwxyz", delayMs = 20L)
            }
        }

        delay(40L)
        jobs.forEach { it.cancel() }
        jobs.forEach { it.join() }

        // After all cancellations, verify dispatcher can be acquired and last report is all-zeros
        assertTrue("Reports should have been sent", sentReports.isNotEmpty())
        val lastReport = sentReports.last()
        assertEquals(0.toByte(), lastReport[0]) // Modifiers zero
        assertEquals(0.toByte(), lastReport[2]) // Keycode zero
    }

    /**
     * EMPIRICAL DEADLOCK CHALLENGE:
     * Does calling resetState() from a thread while a coroutine is suspended in dispatchBurst cause a deadlock?
     * We run dispatchBurst with a pacing delay on a single thread executor.
     * While it is typing (and holding mutex), we call resetState() from another thread with a timeout.
     */
    @Test
    fun testResetStateDoesNotDeadlockUnderCrossThreadContention() {
        val singleThreadExecutor = Executors.newSingleThreadExecutor()
        val dispatcher = DefaultKeystrokeDispatcher(
            translator = UsQwertyKeymap(),
            deltaDiffEngine = DefaultDeltaDiffEngine(),
            reportSender = { true }
        )

        val typingStarted = CountDownLatch(1)
        val resetCompleted = CountDownLatch(1)

        // Start typing on single thread with 15ms pacing (takes 150ms total)
        singleThreadExecutor.submit {
            runBlocking {
                launch {
                    typingStarted.countDown()
                    dispatcher.dispatchBurst("0123456789", delayMs = 15L)
                }
            }
        }

        // Wait until typing has started and acquired mutex
        assertTrue("Typing should have started", typingStarted.await(2, TimeUnit.SECONDS))
        Thread.sleep(20) // Ensure it is suspended inside delay within transmitStrokesInternal

        // Call resetState() from another thread
        val resetThread = Thread {
            dispatcher.resetState()
            resetCompleted.countDown()
        }
        resetThread.start()

        // Verify resetState completes without deadlock within 3 seconds
        val completed = resetCompleted.await(3, TimeUnit.SECONDS)
        assertTrue("resetState must complete without deadlocking", completed)

        singleThreadExecutor.shutdown()
        singleThreadExecutor.awaitTermination(2, TimeUnit.SECONDS)
    }

    /**
     * EMPIRICAL DEADLOCK REPRODUCTION:
     * When dispatchBurst is running on a single-threaded dispatcher (e.g. Dispatchers.Main / UI event loop)
     * and is suspended in delay(), if resetState() is invoked from that same thread's event loop
     * (e.g. UI click handler), runBlocking blocks the single thread.
     * The suspended burst can never resume, causing a permanent DEADLOCK!
     */
    @Test
    fun testDeadlockWhenResetStateCalledOnSameThreadWhileBurstSuspended() {
        val singleThreadExecutor = Executors.newSingleThreadExecutor()
        val coroutineDispatcher = singleThreadExecutor.asCoroutineDispatcher()
        val dispatcher = DefaultKeystrokeDispatcher(
            translator = UsQwertyKeymap(),
            deltaDiffEngine = DefaultDeltaDiffEngine(),
            reportSender = { true }
        )

        val burstStarted = CountDownLatch(1)
        val resetCompleted = CountDownLatch(1)

        // Launch burst on the single-threaded dispatcher
        kotlinx.coroutines.CoroutineScope(coroutineDispatcher).launch {
            burstStarted.countDown()
            dispatcher.dispatchBurst("0123456789", delayMs = 50L)
        }

        // Wait until burst has started and acquired mutex
        assertTrue("Burst should start", burstStarted.await(2, TimeUnit.SECONDS))
        Thread.sleep(15) // Wait for dispatchBurst to enter delay inside transmitStrokesInternal

        // Submit resetState to the SAME single thread
        singleThreadExecutor.submit {
            dispatcher.resetState()
            resetCompleted.countDown()
        }

        // Await with 2 seconds timeout: because runBlocking freezes the single thread, completed is FALSE
        val completed = resetCompleted.await(2, TimeUnit.SECONDS)
        singleThreadExecutor.shutdownNow()

        println("EMPIRICAL EVIDENCE - Same thread resetState completed: $completed")
        // Verify resetState completes without deadlocking the single-threaded event loop
        assertTrue(
            "resetState() on single thread event loop must complete without deadlocking",
            completed
        )
        assertEquals("", dispatcher.currentHostText.value)
    }
}
