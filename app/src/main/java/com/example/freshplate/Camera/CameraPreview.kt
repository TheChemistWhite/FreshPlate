package com.example.freshplate.Camera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            Log.d("CameraPreview", "AndroidView factory invoked")
            val previewView = PreviewView(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLUE) // Debug background
            }

            // Camera setup
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()

                    // Initialize the Preview use case
                    val previewUseCase = Preview.Builder().build()
                    previewUseCase.setSurfaceProvider(previewView.surfaceProvider)

                    // Select the back camera as the default
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Bind the lifecycle owner to the camera and use cases
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase
                    )
                    Log.d("CameraPreview", "Camera successfully bound to lifecycle")
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Error setting up camera: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}
