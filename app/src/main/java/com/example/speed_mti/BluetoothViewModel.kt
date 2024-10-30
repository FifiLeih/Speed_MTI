package com.example.speed_mti

import BluetoothScanManager
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speed_mti.BluetoothGattManager.Companion.CHARACTERISTIC_UUID
import com.example.speed_mti.BluetoothGattManager.Companion.SERVICE_UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    // State variables for connected device
    private val _connectedDeviceName = MutableStateFlow("DEMO")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()
    private var connectedDeviceAddress = mutableStateOf<String?>(null)

    // State for selected file and received data
    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri
    private val _selectedFileName = MutableStateFlow("No file selected")
    val selectedFileName: StateFlow<String> = _selectedFileName
    private val _fileContent = MutableStateFlow<ByteArray?>(null)
    val fileContent: StateFlow<ByteArray?> = _fileContent
    private val _receivedData = MutableStateFlow("")
    val receivedData: StateFlow<String> = _receivedData

    // Buffer for data reception and timeout management
    private var buffer = StringBuilder()
    private var receiveJob: Job? = null
    private val receiveTimeout = 500L  // Timeout for data reception

    private var bluetoothGatt: BluetoothGatt? = null

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val discoveredDevices = mutableStateListOf<BluetoothDevice>()
    var connectionStates = mutableStateMapOf<String, String>()

    private val bluetoothScanManager = BluetoothScanManager(
        bluetoothAdapter = bluetoothAdapter,
        discoveredDevices = discoveredDevices,
        context = getApplication(),
        coroutineScope = viewModelScope,
        onScanStopped = { /* Handle UI updates or other logic here if needed */ }
    )

    private val bluetoothGattManager = BluetoothGattManager(
        bluetoothViewModel = this,
        context = getApplication(),
        coroutineScope = viewModelScope
    )

    // Start and stop scan functions
    fun startScan() = bluetoothScanManager.startBluetoothScan()
    fun stopScan() = bluetoothScanManager.stopBluetoothScan()

    // Connection management
    fun connectToDevice(device: BluetoothDevice) = bluetoothGattManager.connectToDevice(device)
    fun disconnectDevice() = bluetoothGattManager.disconnectDevice()

    fun getDiscoveredDevices(): List<BluetoothDevice> = discoveredDevices

    private fun updateConnectedDeviceName(name: String?) {
        viewModelScope.launch {
            _connectedDeviceName.value = name ?: "Unknown Device"
            Log.d("BluetoothViewModel", "Device name updated to: ${_connectedDeviceName.value}")
        }
    }

    fun updateConnectedDevice(device: BluetoothDevice?) {
        connectedDeviceAddress.value = device?.address
        updateConnectedDeviceName(device?.name)
        Log.d(
            "BluetoothViewModel",
            "Updated device: ${device?.name ?: "Unknown"}, Address: ${device?.address}"
        )
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
        buffer.append(data)

        // Cancel existing timeout job if any, and start a new one
        receiveJob?.cancel()
        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            delay(receiveTimeout)
            _receivedData.value = buffer.toString()
            buffer.clear()
        }
    }

    // Handle file selection and content retrieval
    fun selectFile(context: Context, uri: Uri) {
        _fileContent.value = readFileFromUri(context, uri)
        Log.d("BluetoothViewModel", "File selected, size: ${_fileContent.value?.size ?: 0} bytes")
    }

    // Helper to get file name from URI
    private fun uriToFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: "Unknown"
    }

    // Helper to read file content from URI and log it in hex
    private fun readFileFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val fileData = context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }

            // Convert the byte array to a hex string for logging
            fileData?.let {
                val hexString = it.joinToString(" ") { byte -> String.format("%02X", byte) }
                Log.d("BluetoothViewModel", "File content in hex: $hexString")
            }

            fileData
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private var fileData: ByteArray? = null
    private var currentChunk = 0
    private var mtuSize = 10  // Default MTU size, will be updated after negotiation
    private val maxChunkSize: Int
        get() = mtuSize - 3  // 3 bytes reserved for header

    fun startFileTransfer() {
        val data = _fileContent.value
        if (data != null) {
            fileData = data
            currentChunk = 0
            sendNextChunk()
        } else {
            Log.e("BluetoothViewModel", "No file selected to send.")
        }
    }

    private fun sendNextChunk() {
        val data = fileData ?: return
        if (currentChunk < data.size) {
            val chunkSize = minOf(maxChunkSize, data.size - currentChunk)
            val chunk = data.copyOfRange(currentChunk, currentChunk + chunkSize)
            bluetoothGattManager.writeToCharacteristic(chunk) // Write the chunk

            // Move to the next chunk after write confirmation
            currentChunk += chunkSize
        } else {
            Log.d("BluetoothViewModel", "File transfer complete, sent all chunks.")
            fileData = null // Reset file data after transfer
        }
    }

    fun onCharacteristicWrite(status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            sendNextChunk() // Send next chunk if the previous write was successful
        } else {
            Log.e("BluetoothViewModel", "Failed to write chunk, status: $status")
        }
    }
}
