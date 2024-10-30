import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import com.example.speed_mti.PermissionsManager

class BluetoothScanManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val discoveredDevices: MutableList<BluetoothDevice>,
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onScanStopped: () -> Unit
) {

    private var scanCallback: ScanCallback? = null

    fun startBluetoothScan() {
        if (PermissionsManager(context).hasBluetoothPermissions()) {
            // Clear the list of discovered devices before starting a new scan
            discoveredDevices.clear()

            scanCallback = getScanCallback()
            bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
            coroutineScope.launch {
                delay(20000) // Stop scan after 20 seconds
                stopBluetoothScan()
                onScanStopped()
            }
        }
    }

    fun stopBluetoothScan() {
        if (PermissionsManager(context).hasBluetoothPermissions()) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            Log.d("Bluetooth", "Stopped BLE scan")
        }
    }

    private fun getScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    // Only add devices with a non-null name
                    if (device.name != null && !discoveredDevices.contains(device)) {
                        discoveredDevices.add(device)
                        Log.d("BluetoothScanManager", "Device found: ${device.name} - ${device.address}")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BluetoothScanManager", "Scan failed with error code: $errorCode")
            }
        }
    }
}
