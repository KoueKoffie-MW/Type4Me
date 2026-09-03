package com.transcriptor.hid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcriptor.hid.ui.theme.AccentTeal
import com.transcriptor.hid.ui.theme.DarkSurface
import com.transcriptor.hid.ui.theme.ElectricViolet
import com.transcriptor.hid.ui.theme.TextPrimary
import com.transcriptor.hid.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * High-precision Touchpad & Mouse Control surface for Type4Me.
 *
 * Supports:
 * - 1-Finger Drag: Smooth relative cursor translation (dX, dY) with acceleration scaling.
 * - 1-Finger Tap: Left-click event.
 * - 2-Finger Tap: Right-click event.
 * - Dedicated Scroll Strip (Right edge) & Physical Click Bars (Bottom).
 * - Sensitivity adjustment slider.
 */
@Composable
fun TouchpadCanvas(
    isConnected: Boolean,
    onMouseMove: (dx: Int, dy: Int) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onMiddleClick: () -> Unit,
    onMouseScroll: (deltaY: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var sensitivity by remember { mutableFloatStateOf(1.2f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar: Mode & Sensitivity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mouse,
                        contentDescription = "Mouse Touchpad",
                        tint = AccentTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Touchpad Trackpad",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Sensitivity",
                        tint = ElectricViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${String.format("%.1f", sensitivity)}x",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricViolet
                        )
                    )
                }
            }

            // Main Trackpad Area + Right Scroll Strip
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Primary Touchpad Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF161720),
                                    Color(0xFF1E202C)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .pointerInput(sensitivity, isConnected) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startTime = System.currentTimeMillis()
                                var hasDragged = false
                                var isTwoFinger = false
                                var lastPos = down.position
                                var totalDragDistance = 0f
                                val touchSlop = viewConfiguration.touchSlop
                                var longPressTriggered = false
                                val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressedPointers = event.changes.filter { it.pressed }

                                    if (event.changes.size >= 2 || pressedPointers.size >= 2) {
                                        isTwoFinger = true
                                    }

                                    val elapsed = System.currentTimeMillis() - startTime
                                    if (!hasDragged && !isTwoFinger && !longPressTriggered && elapsed >= longPressTimeout && pressedPointers.isNotEmpty()) {
                                        if (isConnected) {
                                            onRightClick()
                                        }
                                        longPressTriggered = true
                                    }

                                    if (pressedPointers.isEmpty()) {
                                        if (!hasDragged && !longPressTriggered && elapsed < longPressTimeout) {
                                            if (isTwoFinger) {
                                                if (isConnected) onRightClick()
                                            } else {
                                                if (isConnected) onLeftClick()
                                            }
                                        }
                                        break
                                    }

                                    if (!isTwoFinger) {
                                        val primaryPointer = pressedPointers.firstOrNull { it.id == down.id } ?: pressedPointers.first()
                                        val delta = primaryPointer.position - lastPos
                                        totalDragDistance += delta.getDistance()
                                        if (totalDragDistance > touchSlop) {
                                            hasDragged = true
                                        }
                                        if (hasDragged) {
                                            primaryPointer.consume()
                                            if (isConnected) {
                                                val dx = (delta.x * sensitivity).roundToInt()
                                                val dy = (delta.y * sensitivity).roundToInt()
                                                if (dx != 0 || dy != 0) {
                                                    onMouseMove(dx, dy)
                                                }
                                            }
                                        }
                                        lastPos = primaryPointer.position
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdsClick,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (isConnected) "Slide to move · Tap for left-click · 2-finger tap or long press for right-click"
                            else "Connect to host PC to use touchpad",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // 2. Dedicated Vertical Scroll Wheel Strip
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1B24))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .pointerInput(isConnected) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (isConnected) {
                                    // Invert Y for intuitive wheel scrolling (drag up = scroll up)
                                    val wheel = (-dragAmount.y / 8f).roundToInt().coerceIn(-127, 127)
                                    if (wheel != 0) {
                                        onMouseScroll(wheel)
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Scroll Strip",
                            tint = AccentTeal.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "SCROLL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentTeal.copy(alpha = 0.7f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Sensitivity Slider Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.5f..3.0f,
                    steps = 5,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricViolet,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // Physical Mouse Buttons Bar (Bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Click Button (55%)
                Button(
                    onClick = onLeftClick,
                    enabled = isConnected,
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "Left Click",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Middle Click (Scroll Wheel Click) (15%)
                Button(
                    onClick = onMiddleClick,
                    enabled = isConnected,
                    modifier = Modifier
                        .weight(0.18f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "Mid",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Right Click Button (30%)
                Button(
                    onClick = onRightClick,
                    enabled = isConnected,
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = "Right Click",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
