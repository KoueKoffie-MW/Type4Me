package com.transcriptor.hid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.transcriptor.hid.service.HidConnectionState
import com.transcriptor.hid.ui.theme.StatusConnected
import com.transcriptor.hid.ui.theme.StatusConnecting
import com.transcriptor.hid.ui.theme.StatusDisconnected
import com.transcriptor.hid.ui.theme.StatusError

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@Composable
fun ConnectionHeader(
    connectionState: HidConnectionState,
    connectedDeviceName: String?,
    onPairHostClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title & Brand
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = stringResource(R.string.connection_header_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Hardware HID Voice Input Companion",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )
            }

            // Right Actions: Status Badge & Settings Entry Point
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection Status Pill Badge
                ConnectionStatusBadge(
                    connectionState = connectionState,
                    connectedDeviceName = connectedDeviceName,
                    onBadgeClick = onPairHostClick
                )

                // Settings Entry Point IconButton
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .size(48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Settings and AI Configuration"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBadge(
    connectionState: HidConnectionState,
    connectedDeviceName: String?,
    onBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusBg, statusText, statusIcon) = when (connectionState) {
        HidConnectionState.CONNECTED -> {
            val name = connectedDeviceName ?: stringResource(R.string.host_pc_default)
            Quadruple(
                StatusConnected,
                StatusConnected.copy(alpha = 0.15f),
                name,
                Icons.Default.BluetoothConnected
            )
        }
        HidConnectionState.CONNECTING -> Quadruple(
            StatusConnecting,
            StatusConnecting.copy(alpha = 0.15f),
            stringResource(R.string.status_connecting),
            Icons.AutoMirrored.Filled.BluetoothSearching
        )
        HidConnectionState.DISCONNECTED -> Quadruple(
            StatusDisconnected,
            StatusDisconnected.copy(alpha = 0.15f),
            stringResource(R.string.status_disconnected),
            Icons.Default.Bluetooth
        )
        HidConnectionState.ERROR -> Quadruple(
            StatusError,
            StatusError.copy(alpha = 0.15f),
            stringResource(R.string.status_error),
            Icons.Default.Bluetooth
        )
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onBadgeClick() }
            .semantics {
                role = Role.Button
                contentDescription = "Bluetooth status: $statusText. Tap to pair or manage connection."
            },
        color = statusBg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Icon(
                imageVector = statusIcon,
                contentDescription = statusText,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
