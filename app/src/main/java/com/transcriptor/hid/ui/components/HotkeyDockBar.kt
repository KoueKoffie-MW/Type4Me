package com.transcriptor.hid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcriptor.hid.engine.HidConstants
import com.transcriptor.hid.engine.HidKeyStroke
import com.transcriptor.hid.ui.theme.ElectricViolet

/**
 * Data model representing a hotkey button in the Developer Hotkey Bar.
 */
data class HotkeyItem(
    val label: String,
    val stroke: HidKeyStroke,
    val description: String,
    val tag: String,
    val isPrimary: Boolean = true
)

/**
 * Central catalog of developer and terminal hotkeys.
 */
object HotkeyCatalog {
    val PRIMARY_HOTKEYS = listOf(
        HotkeyItem("ESC", HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_ESCAPE), "Escape", "esc"),
        HotkeyItem("TAB", HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_TAB), "Tab", "tab"),
        HotkeyItem("^C", HidKeyStroke(HidConstants.MOD_LCTRL, HidConstants.KEY_C), "Control C (Interrupt)", "ctrl_c"),
        HotkeyItem("^Z", HidKeyStroke(HidConstants.MOD_LCTRL, HidConstants.KEY_Z), "Control Z (Suspend)", "ctrl_z"),
        HotkeyItem("^D", HidKeyStroke(HidConstants.MOD_LCTRL, HidConstants.KEY_D), "Control D (EOF)", "ctrl_d"),
        HotkeyItem("^L", HidKeyStroke(HidConstants.MOD_LCTRL, HidConstants.KEY_L), "Control L (Clear)", "ctrl_l"),
        HotkeyItem("ALT+TAB", HidKeyStroke(HidConstants.MOD_LALT, HidConstants.KEY_TAB), "Alt Tab (Switch Window)", "alt_tab"),
        HotkeyItem("←", HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_LEFT), "Arrow Left", "arrow_left"),
        HotkeyItem("↑", HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_UP), "Arrow Up", "arrow_up"),
        HotkeyItem("↓", HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_DOWN), "Arrow Down", "arrow_down"),
        HotkeyItem("→", HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT), "Arrow Right", "arrow_right"),
        HotkeyItem("`", HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_GRAVE), "Backtick", "backtick"),
        HotkeyItem("^P", HidKeyStroke(HidConstants.MOD_LCTRL, HidConstants.KEY_P), "Control P (Search/Command)", "ctrl_p")
    )

    val FUNCTION_KEYS = (1..12).map { i ->
        val usageId = (HidConstants.KEY_F1 + (i - 1)).toByte()
        HotkeyItem("F$i", HidKeyStroke(HidConstants.MOD_NONE, usageId), "Function Key $i", "f$i", isPrimary = false)
    }
}

/**
 * Responsive Developer Hotkey Dock Bar rendering virtual terminal navigation keys,
 * control combinations (Ctrl+C, Ctrl+Z, Ctrl+D, Esc, Tab), arrow navigation,
 * expandable F-keys (F1-F12), and a direct Android-to-Host Clipboard Streamer.
 */
@Composable
fun HotkeyDockBar(
    isConnected: Boolean = true,
    onSendKeyStroke: (HidKeyStroke) -> Unit,
    onStreamClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFKeys by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Main Terminal & Navigation Keys Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clipboard Streamer Button
                HotkeyButton(
                    label = "PASTE",
                    icon = Icons.Default.ContentPaste,
                    isAccent = true,
                    onClick = onStreamClipboard
                )

                // Primary Hotkeys from Catalog
                HotkeyCatalog.PRIMARY_HOTKEYS.forEach { item ->
                    HotkeyButton(
                        label = item.label,
                        onClick = { onSendKeyStroke(item.stroke) }
                    )
                }

                // Expand F-Keys Toggle
                HotkeyButton(
                    label = if (showFKeys) "F-Keys ▲" else "F-Keys ▼",
                    onClick = { showFKeys = !showFKeys }
                )
            }

            // Expandable F-Keys Row (F1 - F12)
            if (showFKeys) {
                val fScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(fScrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HotkeyCatalog.FUNCTION_KEYS.forEach { item ->
                        HotkeyButton(
                            label = item.label,
                            onClick = { onSendKeyStroke(item.stroke) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HotkeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isAccent: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (isAccent) ElectricViolet.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            ),
        color = if (isAccent) ElectricViolet.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isAccent) ElectricViolet else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isAccent) ElectricViolet else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
