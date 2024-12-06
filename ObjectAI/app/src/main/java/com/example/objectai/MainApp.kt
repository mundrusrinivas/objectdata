package com.example.objectai

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    LockOrientation()
    // Define navigation routes
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            SuspiciousDetectionApp(navController)
        }
        composable("objectDetection") {
            val intent = Intent(context, ObjectRoundActivity::class.java)
            context.startActivity(intent)
        }
        composable("roboFlow") {
            val intent = Intent(context, RoboDetectActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@Composable
fun LockOrientation() {
    val context = LocalContext.current
    SideEffect {
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}
