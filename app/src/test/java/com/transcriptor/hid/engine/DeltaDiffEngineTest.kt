package com.transcriptor.hid.engine

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeltaDiffEngineTest {

    private lateinit var engine: DeltaDiffEngine

    @Before
    fun setUp() {
        engine = DeltaDiffEngine.create()
    }

    @Test
    fun testIdenticalStrings() {
        val diff = engine.computeDiff("Hello", "Hello")
        assertEquals(5, diff.commonPrefixLength)
        assertEquals(0, diff.backspacesNeeded)
        assertEquals("", diff.textToAppend)
    }

    @Test
    fun testEmptyToText() {
        val diff = engine.computeDiff("", "Start")
        assertEquals(0, diff.commonPrefixLength)
        assertEquals(0, diff.backspacesNeeded)
        assertEquals("Start", diff.textToAppend)
    }

    @Test
    fun testTextToEmpty() {
        val diff = engine.computeDiff("Testing", "")
        assertEquals(0, diff.commonPrefixLength)
        assertEquals(7, diff.backspacesNeeded)
        assertEquals("", diff.textToAppend)
    }

    @Test
    fun testSuffixAppend() {
        val diff = engine.computeDiff("Hello", "Hello world")
        assertEquals(5, diff.commonPrefixLength)
        assertEquals(0, diff.backspacesNeeded)
        assertEquals(" world", diff.textToAppend)
    }

    @Test
    fun testSuffixReplaceWord() {
        val diff = engine.computeDiff("I like to read", "I like to lead")
        assertEquals(10, diff.commonPrefixLength) // "I like to " is 10 chars
        assertEquals(4, diff.backspacesNeeded)    // "read" is 4 chars
        assertEquals("lead", diff.textToAppend)
    }

    @Test
    fun testTotalReplacement() {
        val diff = engine.computeDiff("cat", "dog")
        assertEquals(0, diff.commonPrefixLength)
        assertEquals(3, diff.backspacesNeeded)
        assertEquals("dog", diff.textToAppend)
    }

    @Test
    fun testUnicodeCodePointSafetyWithSurrogatePairs() {
        // Emoji 😀 is 2 UTF-16 code units (surrogate pair) but 1 Unicode code point
        // Case 1: "Hello 😀" -> "Hello " (deleting only the emoji)
        val diff1 = engine.computeDiff("Hello 😀", "Hello ")
        assertEquals(6, diff1.commonPrefixLength) // "Hello " is 6 code points
        assertEquals(1, diff1.backspacesNeeded)   // Exactly 1 backspace needed for the emoji
        assertEquals("", diff1.textToAppend)

        // Case 2: "Hello 😀" -> "Hello" (deleting space and emoji)
        val diff2 = engine.computeDiff("Hello 😀", "Hello")
        assertEquals(5, diff2.commonPrefixLength) // "Hello" is 5 code points
        assertEquals(2, diff2.backspacesNeeded)   // 2 backspaces for space + emoji
        assertEquals("", diff2.textToAppend)
    }

    @Test
    fun testUnicodeCodePointPreservationInMiddle() {
        val diff = engine.computeDiff("Hello 😀 world", "Hello 😀 earth")
        assertEquals(8, diff.commonPrefixLength) // "Hello 😀 " is 8 code points
        assertEquals(5, diff.backspacesNeeded)   // "world" is 5
        assertEquals("earth", diff.textToAppend)
    }

    @Test
    fun testGermanUmlautDiff() {
        val diff = engine.computeDiff("Über", "Übung")
        assertEquals(2, diff.commonPrefixLength) // "Üb" is 2 code points
        assertEquals(2, diff.backspacesNeeded)   // "er" is 2
        assertEquals("ung", diff.textToAppend)
    }
}
