package com.transcriptor.hid.engine

/**
 * Parsed token hierarchy from a snippet template.
 */
sealed interface TemplateToken {
    data class Literal(val text: String) : TemplateToken
    data class DynamicVariable(val descriptor: VariableDescriptor) : TemplateToken
}

/**
 * Variable descriptor extracted from mustache tag.
 */
sealed interface VariableDescriptor {
    data object Timestamp : VariableDescriptor
    data object IsoDate : VariableDescriptor
    data class FormattedDate(val pattern: String) : VariableDescriptor
    data object Uuid : VariableDescriptor
    data object ShortUuid : VariableDescriptor
    data object Clipboard : VariableDescriptor
    data class Prompt(val label: String, val defaultValue: String = "") : VariableDescriptor
    data object Cursor : VariableDescriptor
    data object HostOs : VariableDescriptor
    data class UnrecognizedLiteral(val rawTag: String) : VariableDescriptor
}

/**
 * Execution context providing values for dynamic variable resolution.
 */
data class InterpolationContext(
    val clipboardText: String? = null,
    val promptAnswers: Map<String, String> = emptyMap(),
    val hostOs: String = "WINDOWS"
)
