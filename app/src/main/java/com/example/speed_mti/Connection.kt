package com.example.speed_mti

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// Bluetooth UUIDs
val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
val CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


@Composable
fun BluetoothDeviceListScreen(
    navController: NavHostController,
    bluetoothViewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled ?: false) }
    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
    var scanning by remember { mutableStateOf(false) }
    var scanCallback by remember { mutableStateOf<ScanCallback?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Reference the connected device address from the ViewModel
    val connectedDeviceAddress by rememberSaveable { mutableStateOf(bluetoothViewModel.connectedDeviceAddress) }

    // Track connection state for each device from the ViewModel
    val connectionStates = bluetoothViewModel.connectionStates


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val scanPermissionGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
        val connectPermissionGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        val locationPermissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (scanPermissionGranted && connectPermissionGranted && locationPermissionGranted) {
            Log.d("Bluetooth", "Permissions granted. Starting scan.")
            if (scanCallback == null) {
                scanCallback = getScanCallback(discoveredDevices)
            }
            startBluetoothScan(
                bluetoothAdapter,
                discoveredDevices,
                context,
                scanCallback,
                coroutineScope,
                onScanStopped = {
                    scanning = false  // Update the scanning state or handle other UI changes
                    Log.d("Bluetooth", "Scan stopped after timeout")
                }
            )
        } else {
            Log.d("Bluetooth", "Permissions not granted.")
        }
    }

    // GATT Callback for managing Bluetooth GATT events (moved to the main composable scope)
    val gattCallback = object : BluetoothGattCallback() {

        var bluetoothGatt: BluetoothGatt? = null  // This will hold the active GATT connection

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val device = gatt?.device ?: return  // Get the BluetoothDevice object
            val deviceAddress = device.address
            val deviceName = device.name ?: "Unknown Device"  // Get the device name

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BluetoothGatt", "Connected to GATT server. Device Name: $deviceName")
                bluetoothViewModel.updateConnectionState(deviceAddress, "Connected")
                bluetoothViewModel.updateConnectedDevice(device) // Pass the whole BluetoothDevice object
                bluetoothViewModel.updateConnectedDeviceName(deviceName) // Update device name here
                stopBluetoothScan(
                    bluetoothAdapter,
                    context,
                    scanCallback
                )  // Stop scanning when connected
                scanning = false
                gatt?.discoverServices()
                bluetoothViewModel.bluetoothGatt = gatt // Persist the GATT connection
                bluetoothViewModel.setGattCallback(this)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BluetoothGatt", "Disconnected from GATT server. Address: $deviceAddress")
                bluetoothViewModel.updateConnectionState(deviceAddress, "Idle")
                bluetoothViewModel.clearConnection() // Clear connection data

                coroutineScope.launch {
                    Log.d("BluetoothGatt", "Delaying restart scan by 1000ms.")
                    delay(1000)  // Wait for 1 second before restarting the scan
                    if (!scanning) {
                        Log.d("BluetoothGatt", "Restarting scan after disconnection.")
                        discoveredDevices.clear()  // Clear the list of discovered devices
                        scanning = true

                        if (scanCallback == null) {
                            Log.d("BluetoothGatt", "Scan callback is null. Initializing callback.")
                            scanCallback = getScanCallback(discoveredDevices)
                        }

                        if (hasBluetoothPermissions(context)) {
                            Log.d("BluetoothGatt", "Permissions granted. Starting scan.")
                            startBluetoothScan(
                                bluetoothAdapter,
                                discoveredDevices,
                                context,
                                scanCallback,
                                coroutineScope,
                                onScanStopped = {
                                    scanning =
                                        false  // Update the scanning state or handle other UI changes
                                    Log.d("Bluetooth", "Scan stopped after timeout")
                                }
                            )
                        } else {
                            Log.e(
                                "BluetoothGatt",
                                "Bluetooth permissions not granted. Cannot start scan."
                            )
                        }
                    }
                }
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothGatt", "Services discovered for device: ${gatt?.device?.name}")
                gatt?.requestMtu(512)  // Request higher MTU size
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            bluetoothViewModel.onCharacteristicWrite(status)  // Notify ViewModel when a chunk is written
        }

        var totalReceivedData = ""
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?
        ) {
            val data = characteristic?.value?.let { String(it) }

            if (data != null) {
                totalReceivedData += data  // Accumulate the data

                // If you use a timeout or other mechanism to detect when all data is received,
                // after processing the total data, reset the `totalReceivedData`.
                bluetoothViewModel.receiveDataFromDevice(totalReceivedData)  // Pass total data

                // After passing the complete data, reset `totalReceivedData` to avoid duplicates
                totalReceivedData = ""  // Clear it after the full message is processed
            }
        }


        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothGatt", "MTU changed to: $mtu")
                // Enable notifications on a specific characteristic after MTU change
                val characteristic =
                    gatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_UUID)
                characteristic?.let {
                    gatt.setCharacteristicNotification(it, true)
                    val descriptor = it.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }
        }
    }

    // Request the required Bluetooth and location permissions
    fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        // Launch the permission request
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(Unit) {
        if (!scanning && bluetoothViewModel.bluetoothGatt == null) {
            scanning = true
            requestPermissions()  // Request permissions and start scanning automatically
            Log.d("Bluetooth", "Starting BLE scan when screen is opened.")
        } else {
            Log.d("Bluetooth", "Scan is already running or device is connected.")
        }
    }

    // Function to connect to a Bluetooth device
    fun connectToGattDevice(device: BluetoothDevice, context: Context) {
        if (hasBluetoothPermissions(context)) {
            if (bluetoothViewModel.bluetoothGatt == null) { // Check if already connected
                if (scanning) {
                    stopBluetoothScan(bluetoothAdapter, context, scanCallback)
                    scanning = false
                    Log.d("Bluetooth", "Stopping BLE scan before connecting to GATT server...")
                }
                bluetoothViewModel.updateConnectionState(device.address, "Connecting")
                val gatt = device.connectGatt(context, false, gattCallback)
                bluetoothViewModel.bluetoothGatt = gatt
                bluetoothViewModel.updateConnectedDevice(device)
                Log.d("BluetoothGatt", "Connecting to GATT server...")
            } else {
                Log.d("BluetoothGatt", "Already connected to GATT.")
            }
        }
    }

    // Function to disconnect from a Bluetooth device and restart the scan
    fun disconnectFromGattDevice(device: BluetoothDevice) {
        val gatt = bluetoothViewModel.bluetoothGatt
        if (gatt != null) { // Ensure there's an active GATT connection
            Log.d("BluetoothGatt", "Attempting to disconnect from device: ${device.address}")
            gatt.disconnect()
            bluetoothViewModel.updateConnectionState(device.address, "Idle")
            // After disconnecting, close the GATT client to release resources
            Log.d("BluetoothGatt", "Closing GATT connection for device: ${device.address}")
            gatt.close()
            bluetoothViewModel.clearConnection()
            Log.d("BluetoothGatt", "Disconnected and GATT closed.")
        } else {
            Log.d("BluetoothGatt", "No active GATT connection to disconnect.")
        }

        // Clear cached devices and restart scanning after a delay
        coroutineScope.launch {
            delay(1000)  // Wait for 1 second to allow the device to become rediscoverable
            discoveredDevices.clear()  // Clear the list of discovered devices
            if (scanning) {
                startBluetoothScan(
                    bluetoothAdapter,
                    discoveredDevices,
                    context,
                    scanCallback,
                    coroutineScope,
                    onScanStopped = {
                        scanning = false  // Update the scanning state or handle other UI changes
                        Log.d("Bluetooth", "Scan stopped after timeout")
                    })
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back navigation button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(onClick = { navController.navigateUp() }) {
                Text(text = "Back", color = Color.White)
            }
        }

        if (isBluetoothEnabled) {
            if (discoveredDevices.isEmpty() && connectedDeviceAddress == null) {
                Text(
                    text = "No devices found. Try searching again.",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxHeight(0.8f)) {
                    // Manually add connected device to the list if it's still connected
                    val deviceAddress = bluetoothViewModel.connectedDeviceAddress.value
                    if (!deviceAddress.isNullOrEmpty()) {
                        val connectedDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                        if (connectedDevice != null) {
                            item {
                                BluetoothDeviceRow(
                                    device = connectedDevice,
                                    connectToGatt = { deviceToConnect ->
                                        coroutineScope.launch {
                                            disconnectFromGattDevice(deviceToConnect)
                                        }
                                    },
                                    connectionState = "Connected"
                                )
                            }
                        }
                    }

                    // Display discovered devices
                    items(discoveredDevices) { device ->
                        val connectedDeviceAddress =
                            bluetoothViewModel.connectedDeviceAddress.value

                        // Compare device address with connected device address
                        if (device.address != connectedDeviceAddress) {
                            BluetoothDeviceRow(
                                device = device,
                                connectToGatt = { deviceToConnect ->
                                    coroutineScope.launch {
                                        connectToGattDevice(deviceToConnect, context)
                                    }
                                },
                                connectionState = connectionStates[device.address] ?: "Idle"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scanning = !scanning
                    if (scanning) {
                        if (hasBluetoothPermissions(context)) {
                            if (scanCallback == null) {
                                scanCallback = getScanCallback(discoveredDevices)
                            }
                            Log.d("Bluetooth", "Starting Bluetooth scans")
                            startBluetoothScan(
                                bluetoothAdapter,
                                discoveredDevices,
                                context,
                                scanCallback,
                                coroutineScope,
                                onScanStopped = {
                                    // Callback to stop scanning and update UI
                                    scanning = false  // Stop scanning and update the button text
                                }
                            )
                        } else {
                            requestPermissions()
                        }
                    } else {
                        Log.d("Bluetooth", "Stopping Bluetooth scans")
                        stopBluetoothScan(bluetoothAdapter, context, scanCallback)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (scanning) "Stop Searching" else "Search Again",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        } else {
            Text("Bluetooth is disabled. Please enable Bluetooth to scan for devices.")
        }
    }
}


@Composable
fun BluetoothDeviceRow(
    device: BluetoothDevice,
    connectToGatt: (BluetoothDevice) -> Unit,
    connectionState: String
) {
    val coroutineScope = rememberCoroutineScope()  // CoroutineScope for handling side effects

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = device.name ?: "Unknown Device", fontSize = 16.sp)
            Text(text = device.address, fontSize = 14.sp, color = Color.Gray)
        }
        Button(
            onClick = {
                // Use coroutine scope to trigger the side-effect outside the composable context
                coroutineScope.launch {
                    connectToGatt(device)
                }
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = when (connectionState) {
                    "Idle" -> "Connect"
                    "Connecting" -> "Connecting..."
                    "Connected" -> "Disconnect"
                    else -> "Connect"
                }
            )
        }
    }
}

// Function to check if Bluetooth permissions are granted
fun hasBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}

// Function to start BLE scanning
fun startBluetoothScan(
    bluetoothAdapter: BluetoothAdapter?,
    discoveredDevices: MutableList<BluetoothDevice>,
    context: Context,
    scanCallback: ScanCallback?,
    coroutineScope: CoroutineScope,
    onScanStopped: () -> Unit
) {
    if (hasBluetoothPermissions(context)) {
        try {
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            if (scanCallback != null) {
                Log.d("Bluetooth", "Starting BLE scan")
                scanner?.startScan(scanCallback)

                // Stop scanning after 20 seconds
                coroutineScope.launch {
                    delay(20000)  // 20 seconds delay
                    Log.d("Bluetooth", "Stopping BLE scan after 20 seconds")
                    stopBluetoothScan(bluetoothAdapter, context, scanCallback)

                    onScanStopped()
                }
            }
        } catch (e: SecurityException) {
            Log.e("Bluetooth", "BLE Permission denied: ${e.message}")
        }
    } else {
        Log.d("Bluetooth", "Bluetooth permissions are not granted for BLE scan.")
    }
}

// Function to stop BLE scanning
fun stopBluetoothScan(
    bluetoothAdapter: BluetoothAdapter?,
    context: Context,
    scanCallback: ScanCallback?
) {
    if (hasBluetoothPermissions(context)) {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        Log.d("Bluetooth", "Stopped BLE scan")
    }
}

// Function to get the ScanCallback for BLE device discovery
fun getScanCallback(discoveredDevices: MutableList<BluetoothDevice>): ScanCallback {
    return object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = device.name
                if (!discoveredDevices.contains(device) && deviceName != null) {
                    discoveredDevices.add(device)
                    Log.d(
                        "Bluetooth",
                        "BLE Device found: ${device.name ?: "Unknown"} - ${device.address}"
                    )
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { result ->
                val device = result.device
                val deviceName = device.name
                if (!discoveredDevices.contains(device) && deviceName != null) {
                    discoveredDevices.add(device)
                    Log.d("Bluetooth", "Batch BLE Device found: ${device.name ?: "Unknown"}")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("Bluetooth", "BLE Scan failed with error code: $errorCode")
        }
    }
}