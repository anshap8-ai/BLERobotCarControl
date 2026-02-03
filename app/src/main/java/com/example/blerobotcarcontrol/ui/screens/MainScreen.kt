package com.example.blerobotcarcontrol.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blerobotcarcontrol.data.ConnectionState
import com.example.blerobotcarcontrol.data.ControlCommand
import com.example.blerobotcarcontrol.ui.theme.BLERobotCarControlTheme
import com.example.blerobotcarcontrol.ui.viewmodel.MainViewModel

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val connectionState = viewModel.connectionState
    val selectedDevice = viewModel.selectedDevice
    val logs = viewModel.logs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BLE RC Control",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    Button(
                        onClick = { viewModel.clearLogs() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear Logs")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear logs")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            // Status Card
            ConnectionStatusCard(
                connectionState = connectionState.collectAsState().value,
                selectedDevice = selectedDevice,
                onScanClick = { viewModel.startScan() },
                onConnectClick = { viewModel.connectToDevice() },
                onDisconnectClick = { viewModel.disconnect() },
                onStopScanClick = { viewModel.stopScan() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Control Panel
            ControlPanelCard(
                isConnected = connectionState.collectAsState().value is ConnectionState.Connected,
                onCommand = { command -> viewModel.sendCommand(command) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Logs
            LogsCard(logs = logs, formatTimestamp = { viewModel.formatTimestamp(it) })
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionState,
    selectedDevice: com.example.blerobotcarcontrol.data.BLEDevice?,
    onScanClick: () -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onStopScanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (connectionState) {
                        is ConnectionState.Connected -> Icons.Default.BluetoothConnected
                        is ConnectionState.Scanning -> Icons.AutoMirrored.Filled.BluetoothSearching
                        is ConnectionState.Connecting -> Icons.AutoMirrored.Filled.BluetoothSearching
                        else -> Icons.Default.BluetoothDisabled
                    },
                    contentDescription = "Connection Status",
                    tint = when (connectionState) {
                        is ConnectionState.Connected -> Color.Green
                        is ConnectionState.Scanning -> Color.Yellow
                        is ConnectionState.Connecting -> Color.Yellow
                        else -> Color.Red
                    },
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Connected -> "Connected to: ${connectionState.deviceName}"
                            is ConnectionState.Connecting -> "Connecting..."
                            is ConnectionState.Error -> "Error: ${connectionState.message}"
                            else -> "Disconnected"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (selectedDevice != null) {
                        Text(
                            text = "Selected: ${selectedDevice.name}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (connectionState) {
                    is ConnectionState.Disconnected -> {
                        Button(
                            onClick = onScanClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SCAN")
                        }
                    }
                    is ConnectionState.Scanning -> {
                        Button(
                            onClick = onStopScanClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Scan")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STOP SCAN")
                        }
                    }
                    is ConnectionState.Connected -> {
                        Button(
                            onClick = onDisconnectClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Disconnect")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DISCONNECT")
                        }
                    }
                    else -> {
                        // Other states
                    }
                }
                if (selectedDevice != null && connectionState is ConnectionState.Disconnected) {
                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier.weight(1f),
                        enabled = true
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "Connect")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CONNECT")
                    }
                }
            }
        }
    }
}

//-------------------- CONTROL PANEL CARD ----------------------------
@Composable
fun ControlPanelCard(
    isConnected: Boolean,
    onCommand: (ControlCommand) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FORWARD button
            ControlButton(
                text = "F O R W A R D",
                icon = Icons.Default.PlayArrow,
                enabled = isConnected,
                onClick = { onCommand(ControlCommand.Forward) },
                // modifier = Modifier.fillMaxWidth(),
                modifier = Modifier.size ( width = 350.dp , height = 80.dp ),
                rotation = 270f
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.width(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp,
                alignment = Alignment.CenterHorizontally)
                //  modifier = Modifier.fillMaxWidth()
            ) {
                // LEFT button
                ControlButton(
                    text = "L\nE\nF\nT",
                    icon = Icons.Default.PlayArrow,
                    enabled = isConnected,
                    onClick = { onCommand(ControlCommand.Left) },
                //    modifier = Modifier.weight(1f),
                    modifier = Modifier.size ( width = 100.dp , height = 110.dp ),
                    rotation = 180f
                )
                // STOP button
                Button(
                    onClick = { onCommand(ControlCommand.Stop) },
                    enabled = isConnected,
                //    modifier = Modifier.weight(0.95f),
                    modifier = Modifier.size ( width = 100.dp , height =110.dp ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("S\nT\nO\nP")
                }
                // RIGHT button
                ControlButton(
                    text = "R\n I\nG\nH\nT",
                    icon = Icons.Default.PlayArrow,
                    enabled = isConnected,
                    onClick = { onCommand(ControlCommand.Right) },
                 //   modifier = Modifier.weight(1f),
                    modifier = Modifier.size ( width = 100.dp , height = 110.dp ),
                    rotation = 0f
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // BACKWARD button
            ControlButton(
                text = "B A C K W A R D",
                icon = Icons.Default.PlayArrow,
                enabled = isConnected,
                onClick = { onCommand(ControlCommand.Backward) },
                // modifier = Modifier.fillMaxWidth(),
                modifier = Modifier.size ( width = 350.dp , height = 80.dp ),
                rotation = 90f
            )
        }
    }
}

//---------------- CONTROL BUTTON ---------------------------------
@Composable
fun ControlButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rotation: Float = 0f
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.rotate(rotation)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

//--------------------- LOGS CARD -------------------------------
@Composable
fun LogsCard(
    logs: List<com.example.blerobotcarcontrol.data.LogEntry>,
    formatTimestamp: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Text(
                text = "  Logs:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier.weight(1f)
            ) {
                items(logs) { logEntry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(logEntry.timestamp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(70.dp)
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = logEntry.message,
                            fontSize = 12.sp,
                            color = when (logEntry.type) {
                                com.example.blerobotcarcontrol.data.LogType.SUCCESS -> Color.Blue
                                com.example.blerobotcarcontrol.data.LogType.ERROR -> Color.Red
                                com.example.blerobotcarcontrol.data.LogType.COMMAND -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    BLERobotCarControlTheme {
        // Mock preview
        MainScreen(viewModel = androidx.lifecycle.viewmodel.compose.viewModel())
    }
}