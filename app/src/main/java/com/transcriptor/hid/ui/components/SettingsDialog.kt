package com.transcriptor.hid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcriptor.hid.R
import com.transcriptor.hid.ai.GeminiRemoteRewriter
import com.transcriptor.hid.ui.theme.ElectricViolet
import com.transcriptor.hid.ui.theme.StatusConnected
import com.transcriptor.hid.ui.theme.StatusError
import com.transcriptor.hid.ui.theme.TextPrimary
import com.transcriptor.hid.ui.theme.TextSecondary

/**
 * Settings and AI Configuration Dialog for configuring Gemini API key,
 * model selection, and verification.
 */
@Composable
fun SettingsDialog(
    apiKey: String,
    isApiKeyVisible: Boolean,
    selectedModel: String,
    speakerAccent: String,
    spokenLanguage: String,
    isTestingApiKey: Boolean,
    feedbackMessage: String?,
    isApiKeyValid: Boolean?,
    onApiKeyChange: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onModelSelect: (String) -> Unit,
    onSpeakerAccentChange: (String) -> Unit,
    onSpokenLanguageChange: (String) -> Unit,
    onTestApiKey: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val accentPresets = listOf(
        AccentOption(id = "None", label = "🌐 Neutral / None", description = "No acoustic adjustments applied"),
        AccentOption(id = "Afrikaans", label = "🇿🇦 Afrikaans / SA", description = "Fixes Afrikaans vowel shifts & terminology"),
        AccentOption(id = "German", label = "🇩🇪 German", description = "Fixes w/v shifts, th/s, and German cognates"),
        AccentOption(id = "Dutch", label = "🇳🇱 Dutch", description = "Fixes Dutch phonetic substitutions"),
        AccentOption(id = "French", label = "🇫🇷 French", description = "Fixes h-dropping and French vowel patterns"),
        AccentOption(id = "Indian", label = "🇮🇳 Indian", description = "Fixes retroflex consonants & Indian phrasing"),
        AccentOption(id = "Spanish", label = "🇪🇸 Spanish", description = "Fixes initial s-clusters (e.g. estatus -> status)")
    )

    val isCustomAccent = accentPresets.none { it.id.equals(speakerAccent, ignoreCase = true) } && speakerAccent.isNotBlank()

    val modelOptions = listOf(
        ModelOption(
            id = GeminiRemoteRewriter.MODEL_GEMINI_3_5_FLASH_LITE,
            title = "Gemini 3.5 Flash-Lite",
            subtitle = "Recommended · Ultra low-latency (~0.6s speech cleanup)",
            tag = "Default"
        ),
        ModelOption(
            id = GeminiRemoteRewriter.MODEL_GEMINI_3_6_FLASH,
            title = "Gemini 3.6 Flash",
            subtitle = "Balanced high-accuracy reasoning (~1.2s)",
            tag = "Flash"
        ),
        ModelOption(
            id = GeminiRemoteRewriter.MODEL_GEMINI_3_FLASH_PREVIEW,
            title = "Gemini 3 Flash Preview",
            subtitle = "Fast multimodal reasoning (~2s)",
            tag = "Preview"
        ),
        ModelOption(
            id = GeminiRemoteRewriter.MODEL_GEMINI_3_7_FLASH,
            title = "Gemini 3.7 Flash",
            subtitle = "Extended reasoning & high-fidelity formatting",
            tag = "Pro"
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = ElectricViolet,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Settings & AI Configuration",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Gemini API Key
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Gemini API Key",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Enter your Google AI Studio API key to enable remote AI transcript rewriting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text("API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (apiKey.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onApiKeyChange("") },
                                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear API key",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onToggleApiKeyVisibility,
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isApiKeyVisible) "Hide API key" else "Show API key",
                                        tint = if (isApiKeyVisible) ElectricViolet else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Test Connection Button
                    OutlinedButton(
                        onClick = onTestApiKey,
                        enabled = apiKey.isNotBlank() && !isTestingApiKey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTestingApiKey) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Testing Connection...")
                        } else {
                            Text(text = "Test Gemini Connection")
                        }
                    }

                    // Test Result Feedback Banner
                    if (feedbackMessage != null) {
                        val (bannerBg, bannerBorder, bannerIcon, iconTint) = when (isApiKeyValid) {
                            true -> Quad(
                                StatusConnected.copy(alpha = 0.15f),
                                StatusConnected,
                                Icons.Default.CheckCircle,
                                StatusConnected
                            )
                            false -> Quad(
                                StatusError.copy(alpha = 0.15f),
                                StatusError,
                                Icons.Default.Error,
                                StatusError
                            )
                            else -> Quad(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.outlineVariant,
                                Icons.Default.Info,
                                MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, bannerBorder, RoundedCornerShape(8.dp)),
                            color = bannerBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = bannerIcon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = feedbackMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 2: Speaker Accent & Phonetic Adaptation
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🎙️",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Speaker Accent & Phonetic Adaptation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Instructs Gemini to contextualize and repair typical ASR phonetic errors, vowel shifts, and misunderstandings common to your accent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Spoken Language Input
                    OutlinedTextField(
                        value = spokenLanguage,
                        onValueChange = onSpokenLanguageChange,
                        label = { Text("Spoken Language") },
                        placeholder = { Text("e.g. English, German, Afrikaans") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Your Native Accent / Dialect:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accentPresets.forEach { preset ->
                            val isSelected = !isCustomAccent && (preset.id.equals(speakerAccent, ignoreCase = true) || (preset.id == "None" && speakerAccent.isBlank()))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) ElectricViolet else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { onSpeakerAccentChange(preset.id) },
                                        role = Role.RadioButton
                                    ),
                                color = if (isSelected) ElectricViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = ElectricViolet,
                                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = preset.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Accent Option
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isCustomAccent) 1.5.dp else 1.dp,
                                    color = if (isCustomAccent) ElectricViolet else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .selectable(
                                    selected = isCustomAccent,
                                    onClick = { if (!isCustomAccent) onSpeakerAccentChange("Custom") },
                                    role = Role.RadioButton
                                ),
                            color = if (isCustomAccent) ElectricViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isCustomAccent,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = ElectricViolet,
                                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "✏️ Custom Accent / Dialect",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCustomAccent) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isCustomAccent) {
                                    OutlinedTextField(
                                        value = if (speakerAccent == "Custom") "" else speakerAccent,
                                        onValueChange = onSpeakerAccentChange,
                                        placeholder = { Text("e.g. Scottish, Bavarian, Swiss-German") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 26.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 3: Gemini Model Selection
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = ElectricViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI Model Selection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    modelOptions.forEach { option ->
                        val isSelected = option.id == selectedModel

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ElectricViolet else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .selectable(
                                    selected = isSelected,
                                    onClick = { onModelSelect(option.id) },
                                    role = Role.RadioButton
                                ),
                            color = if (isSelected) ElectricViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ElectricViolet,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isSelected) ElectricViolet.copy(alpha = 0.25f)
                                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = option.tag,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (isSelected) ElectricViolet else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricViolet,
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = "Save Settings",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private data class ModelOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String
)

private data class AccentOption(
    val id: String,
    val label: String,
    val description: String
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
