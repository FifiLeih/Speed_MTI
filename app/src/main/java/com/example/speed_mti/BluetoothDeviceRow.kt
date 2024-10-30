package com.example.speed_mti

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BluetoothDeviceRow(
    device: BluetoothDevice,
    connectToGatt: (BluetoothDevice) -> Unit,
    connectionState: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = device.name ?: "Unknown Device", fontSize = 16.sp)
            Text(text = device.address, fontSize = 14.sp)
        }
        Button(
            onClick = { connectToGatt(device) },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(text = when (connectionState) {
                "Idle" -> "Connect"
                "Connecting" -> "Connecting..."
                "Connected" -> "Disconnect"
                else -> "Connect"
            })
        }
    }
}