package com.example.speed_mti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

    NavHost(
        navController = navController,
        startDestination = "startScreen"  // Define the starting screen
    ) {
        composable("startScreen") {
            StartScreenWithDeviceData(navController = navController)  // Use StartScreenWithDeviceData here
        }

        composable("bluetoothDeviceListScreen") {
            BluetoothDeviceListScreen(navController = navController)
        }
    }
}
