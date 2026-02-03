package com.example.blerobotcarcontrol.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
data class BLEDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice? = null
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

sealed class ControlCommand(val code: String) {
    object Forward : ControlCommand("F")
    object Backward : ControlCommand("B")
    object Left : ControlCommand("L")
    object Right : ControlCommand("R")
    object Stop : ControlCommand("S")
}

data class LogEntry(
    val timestamp: Long,
    val message: String,
    val type: LogType = LogType.INFO
)

enum class LogType {
    INFO, SUCCESS, ERROR, COMMAND
}