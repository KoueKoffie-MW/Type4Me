package com.transcriptor.hid.engine

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * High-performance, single-pass variable tokenizer and AST evaluator.
 * Supports backslash delimiter escaping: `\{\{ ... \}\}` outputs literal `{{ ... }}`.
 */
object VariableParser {

    // Matches unescaped {{ ... }} tags (negative lookbehind for backslash, except when backslash is part of a file path)
    private val VARIABLE_REGEX = Regex("""(?:(?<!\\)|(?<=[a-zA-Z0-9_.:/\\()\-+@~%$\[\]]\\))\{\{([^}]+)\}\}""")

    /**
     * Parses raw template string into a structured list of tokens.
     * Handles `\{\{` and `\}\}` backslash escaping for literal Jinja2/Helm/Ansible templates.
     */
    fun parse(template: String): List<TemplateToken> {
        val tokens = mutableListOf<TemplateToken>()
        var lastIndex = 0

        for (match in VARIABLE_REGEX.findAll(template)) {
            val range = match.range
            if (range.first > lastIndex) {
                val literalText = unescapeDelimiters(template.substring(lastIndex, range.first))
                if (literalText.isNotEmpty()) {
                    tokens.add(TemplateToken.Literal(literalText))
                }
            }

            val rawExpression = match.groupValues[1].trim()
            val descriptor = parseDescriptor(rawExpression)
            if (descriptor is VariableDescriptor.UnrecognizedLiteral) {
                // Treat unrecognized tags as literal text to prevent unintended interactive prompts
                tokens.add(TemplateToken.Literal("{{${descriptor.rawTag}}}"))
            } else {
                tokens.add(TemplateToken.DynamicVariable(descriptor))
            }

            lastIndex = range.last + 1
        }

        if (lastIndex < template.length) {
            val trailingText = unescapeDelimiters(template.substring(lastIndex))
            if (trailingText.isNotEmpty()) {
                tokens.add(TemplateToken.Literal(trailingText))
            }
        }

        return tokens
    }

    private fun unescapeDelimiters(text: String): String {
        return text
            .replace("""\\\{\\\{""", "{{")
            .replace("""\\\}\\\}""", "}}")
            .replace("""\\{\\{""", "{{")
            .replace("""\\}\\}""", "}}")
            .replace("""\{\{""", "{{")
            .replace("""\}\}""", "}}")
            .replace("""\{{""", "{{")
            .replace("""\}}""", "}}")
    }

    private fun parseDescriptor(expression: String): VariableDescriptor {
        return when {
            expression.equals("timestamp", ignoreCase = true) -> VariableDescriptor.Timestamp
            expression.equals("iso_date", ignoreCase = true) || expression.equals("date", ignoreCase = true) -> VariableDescriptor.IsoDate
            expression.startsWith("date:", ignoreCase = true) -> {
                val pattern = expression.substringAfter("date:").trim()
                VariableDescriptor.FormattedDate(pattern)
            }
            expression.equals("uuid", ignoreCase = true) -> VariableDescriptor.Uuid
            expression.equals("short_uuid", ignoreCase = true) -> VariableDescriptor.ShortUuid
            expression.equals("clipboard", ignoreCase = true) -> VariableDescriptor.Clipboard
            expression.equals("prompt_input", ignoreCase = true) || expression.equals("prompt", ignoreCase = true) ->
                VariableDescriptor.Prompt(label = "Input")
            expression.startsWith("prompt:", ignoreCase = true) -> {
                val body = expression.substringAfter("prompt:").trim()
                val delimiter = if (body.contains("|")) "|" else if (body.contains(":")) ":" else null
                if (delimiter != null) {
                    val parts = body.split(delimiter, limit = 2)
                    val label = parts[0].trim().ifBlank { "Input" }
                    val defaultVal = parts[1].trim()
                    VariableDescriptor.Prompt(label = label, defaultValue = defaultVal)
                } else {
                    val label = body.ifBlank { "Input" }
                    VariableDescriptor.Prompt(label = label)
                }
            }
            expression.equals("cursor", ignoreCase = true) -> VariableDescriptor.Cursor
            expression.equals("host_os", ignoreCase = true) -> VariableDescriptor.HostOs
            else -> VariableDescriptor.UnrecognizedLiteral(expression)
        }
    }

    /**
     * Extracts all explicit interactive prompt descriptors from a template that require user input.
     */
    fun extractPrompts(template: String): List<VariableDescriptor.Prompt> {
        return parse(template).mapNotNull { token ->
            if (token is TemplateToken.DynamicVariable && token.descriptor is VariableDescriptor.Prompt) {
                token.descriptor
            } else null
        }.distinctBy { it.label }
    }

    /**
     * Evaluates the template with the provided context into final text and cursor backtrack offset.
     * Uses Unicode code-point counting (Character.codePointCount) to avoid emoji / surrogate-pair offset drift.
     *
     * @return Pair of (Final String to Type, Number of Left-Arrow Keys to Backtrack)
     */
    fun evaluate(template: String, context: InterpolationContext): Pair<String, Int> {
        val tokens = parse(template)
        val sb = StringBuilder()
        var cursorBacktrack = 0
        var cursorFound = false

        for (token in tokens) {
            when (token) {
                is TemplateToken.Literal -> {
                    sb.append(token.text)
                    if (cursorFound) {
                        cursorBacktrack += token.text.codePointCount(0, token.text.length)
                    }
                }
                is TemplateToken.DynamicVariable -> {
                    when (val desc = token.descriptor) {
                        is VariableDescriptor.Timestamp -> {
                            val value = System.currentTimeMillis().toString()
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                        is VariableDescriptor.IsoDate -> {
                            val value = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC))
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                        is VariableDescriptor.FormattedDate -> {
                            val sdf = try {
                                SimpleDateFormat(desc.pattern, Locale.getDefault())
                            } catch (_: Exception) {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            }
                            val value = sdf.format(Date())
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                        is VariableDescriptor.Uuid -> {
                            val value = UUID.randomUUID().toString()
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                        is VariableDescriptor.ShortUuid -> {
                            val value = UUID.randomUUID().toString().substring(0, 8)
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                        is VariableDescriptor.Clipboard -> {
                            val value = context.clipboardText ?: ""
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                        is VariableDescriptor.Prompt -> {
                            val value = context.promptAnswers[desc.label]
                                ?: context.promptAnswers[desc.label.lowercase().replace(" ", "_")]
                                ?: desc.defaultValue
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                        is VariableDescriptor.HostOs -> {
                            val value = context.hostOs
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                        is VariableDescriptor.Cursor -> {
                            // Enforce single cursor handling: only first occurrence activates backtracking
                            if (!cursorFound) {
                                cursorFound = true
                            }
                        }
                        is VariableDescriptor.UnrecognizedLiteral -> {
                            val value = "{{${desc.rawTag}}}"
                            sb.append(value)
                            if (cursorFound) cursorBacktrack += value.codePointCount(0, value.length)
                        }
                    }
                }
            }
        }

        return Pair(sb.toString(), cursorBacktrack)
    }
}
