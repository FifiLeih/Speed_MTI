package com.example.speed_mti

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun StartScreen(
    navController: NavHostController,
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    context: Context
) {
    val connectedDeviceName by bluetoothViewModel.connectedDeviceName.collectAsState()
    val receivedData by bluetoothViewModel.receivedData.collectAsState()
    val selectedFileName by bluetoothViewModel.selectedFileName.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HeaderSection(navController)
        BodySection(
            modifier = Modifier.weight(1f),
            onSendFile = { bluetoothViewModel.startFileTransfer() },
            onFileSelected = { uri ->
                bluetoothViewModel.selectFile(
                    context,
                    uri
                )
            }, // Use context here
            receivedData = receivedData,
            selectedFileName = selectedFileName
        )
        FooterSection(deviceName = connectedDeviceName)
    }
}

@Composable
fun BodySection(
    modifier: Modifier = Modifier,
    onSendFile: () -> Unit,
    onFileSelected: (Uri) -> Unit,
    receivedData: String,
    selectedFileName: String
) {
    var sendData by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                .padding(8.dp)
        ) {
            Text(
                text = if (receivedData.isEmpty()) "No data received" else receivedData,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        FilePickerLauncher { uri -> onFileSelected(uri) }

        Text(text = "Selected file: $selectedFileName")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onSendFile()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text("Send", color = Color.White)
        }
    }
}

@Composable
fun FilePickerLauncher(onFileSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onFileSelected(it) }
    }

    Button(
        onClick = { launcher.launch("*/*") },
        modifier = Modifier.padding(8.dp)
    ) {
        Text("Select File")
    }
}

@Composable
fun HeaderSection(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
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
    Log.d("FooterSection", "Displaying device name: $deviceName")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
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
                color = Color.Red,
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
                text = deviceName,
                color = Color.Red,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        TextButton(onClick = { /* Handle reset click */ }) {
            Text(
                text = "Reset",
                color = Color.Red
            )
        }
    }
}

@Composable
fun ButtonMenu(navController: NavHostController) {
    IconButton(onClick = {
        navController.navigate("bluetoothDeviceListScreen")
    }) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = Color.White,
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
