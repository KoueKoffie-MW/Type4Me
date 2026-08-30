package com.transcriptor.hid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcriptor.hid.R
import com.transcriptor.hid.engine.KeyLayout
import com.transcriptor.hid.ui.theme.ElectricViolet

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun ControlBar(
    activeLayout: KeyLayout,
    liveDiffEnabled: Boolean,
    typingDelayMs: Long,
    onLayoutChange: (KeyLayout) -> Unit,
    onLiveDiffToggle: (Boolean) -> Unit,
    onDelayChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Keymap & Mode Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keymap Toggle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_keymap),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SegmentedButtonGroup(
                        options = listOf(
                            KeyLayout.GERMAN_QWERTZ to stringResource(R.string.keymap_german),
                            KeyLayout.US_QWERTY to stringResource(R.string.keymap_us)
                        ),
                        selected = activeLayout,
                        onSelect = onLayoutChange
                    )
                }

                // Mode Toggle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_transmission_mode),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SegmentedButtonGroup(
                        options = listOf(
                            false to stringResource(R.string.mode_burst),
                            true to stringResource(R.string.mode_live_diff)
                        ),
                        selected = liveDiffEnabled,
                        onSelect = onLiveDiffToggle
                    )
                }
            }

            // Row 2: Typing Delay Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_typing_delay, typingDelayMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (typingDelayMs <= 6L) "Ultra-Fast (Host Buffer)" else "Standard Pacing",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = typingDelayMs.toFloat(),
                onValueChange = { onDelayChange(it.toLong()) },
                valueRange = 5f..15f,
                steps = 9,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics {
                        contentDescription = "Typing delay $typingDelayMs milliseconds"
                    },
                colors = SliderDefaults.colors(
                    thumbColor = ElectricViolet,
                    activeTrackColor = ElectricViolet,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
fun <T> SegmentedButtonGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .selectableGroup()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) ElectricViolet else Color.Transparent
                        )
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(value) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
