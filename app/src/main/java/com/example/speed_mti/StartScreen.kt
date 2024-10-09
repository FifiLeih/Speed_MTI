package com.example.speed_mti

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun StartScreen(navController: NavHostController, deviceSpeed: Int, connectedDeviceName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        HeaderSection(navController)
        BodySection(speed = deviceSpeed, modifier = Modifier.weight(1f))
        FooterSection(deviceName = connectedDeviceName)
    }
}

@Composable
fun BodySection(speed: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black) // Set background to black like in the image
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$speed",  // Display the received speed
                style = TextStyle(
                    fontSize = 72.sp, // Large text size for the number
                    fontWeight = FontWeight.Bold,
                    color = Color.White // White text color
                )
            )
            Text(
                text = "KM/H",
                style = TextStyle(
                    fontSize = 24.sp, // Medium text size for the unit
                    color = Color.Red // Red text color for KM/H
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { /* Handle Disable click */ },
                colors = ButtonDefaults.buttonColors(contentColor = Color.Red)
            ) {
                Text("Disable", color = Color.White)
            }
        }
    }
}

@Composable
fun HeaderSection(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black) // Use black for the header background
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ButtonMenu(navController = navController)
        HeaderImage()
    }
}

@Composable
fun FooterSection(deviceName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black) // Black background for footer
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Day Trip",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Disable",
                color = Color.Red, // Red text for disable
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Column {
            Text(
                text = "Device",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = deviceName,  // Display the connected device name or "DEMO"
                color = Color.Red,   // Red text for device status
                style = MaterialTheme.typography.bodyLarge
            )
        }

        TextButton(onClick = { /* Handle reset click */ }) {
            Text(
                text = "Reset",
                color = Color.Red // Red text for reset
            )
        }
    }
}

@Composable
fun ButtonMenu(navController: NavHostController) {
    IconButton(onClick = {
        navController.navigate("bluetoothDeviceListScreen")  // Navigate to BluetoothDeviceListScreen
    }) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = Color.White, // White icon color
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun HeaderImage() {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_background),
        contentDescription = "Header Image",
        modifier = Modifier.size(40.dp)
    )
}

// Example function to simulate receiving data from the device
@Composable
fun StartScreenWithDeviceData(navController: NavHostController, bluetoothViewModel: BluetoothViewModel = viewModel()) {
    // Use reactive state to track connected device name and speed
    val connectedDeviceName by bluetoothViewModel.connectedDeviceName
    var deviceSpeed by rememberSaveable { mutableStateOf(0) }

    // Simulate receiving new speed from the device
    LaunchedEffect(Unit) {
        deviceSpeed = 45  // Replace this with actual Bluetooth data handling
    }

    StartScreen(
        navController = navController,
        deviceSpeed = deviceSpeed,
        connectedDeviceName = connectedDeviceName  // Dynamically pass the connected device name
    )
}


