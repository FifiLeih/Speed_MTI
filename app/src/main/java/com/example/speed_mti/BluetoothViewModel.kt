package com.example.speed_mti

import android.bluetooth.BluetoothGatt
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class BluetoothViewModel : ViewModel() {

    var connectedDeviceAddress = mutableStateOf<String?>(null)  // Make it reactive
        private set

    var connectionStates = mutableStateMapOf<String, String>()  // Make it reactive
        private set

    var bluetoothGatt: BluetoothGatt? = null // Store the GATT connection

    // Method to update connected device address
    fun updateConnectedDevice(address: String?) {
        connectedDeviceAddress.value = address  // Use `.value` for mutableStateOf
    }

    // Method to update connection state
    fun updateConnectionState(address: String, state: String) {
        connectionStates[address] = state  // This will recompose in real time
    }

    // Clear the connection
    fun clearConnection() {
        connectedDeviceAddress.value = null  // Use `.value` for mutableStateOf
        connectionStates.clear()  // Clear the state map
        bluetoothGatt = null // Clear the GATT reference as well
    }
}

