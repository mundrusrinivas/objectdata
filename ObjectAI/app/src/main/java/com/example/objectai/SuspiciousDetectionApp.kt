package com.example.objectai

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SuspiciousDetectionApp(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var detectedObject by remember { mutableStateOf("No suspicious objects detected") }

    // PreviewView for CameraX
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val generativeModel = GenerativeModel(
       // modelName = "gemini-1.0-pro-vision-latest", // Use the specific model name
        modelName = "gemini-1.5-flash-001", // Use the specific model name
        apiKey = "AIzaSyAKw5jVUsgyaUjbmLSYPYmGuRy8H6vZfsc"
    )

    // Start Camera
    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val imageCapture = ImageCapture.Builder().build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e("CameraX", "Failed to start camera", e)
        }



        // Capture frames every 2 seconds
        scope.launch {
            while (true) {
                delay(200)
                captureFrame(imageCapture, context) { bitmap ->
                 //   val base64Image = bitmap.toBase64()

                    scope.launch {

                        try {
                            val response = generativeModel.generateContent(
                                content {
                                    image(bitmap)
                                    text("Give YES or NO if there is any crime, weapons, blood, fight and Describe the image not more than 30 words")
                                }
                            )

                            val name=response.text


                            if(name!=null){
                                if(name.contains("YES", ignoreCase = true)) {
                                    // Call function for "YES"
                                    val afterYes = name.substringAfter("YES.").trim()
                                    detectedObject = "Alert :\n  $afterYes"
                                    playAlarm(context)
                                }
                                else {
                                    val afterYes = "No abnormal activity detected"
                                    detectedObject = "Normal :\n  $afterYes"
                                }
                            }


                        }catch (e:Exception){
                            e.printStackTrace()
                        }

                    }


//                    scope.launch {
//                        val response = RetrofitInstance.api.detectSuspiciousObjects("AIzaSyCUW33z63FlUF40O6vz_hJJftE0L8hTGTc",content)
//                        response?.let {
//                            print(it)
//                            if (it!=null) {
//                                //detectedObject = it.description
//                                //playAlarm(context)
//                            }
//                        }
//
//                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Block 1: Remaining space to show some text
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 60.dp, end = 200.dp), // Leave space for Block 2
            verticalArrangement = Arrangement.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(), // Fills the allocated space
                factory = { previewView }
            )
        }

        // Block 2: Right side TextView
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(200.dp) // Set width for Block 2
                .align(Alignment.CenterEnd)
                .padding(8.dp),
        ) {
            Text(
                text = detectedObject,
                style = MaterialTheme.typography.bodyLarge
            )
        }


        // Block 3: Bottom Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp) // Adjust height as needed
                .align(Alignment.BottomCenter)
                .background(Color.White)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { navController.navigate("home") }) {
                Text(text = "IndeGenAious")
               // Text(text = "Basic")
            }
            Button(onClick = { navController.navigate("objectDetection") }) {
                Text(text = "Intelli AI+")
            }
            Button(onClick = { navController.navigate("roboFlow") }) {
                Text(text = "Intelli AI")
            }
        }
    }


    // UI Layout
//    Column(
//        modifier = Modifier.fillMaxSize()
//    ) {
//        // Camera and Status Section
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .weight(0.7f) // Allocates 70% of the vertical space
//        ) {
//            AndroidView(
//                modifier = Modifier.fillMaxSize(), // Fills the allocated space
//                factory = { previewView }
//            )
//
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(0.3f)
//                    .background(Color.LightGray),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.Center
//            ) {
//                Text(text = "Status")
//            }
//        }
//
//        // Buttons Section
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .weight(0.3f) // Allocates 30% of the vertical space
//                .background(Color.White),
//            horizontalArrangement = Arrangement.SpaceEvenly,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Button(onClick = { navController.navigate("home") }) {
//                Text(text = "Basic")
//            }
//            Button(onClick = { navController.navigate("objectDetection") }) {
//                Text(text = "Object Detection")
//            }
//            Button(onClick = { navController.navigate("roboFlow") }) {
//                Text(text = "Robo Flow")
//            }
//        }
//    }
}



fun captureFrame(imageCapture: ImageCapture, context: Context, onFrameCaptured: (Bitmap) -> Unit) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val bitmap = imageProxy.toBitmap()
                imageProxy.close()
                onFrameCaptured(bitmap)
            }
        }
    )
}

fun playAlarm(context: Context) {
    MediaPlayer.create(context, R.raw.beep_warning).apply {
        start()
    }
}

