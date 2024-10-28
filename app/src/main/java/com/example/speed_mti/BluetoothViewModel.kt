package com.example.speed_mti

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri

    private val _selectedFileName = MutableStateFlow("No file selected")
    val selectedFileName: StateFlow<String> = _selectedFileName

    private val _fileContent = MutableStateFlow<ByteArray?>(null)
    val fileContent: StateFlow<ByteArray?> = _fileContent

    private val _receivedData = MutableStateFlow<String>("")
    val receivedData: StateFlow<String> = _receivedData

    private var buffer = StringBuilder()  // Buffer to accumulate incoming data
    private var receiveJob: Job? = null   // Job for managing the timeout delay

    private val receiveTimeout = 500L  // Timeout in milliseconds (adjust as needed)

    var connectionStates = mutableStateMapOf<String, String>()
        private set

    var bluetoothGatt: BluetoothGatt? = null

    private var gattCallback: BluetoothGattCallback? = null


    // This function will be called after the GATT connection is established
    fun setGattCallback(callback: BluetoothGattCallback) {
        gattCallback = callback
    }

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

    fun receiveDataFromDevice(data: String) {
        buffer.append(data)  // Append incoming data to the buffer

        // Cancel any existing timeout job
        receiveJob?.cancel()

        // Start a new timeout job
        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            delay(receiveTimeout)  // Wait for the timeout period

            // After the timeout, we assume the message is complete
            _receivedData.value =
                buffer.toString()  // Update the receivedData with the full message

            buffer.clear()  // Clear the buffer after processing the message
        }
    }

    // Function to handle file selection (sets both name and content)
    fun selectFile(context: Context, uri: Uri) {
        _fileContent.value = readFileFromUri(context, uri)
        // Log file content size to verify it's loaded correctly
        val fileSize = _fileContent.value?.size ?: 0
        Log.d("BluetoothViewModel", "File selected, size: $fileSize bytes")
    }

    private var fileData: ByteArray? = null
    private var offset = 0
    private val maxChunkSize = 20  // Max BLE chunk size


    // Function to start sending file data in chunks
    fun sendFileToDevice() {
        val data = _fileContent.value
        if (data != null) {
            offset = 0
            sendNextChunk()  // Start sending the first chunk
        } else {
            Log.e("BluetoothViewModel", "No file selected to send")
        }
    }

    // Function to send the next chunk of data
    private fun sendNextChunk() {
        val data = _fileContent.value ?: return
        if (offset < data.size) {
            val chunkSize = minOf(maxChunkSize, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkSize)
            val characteristic = bluetoothGatt?.getService(SERVICE_UUID)
                ?.getCharacteristic(CHARACTERISTIC_UUID)

            if (characteristic != null) {
                characteristic.value = chunk
                val writeResult = bluetoothGatt?.writeCharacteristic(characteristic)
                Log.d("BluetoothViewModel", "Writing chunk: $chunkSize bytes, write result: $writeResult")
                if (writeResult == true) {
                    offset += chunkSize  // Move the offset forward after a successful write
                } else {
                    Log.e("BluetoothViewModel", "Failed to write chunk")
                }
            } else {
                Log.e("BluetoothViewModel", "Characteristic not found!")
            }
        } else {
            Log.d("BluetoothViewModel", "File transfer complete, sent all chunks")
        }
    }

    // Handle the result of a characteristic write
    fun onCharacteristicWrite(status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            sendNextChunk()  // Send the next chunk if the previous one was successful
        } else {
            Log.e("BluetoothViewModel", "Failed to write chunk, status: $status")
        }
    }

    // Send data to the device (as ByteArray)
    fun sendDataToDevice(data: ByteArray) {
        bluetoothGatt?.let { gatt ->
            val characteristic = gatt.getService(SERVICE_UUID)
                ?.getCharacteristic(CHARACTERISTIC_UUID)
            if (characteristic != null) {
                characteristic.value = data
                val writeResult = gatt.writeCharacteristic(characteristic)
                Log.d("BluetoothViewModel", "Writing data: ${data.size} bytes, write result: $writeResult")
            } else {
                Log.e("BluetoothViewModel", "Characteristic not found!")
            }
        }
    }

    // Utility function to get the file name from URI
    private fun uriToFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "Unknown"
    }

    // Utility function to read the file content from URI
    private fun readFileFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.buffered()?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}



