package com.transcriptor.hid.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.transcriptor.hid.R
import com.transcriptor.hid.ai.PromptPreset
import com.transcriptor.hid.ui.theme.ElectricViolet

@Composable
fun PresetSelector(
    presets: List<PromptPreset>,
    selectedPreset: PromptPreset?,
    onPresetSelect: (PromptPreset) -> Unit,
    onAddPresetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            val isSelected = selectedPreset?.id == preset.id
            val icon = getPresetIcon(preset)

            FilterChip(
                selected = isSelected,
                onClick = { onPresetSelect(preset) },
                label = {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = preset.title,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) ElectricViolet else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricViolet.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = ElectricViolet
                )
            )
        }

        // Add / Manage Custom Preset Chip
        SuggestionChip(
            onClick = onAddPresetClick,
            label = {
                Text(
                    text = stringResource(R.string.btn_add_preset),
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricViolet
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.btn_add_preset),
                    modifier = Modifier.size(16.dp),
                    tint = ElectricViolet
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
                enabled = true,
                borderColor = ElectricViolet.copy(alpha = 0.5f)
            )
        )
    }
}

private fun getPresetIcon(preset: PromptPreset): ImageVector {
    return when (preset.id) {
        1L -> Icons.Default.AutoAwesome
        2L -> Icons.Default.Work
        3L -> Icons.Default.Code
        else -> Icons.Default.AutoAwesome
    }
}
