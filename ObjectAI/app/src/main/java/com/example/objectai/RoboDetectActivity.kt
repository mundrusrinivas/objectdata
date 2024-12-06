package com.example.objectai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RoboDetectActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: RoboOverlay
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private var lastInferenceTime = 0L

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val INFERENCE_INTERVAL = 1000L // In milliseconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view
        setContentView(R.layout.activity_robo_detect)

        // Initialize views
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Initialize executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

             cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Image Analysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            // Select back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInferenceTime >= INFERENCE_INTERVAL) {
            lastInferenceTime = currentTime

            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(imageProxy)

            // Send bitmap to Roboflow
            sendImageToRoboflow(bitmap)
        }

        // Close the imageProxy
        imageProxy.close()
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer // Y
        val uBuffer = imageProxy.planes[1].buffer // U
        val vBuffer = imageProxy.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun sendImageToRoboflow(bitmap: Bitmap) {
        // Compress the bitmap to JPEG
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val byteArray = stream.toByteArray()

        // Create RequestBody
        val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)

        // Prepare API call
        val model = "weapon-detection-aoxpz" // Replace with your model name
        val version = "5" // Replace with your model version
        val apiKey = "rzW5Jy70MYpvjIVcXCLO" // Replace with your API key

        val call = RetrofitInstance.roboInstance.detectObjects(
            model = model,
            version = version,
            apiKey = apiKey,
            image = imagePart
        )

        // Execute API call
        call.enqueue(object : Callback<RoboflowResponse> {
            override fun onResponse(call: Call<RoboflowResponse>, response: Response<RoboflowResponse>) {
                if (response.isSuccessful) {
                    val predictions = response.body()?.predictions ?: emptyList()
                    // Process predictions
                    processPredictions(predictions, bitmap)
                } else {
                    Log.e(TAG, "Roboflow API Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RoboflowResponse>, t: Throwable) {
                Log.e(TAG, "Roboflow API Failure: ${t.message}")
            }
        })
    }

    private fun processPredictions(predictions: List<Prediction>, bitmap: Bitmap) {
        // Calculate scale factor between bitmap and previewView
        val scaleFactorX = previewView.width.toFloat() / bitmap.width.toFloat()
        val scaleFactorY = previewView.height.toFloat() / bitmap.height.toFloat()

        runOnUiThread {
            overlayView.setPredictions(predictions, scaleFactorX, scaleFactorY)
        }
    }

    private fun allPermissionsGranted() = RoboDetectActivity.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Handle permissions result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RoboDetectActivity.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this, "Permissions not granted by the user.", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider.unbindAll()
        cameraExecutor.shutdown()
    }
}