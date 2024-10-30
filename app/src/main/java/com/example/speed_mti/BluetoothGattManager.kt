package com.example.speed_mti

import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothGattManager(
    private val bluetoothViewModel: BluetoothViewModel,
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (PermissionsManager(context).hasBluetoothPermissions() && bluetoothGatt == null) {
            bluetoothViewModel.updateConnectionState(device.address, "Connecting")
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            bluetoothViewModel.updateConnectedDevice(device)
        }
    }

    fun disconnectDevice() {
        bluetoothGatt?.disconnect()
        coroutineScope.launch {
            delay(500) // Wait briefly to ensure disconnect completes
            bluetoothGatt?.close() // Release resources associated with the GATT connection
            bluetoothGatt = null // Reset the GATT instance to allow a new connection

            bluetoothViewModel.clearConnection() // Clear connection data in ViewModel
            delay(1000) // Optional: wait before starting scan to give device time to reset
            bluetoothViewModel.startScan() // Start scanning after disconnecting and cleaning up
        }
    }

    fun writeToCharacteristic(data: ByteArray) {
        characteristic?.let {
            it.value = data
            val result = bluetoothGatt?.writeCharacteristic(it)
            Log.d("BluetoothGattManager", "Writing data: ${data.size} bytes, result: $result")
        } ?: Log.e("BluetoothGattManager", "Characteristic not found")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address ?: return
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                bluetoothViewModel.updateConnectionState(deviceAddress, "Connected")
                gatt?.discoverServices()
                Log.d("BluetoothGattManager", "Connected to device: $deviceAddress")
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                bluetoothViewModel.updateConnectionState(deviceAddress, "Disconnected")
                Log.d("BluetoothGattManager", "Disconnected from device: $deviceAddress")
                bluetoothViewModel.clearConnection()
                coroutineScope.launch {
                    delay(1000) // Optional: small delay before restarting scan
                    bluetoothViewModel.startScan() // Restart scan on disconnect
                }
            }
        }
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            bluetoothViewModel.onCharacteristicWrite(status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothGattManager", "Services discovered")
                characteristic = gatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)

                if (characteristic != null) {
                    Log.d("BluetoothGattManager", "Characteristic found: ${CHARACTERISTIC_UUID}")
                    bluetoothViewModel.startFileTransfer() // Start file transfer if characteristic is found
                } else {
                    Log.e("BluetoothGattManager", "Characteristic not found!")
                }
            } else {
                Log.e("BluetoothGattManager", "Service discovery failed, status: $status")
            }
        }


        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val data = characteristic?.value?.let { String(it) }
            if (data != null) {
                bluetoothViewModel.receiveDataFromDevice(data)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)?.let {
                    gatt.setCharacteristicNotification(it, true)
                    val descriptor = it.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }
        }
    }


}
