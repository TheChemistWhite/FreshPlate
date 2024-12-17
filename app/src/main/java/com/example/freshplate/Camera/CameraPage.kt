package com.example.freshplate.Camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.freshplate.authentication.AuthViewModel


object Camera{
    internal val CAMERAX_PERMISSION = arrayOf(
        Manifest.permission.CAMERA
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPage(){

    val applicationContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    if(!hasRequiredPermissions(applicationContext)){
        if (applicationContext is Activity) {
            ActivityCompat.requestPermissions(
                applicationContext,
                Camera.CAMERAX_PERMISSION,
                0
            )
        }
        return
    }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val controller = remember {
        LifecycleCameraController(applicationContext).apply{
            bindToLifecycle(lifecycleOwner)
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE
            )

        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {}
    ) {padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(androidx.compose.ui.graphics.Color.Red)
        ){
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Green)
            )
        }
    }
}

@Composable
private fun hasRequiredPermissions(context: android.content.Context): Boolean {
    return Camera.CAMERAX_PERMISSION.all {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}


