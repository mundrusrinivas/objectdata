package com.example.objectai

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.objectai.ui.theme.ObjectAITheme

class MainActivity : ComponentActivity() {
    private lateinit var cameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register for camera permission result
        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted
                setContent {
                    ObjectAITheme {
                        MainApp()
                    }
                }
            } else {
                // Permission denied
                Log.e("Permission", "Camera permission not granted")
                setContent {
                    ObjectAITheme {
                       // PermissionDeniedScreen()
                    }
                }
            }
        }

        // Launch the permission request
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)

    }
}
