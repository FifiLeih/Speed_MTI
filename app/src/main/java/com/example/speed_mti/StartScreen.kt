package com.example.speed_mti

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController


@Composable
fun StartScreen(
    navController: NavHostController,
    bluetoothViewModel: BluetoothViewModel = viewModel()
) {
    // Collecting the state from ViewModel
    val connectedDeviceName by bluetoothViewModel.connectedDeviceName.collectAsState()  // Device name
    val receivedData by bluetoothViewModel.receivedData.collectAsState()  // Real-time received data
    val selectedFileName by bluetoothViewModel.selectedFileName.collectAsState()  // Selected file name

    // Access to the current context
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        HeaderSection(navController)

        // Body section for sending/receiving data
        BodySection(
            modifier = Modifier.weight(1f),
            onSendData = { data ->
                // Send data to the device
                bluetoothViewModel.sendDataToDevice(data)
            },
            onSendFile = {
                // Send the selected file to the device
                bluetoothViewModel.sendFileToDevice()  // No need to pass a file
            },
            onFileSelected = { uri ->
                // Handle file selection and call ViewModel method
                bluetoothViewModel.selectFile(context, uri)
            },
            receivedData = receivedData,  // Pass received data to BodySection
            selectedFileName = selectedFileName,  // Pass the selected file name
            context = context  // Pass the current context
        )

        // Footer with the connected device name
        FooterSection(deviceName = connectedDeviceName)
    }
}


@Composable
fun BodySection(
    modifier: Modifier = Modifier,
    onSendData: (ByteArray) -> Unit,  // Function to send data as ByteArray
    onSendFile: () -> Unit,  // Function to send a file
    onFileSelected: (Uri) -> Unit,  // Function to handle file selection
    receivedData: String,  // Data received from the device
    selectedFileName: String,  // Selected file name
    context: Context  // Context for file picker
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
        // Large Text Field for Receiving Data
        Box(
            modifier = Modifier
                .weight(2f)  // Make this box larger to resemble the large receiving area
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

        // Text Field for Sending Data
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Gray, shape = MaterialTheme.shapes.small)
                .padding(8.dp)
        ) {
            BasicTextField(
                value = sendData,
                onValueChange = { newValue -> sendData = newValue },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (sendData.isEmpty()) {
                        Text("Enter data to send", color = Color.LightGray)
                    }
                    innerTextField() // Adds the inner text field for user input
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // File picker button
        FilePickerLauncher(context) { uri ->
            onFileSelected(uri)  // Update ViewModel with the selected file
        }

        // Display selected file name
        Text(text = "Selected file: $selectedFileName")

        Spacer(modifier = Modifier.height(16.dp))

        // Send Button for Sending Data
        Button(
            onClick = {
                if (sendData.isNotEmpty()) {
                    // Convert text to ByteArray and send if data exists
                    onSendData(sendData.toByteArray())
                    sendData = ""  // Clear input after sending
                } else {
                    // Send the file if no text data is present
                    onSendFile()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text("Send", color = Color.White)
        }
    }
}

@Composable
fun FilePickerLauncher(context: Context, onFileSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onFileSelected(it) } // Pass the selected file's URI
    }

    Button(
        onClick = { launcher.launch("*/*") }, // Launch the file picker
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








