import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.speed_mti.BluetoothViewModel

@Composable
fun BluetoothDeviceListScreen(
    navController: NavHostController,
    bluetoothViewModel: BluetoothViewModel
) {
    val discoveredDevices = bluetoothViewModel.getDiscoveredDevices()
    var scanning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(onClick = { navController.navigateUp() }) {
                Text(text = "Back", color = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(discoveredDevices) { device ->
                val connectionState = bluetoothViewModel.connectionStates[device.address] ?: "Idle"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = device.name ?: "Unknown Device", fontSize = 18.sp)
                        Text(text = device.address, fontSize = 14.sp, color = Color.Gray)
                    }

                    // Connect/Disconnect button
                    Button(
                        onClick = {
                            if (connectionState == "Connected") {
                                bluetoothViewModel.disconnectDevice()
                            } else {
                                bluetoothViewModel.connectToDevice(device)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                    ) {
                        Text(
                            text = if (connectionState == "Connected") "Disconnect" else "Connect",
                            color = Color.White
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                scanning = !scanning
                if (scanning) bluetoothViewModel.startScan() else bluetoothViewModel.stopScan()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text(
                text = if (scanning) "Stop Searching" else "Search Again",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}



