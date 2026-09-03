package com.transcriptor.hid.vision

import android.graphics.Rect
import com.google.mlkit.vision.text.Text

/**
 * Spatial Monospace Post-Processor for code and terminal OCR.
 *
 * Preserves code indentation by analyzing block/line bounding box X-coordinates,
 * strips noisy terminal line numbers (e.g. "  12 |", "45: "), and formats
 * raw OCR text blocks into clean, compilable code/stack-trace context.
 */
object CodeOcrPostProcessor {

    /**
     * Processes raw ML Kit [Text] recognition output and reconstructs
     * horizontal spatial indentation and line structure.
     */
    fun process(mlKitText: Text): String {
        if (mlKitText.textBlocks.isEmpty()) return ""

        val linesWithBoxes = mutableListOf<LineWithBounds>()
        for (block in mlKitText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: Rect()
                linesWithBoxes.add(LineWithBounds(line.text, box))
            }
        }

        if (linesWithBoxes.isEmpty()) return ""

        // Sort lines primarily by vertical Y-coordinate (top to bottom)
        linesWithBoxes.sortBy { it.box.top }

        // Find minimum left coordinate across all lines to establish left margin baseline
        val minLeft = linesWithBoxes.minOfOrNull { it.box.left } ?: 0
        // Estimate average character width based on line length and bounding box width
        val avgCharWidth = linesWithBoxes
            .filter { it.text.length > 5 && it.box.width() > 0 }
            .map { it.box.width().toFloat() / it.text.length }
            .average()
            .takeIf { !it.isNaN() && it > 2.0 } ?: 18.0

        val formattedLines = linesWithBoxes.map { lineItem ->
            val relativeIndent = maxOf(0, lineItem.box.left - minLeft)
            val indentSpaces = (relativeIndent / avgCharWidth).toInt().coerceIn(0, 32)
            val cleanText = sanitizeLineNumbers(lineItem.text)
            " ".repeat(indentSpaces) + cleanText
        }

        return formattedLines.joinToString("\n").trimEnd()
    }

    /**
     * Strips leading line numbers, gutter markers, and shell prompt prefixes.
     * Examples:
     * - " 14 | val x = 10" -> "val x = 10"
     * - "120: return true" -> "return true"
     * - "$ git commit" -> "git commit"
     */
    fun sanitizeLineNumbers(line: String): String {
        var result = line.trimStart()

        // Strip common IDE gutter numbers: "123 |", "123: ", "123 - "
        val gutterRegex = Regex("""^\d{1,5}\s*([|:\-])\s*""")
        result = gutterRegex.replace(result, "")

        // Strip terminal prompts: "$ ", "> ", "# ", ">>> "
        val promptRegex = Regex("""^(\$|>|#|>>>)\s+""")
        result = promptRegex.replace(result, "")

        return result
    }
}

private data class LineWithBounds(
    val text: String,
    val box: Rect
)
