package com.example.objectai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.objectai.Constants.LABELS_PATH
import com.example.objectai.Constants.MODEL_PATH
import java.util.concurrent.Executors

@Composable
fun ObjectDetectionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val boundingBoxesState = remember { mutableStateOf(emptyList<BoundingBox>()) }
    val inferenceTimeState = remember { mutableStateOf(0L) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember { Detector(context, MODEL_PATH, LABELS_PATH, object : Detector.DetectorListener {
        override fun onEmptyDetect() {
            boundingBoxesState.value = emptyList()
        }

        override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
            // Handle detection results
            Log.d("ObjectDetection", "Detected $boundingBoxes in $inferenceTime ms")
            boundingBoxesState.value = boundingBoxes
            inferenceTimeState.value = inferenceTime
        }
    }) }
    val hasPermission = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    // Request Camera Permission
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission.value = it
    }

    LaunchedEffect(Unit) {
        if (!hasPermission.value) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission.value) {
        LaunchedEffect(Unit) {
            detector.setup()
        }

        DisposableEffect(Unit) {
            onDispose {
                detector.clear()
                executor.shutdown()
            }
        }

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Image Analysis
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also {
                            it.setAnalyzer(executor) { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                detector.detect(bitmap)
                                imageProxy.close()
                            }
                        }

                    // Bind use cases
                    cameraProvider.unbindAll()
                    try {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                        preview.setSurfaceProvider(this.surfaceProvider)
                    } catch (exc: Exception) {
                        Log.e("ObjectDetection", "Use case binding failed", exc)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Text("Camera permission is required to use this feature.", modifier = Modifier.padding(16.dp))
    }
}

// Extension Function to Convert ImageProxy to Bitmap
private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    val matrix = Matrix()
    if (imageInfo.rotationDegrees != 0) {
        matrix.postRotate(imageInfo.rotationDegrees.toFloat())
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}


