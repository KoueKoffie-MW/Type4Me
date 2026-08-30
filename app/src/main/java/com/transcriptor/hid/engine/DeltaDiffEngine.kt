package com.transcriptor.hid.engine

/**
 * Result of a differential computation between the currently rendered host text
 * and an updated speech transcription hypothesis.
 *
 * @property commonPrefixLength Number of matching Unicode code points in the longest common prefix (LCP).
 * @property backspacesNeeded Number of backspace keystrokes required to erase the non-matching suffix.
 * @property textToAppend The new suffix string to append.
 */
data class DiffResult(
    val commonPrefixLength: Int,
    val backspacesNeeded: Int,
    val textToAppend: String
)

/**
 * Interface for computing diffs between consecutive text states.
 */
interface DeltaDiffEngine {
    /**
     * Computes the minimal backspace and append operations using Unicode code points.
     */
    fun computeDiff(oldText: String, newText: String): DiffResult

    companion object {
        fun create(): DeltaDiffEngine = DefaultDeltaDiffEngine()
    }
}

/**
 * Default implementation of [DeltaDiffEngine] operating on 32-bit Unicode code points
 * to ensure grapheme and surrogate pair safety.
 */
class DefaultDeltaDiffEngine : DeltaDiffEngine {

    override fun computeDiff(oldText: String, newText: String): DiffResult {
        if (oldText == newText) {
            val codePointCount = if (oldText.isEmpty()) 0 else oldText.codePointCount(0, oldText.length)
            return DiffResult(
                commonPrefixLength = codePointCount,
                backspacesNeeded = 0,
                textToAppend = ""
            )
        }

        if (oldText.isEmpty()) {
            return DiffResult(
                commonPrefixLength = 0,
                backspacesNeeded = 0,
                textToAppend = newText
            )
        }

        if (newText.isEmpty()) {
            return DiffResult(
                commonPrefixLength = 0,
                backspacesNeeded = oldText.codePointCount(0, oldText.length),
                textToAppend = ""
            )
        }

        val oldPoints = oldText.codePoints().toArray()
        val newPoints = newText.codePoints().toArray()

        var k = 0
        val minLen = minOf(oldPoints.size, newPoints.size)
        while (k < minLen && oldPoints[k] == newPoints[k]) {
            k++
        }

        val backspacesNeeded = oldPoints.size - k
        val appendPoints = newPoints.sliceArray(k until newPoints.size)
        val textToAppend = String(appendPoints, 0, appendPoints.size)

        return DiffResult(
            commonPrefixLength = k,
            backspacesNeeded = backspacesNeeded,
            textToAppend = textToAppend
        )
    }
}
