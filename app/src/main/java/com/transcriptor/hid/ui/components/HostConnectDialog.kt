package com.transcriptor.hid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.transcriptor.hid.ui.PairedDeviceUi
import com.transcriptor.hid.ui.theme.ElectricViolet
import com.transcriptor.hid.ui.theme.StatusConnected
import com.transcriptor.hid.ui.theme.StatusConnecting
import com.transcriptor.hid.ui.theme.StatusDisconnected
import com.transcriptor.hid.ui.theme.StatusError

/**
 * Host Connection Dialog for managing Bluetooth HID host pairings,
 * 1-tap connecting to already paired PCs, triggering discoverability,
 * and opening Android Bluetooth Settings.
 */
@Composable
fun HostConnectDialog(
    connectionState: HidConnectionState,
    connectedDeviceName: String?,
    pairedDevices: List<PairedDeviceUi>,
    onConnectToDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRequestDiscoverability: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefreshPairedDevices: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth Connection",
                tint = ElectricViolet,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Bluetooth Host Connection",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Current Live Connection Status Banner
                CurrentConnectionCard(
                    connectionState = connectionState,
                    connectedDeviceName = connectedDeviceName,
                    onDisconnect = onDisconnect
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 2: Paired Devices Header & Refresh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAIRED HOST COMPUTERS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onRefreshPairedDevices,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh paired devices",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Section 2b: List of Paired Devices
                if (pairedDevices.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No paired Bluetooth devices found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pair with your PC using the options below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pairedDevices.forEach { device ->
                            PairedDeviceItem(
                                device = device,
                                isCurrentlyConnected = connectionState == HidConnectionState.CONNECTED &&
                                        (device.isConnected || device.name == connectedDeviceName),
                                isConnecting = connectionState == HidConnectionState.CONNECTING &&
                                        (device.isConnected || device.name == connectedDeviceName),
                                onConnect = { onConnectToDevice(device.address) },
                                onDisconnect = onDisconnect
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 3: Pairing Options & Discoverability
                Text(
                    text = "PAIR A NEW HOST PC",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Button: Make Phone Discoverable
                Button(
                    onClick = onRequestDiscoverability,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Make Discoverable",
                        tint = ElectricViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Make Phone Discoverable (120s)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Lets your PC search for & pair with this phone",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Button: Open Android Bluetooth Settings
                OutlinedButton(
                    onClick = onOpenBluetoothSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open Bluetooth Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Android Bluetooth Settings",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CurrentConnectionCard(
    connectionState: HidConnectionState,
    connectedDeviceName: String?,
    onDisconnect: () -> Unit
) {
    val (statusBg, statusBorder, statusText, statusIcon, iconTint) = when (connectionState) {
        HidConnectionState.CONNECTED -> Quintuple(
            StatusConnected.copy(alpha = 0.12f),
            StatusConnected.copy(alpha = 0.4f),
            "Connected: ${connectedDeviceName ?: "Host PC"}",
            Icons.Default.BluetoothConnected,
            StatusConnected
        )
        HidConnectionState.CONNECTING -> Quintuple(
            StatusConnecting.copy(alpha = 0.12f),
            StatusConnecting.copy(alpha = 0.4f),
            "Connecting to ${connectedDeviceName ?: "Host PC"}...",
            Icons.Default.BluetoothSearching,
            StatusConnecting
        )
        HidConnectionState.DISCONNECTED -> Quintuple(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.outlineVariant,
            "Bluetooth HID Disconnected (Idle)",
            Icons.Default.Bluetooth,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        HidConnectionState.ERROR -> Quintuple(
            StatusError.copy(alpha = 0.12f),
            StatusError.copy(alpha = 0.4f),
            "Bluetooth Error — please verify permissions",
            Icons.Default.Bluetooth,
            StatusError
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusBorder, RoundedCornerShape(14.dp)),
        color = statusBg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (connectionState == HidConnectionState.CONNECTED) {
                TextButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusDisconnected)
                ) {
                    Text("Disconnect", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PairedDeviceItem(
    device: PairedDeviceUi,
    isCurrentlyConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isCurrentlyConnected) ElectricViolet.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        border = if (isCurrentlyConnected) androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentlyConnected) ElectricViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = device.name,
                        tint = if (isCurrentlyConnected) ElectricViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (isCurrentlyConnected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDisconnected)
                ) {
                    Text("Disconnect", fontSize = 11.sp)
                }
            } else if (isConnecting) {
                Text(
                    text = "Connecting...",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusConnecting
                )
            } else {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Connect", fontSize = 11.sp)
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
