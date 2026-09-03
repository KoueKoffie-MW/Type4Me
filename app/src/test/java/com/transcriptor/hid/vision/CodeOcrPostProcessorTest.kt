package com.transcriptor.hid.vision

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [CodeOcrPostProcessor] verifying indentation preservation
 * and IDE/terminal noise sanitization.
 */
class CodeOcrPostProcessorTest {

    @Test
    fun testSanitizeLineNumbers_stripsGutterPipe() {
        val input = "  14 | val result = calculateSum()"
        val clean = CodeOcrPostProcessor.sanitizeLineNumbers(input)
        assertThat(clean).isEqualTo("val result = calculateSum()")
    }

    @Test
    fun testSanitizeLineNumbers_stripsGutterColon() {
        val input = "120: return true"
        val clean = CodeOcrPostProcessor.sanitizeLineNumbers(input)
        assertThat(clean).isEqualTo("return true")
    }

    @Test
    fun testSanitizeLineNumbers_stripsTerminalDollarPrompt() {
        val input = "$ git push origin main"
        val clean = CodeOcrPostProcessor.sanitizeLineNumbers(input)
        assertThat(clean).isEqualTo("git push origin main")
    }

    @Test
    fun testSanitizeLineNumbers_stripsPythonPrompt() {
        val input = ">>> import numpy as np"
        val clean = CodeOcrPostProcessor.sanitizeLineNumbers(input)
        assertThat(clean).isEqualTo("import numpy as np")
    }

    @Test
    fun testSanitizeLineNumbers_preservesStandardCodeWithoutGutter() {
        val input = "fun testCode() {"
        val clean = CodeOcrPostProcessor.sanitizeLineNumbers(input)
        assertThat(clean).isEqualTo("fun testCode() {")
    }
}
