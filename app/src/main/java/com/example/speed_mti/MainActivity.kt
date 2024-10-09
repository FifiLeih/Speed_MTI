package com.example.speed_mti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainApp()  // Setup the navigation between screens
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val bluetoothViewModel: BluetoothViewModel = viewModel()  // Share ViewModel

    NavHost(
        navController = navController,
        startDestination = "startScreen"
    ) {
        composable("startScreen") {
            StartScreenWithDeviceData(navController = navController)
        }

        composable("bluetoothDeviceListScreen") {
            BluetoothDeviceListScreen(navController = navController, bluetoothViewModel = bluetoothViewModel)
        }
    }
}
