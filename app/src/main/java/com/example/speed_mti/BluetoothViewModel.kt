package com.example.speed_mti

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel : ViewModel() {

    private val _connectedDeviceName =
        MutableStateFlow<String>("DEMO")  // Use MutableStateFlow for reactivity
    val connectedDeviceName: StateFlow<String> =
        _connectedDeviceName.asStateFlow()  // Expose as StateFlow

    var connectedDeviceAddress = mutableStateOf<String?>(null)
        private set

    var connectionStates = mutableStateMapOf<String, String>()
        private set

    var bluetoothGatt: BluetoothGatt? = null

    fun updateConnectedDeviceName(name: String?) {
        viewModelScope.launch {
            _connectedDeviceName.value = name ?: "Unknown Device"  // Update device name reactively
            Log.d("BluetoothViewModel", "Device name updated to: ${_connectedDeviceName.value}")
        }
    }

    fun updateConnectedDevice(address: BluetoothDevice?) {
        connectedDeviceAddress.value = address?.address
        Log.d(
            "BluetoothViewModel",
            "Updated device: ${address?.name ?: "Unknown Device"}, Address: ${address?.address}"
        )
        updateConnectedDeviceName(address?.name)
    }

    fun updateConnectionState(address: String, state: String) {
        connectionStates[address] = state
        Log.d("BluetoothViewModel", "Connection state updated: Address: $address, State: $state")
    }

    fun clearConnection() {
        connectedDeviceAddress.value = null
        _connectedDeviceName.value = "DEMO"
        connectionStates.clear()
        bluetoothGatt = null
    }
}



