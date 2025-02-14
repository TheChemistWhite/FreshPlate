package com.example.freshplate.pages

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.freshplate.API.RecipeViewModel
import com.example.freshplate.Camera.CameraPreview
import com.example.freshplate.Camera.MainViewModel
import com.example.freshplate.Camera.PhotoBottomSheetContent
import com.example.freshplate.Camera.PhotoViewModel
import com.example.freshplate.Components.LoadingLogo
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID


object Camera{
    internal val CAMERAX_PERMISSION = arrayOf(
        Manifest.permission.CAMERA,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPage(navController: NavHostController, photoViewModel: PhotoViewModel, recipeViewModel: RecipeViewModel,onPhotoUrlReceived: (String) -> Unit){

    val applicationContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val db = Firebase.firestore


    var isLoading by remember { mutableStateOf(false) }

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


    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            isLoading = true
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, uri)
                uploadImageToFirebase(
                    bitmap = bitmap,
                    onSuccess = { downloadUrl ->
                        val photoDocument = hashMapOf(
                            "url" to downloadUrl,
                            "timestamp" to FieldValue.serverTimestamp()
                        )

                        db.collection("photos")
                            .add(photoDocument)
                            .addOnSuccessListener {
                                isLoading = false
                                val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
                                val encodedPhotoUrl = URLEncoder.encode(downloadUrl, StandardCharsets.UTF_8.toString())
                                val safeUri = Uri.encode(uri.toString())
                                val base64Uri = uri.toString().toBase64()
                                val base64PhotoUrl = downloadUrl.toBase64()
                                navController.navigate("photo_preview/$base64Uri/$base64PhotoUrl")
                            }
                            .addOnFailureListener { e ->
                                // Only log if it's not a permission error
                                if (!e.message?.contains("PERMISSION_DENIED")!!) {
                                    Log.e("Firestore", "Error saving gallery photo", e)
                                }
                                // Continue with navigation since we know the upload succeeded
                                isLoading = false
                                val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
                                val encodedPhotoUrl = URLEncoder.encode(downloadUrl, StandardCharsets.UTF_8.toString())
                                val safeUri = Uri.encode(uri.toString())
                                val base64Uri = uri.toString().toBase64()
                                val base64PhotoUrl = downloadUrl.toBase64()
                                navController.navigate("photo_preview/$base64Uri/$base64PhotoUrl")
                            }
                    },
                    onFailure = { exception ->
                        isLoading = false
                        Log.e("Firebase", "Error uploading gallery image", exception)
                    }
                )
            } catch (e: Exception) {
                isLoading = false
                Log.e("Gallery", "Error loading image", e)
            }
        }
    }

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val controller = remember {
        LifecycleCameraController(applicationContext).apply{
            bindToLifecycle(lifecycleOwner)
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE
            )

        }
    }

    val viewModel = viewModel<MainViewModel>()
    val bitmaps by viewModel.bitmaps.collectAsState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            PhotoBottomSheetContent(
                bitmap = bitmaps,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    ) {padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Red)
        ){
            CameraPreview(
                controller = controller,
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Green)
            )

            bitmaps.lastOrNull()?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingLogo(
                        modifier = Modifier
                            .size(300.dp)
                            .background(Color.Black)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceAround
            ){
                IconButton(
                    onClick = {
                        getContent.launch("image/*")
                    },
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Open Gallery",
                        tint = if (isLoading) Color.Gray else Color.White
                    )
                }
                IconButton(
                    onClick = {
                        isLoading = true
                        takePhoto(
                            controller = controller,
                            onPhotoTaken = { bitmap ->
                                // First save locally
                                val uri = saveImageToExternalStorage(bitmap, applicationContext)

                                // Then upload to Firebase
                                uploadImageToFirebase(
                                    bitmap = bitmap,
                                    onSuccess = { downloadUrl ->
                                        // Try to save to Firestore, but don't block on failure
                                        val photoDocument = hashMapOf(
                                            "url" to downloadUrl,
                                            "timestamp" to FieldValue.serverTimestamp()
                                        )

                                        db.collection("photos")
                                            .add(photoDocument)
                                            .addOnSuccessListener {
                                                Log.d("Firestore", "Photo document saved successfully")
                                            }
                                            .addOnFailureListener { e ->
                                                // Only log if it's not a permission error
                                                if (!e.message?.contains("PERMISSION_DENIED")!!) {
                                                    Log.e("Firestore", "Error saving photo document", e)
                                                }
                                            }
                                            .addOnCompleteListener {
                                                // Always proceed to preview, even if Firestore save failed
                                                isLoading = false
                                                val base64Uri = uri.toString().toBase64()
                                                val base64PhotoUrl = downloadUrl.toBase64()
                                                navController.navigate("photo_preview/$base64Uri/$base64PhotoUrl")
                                            }
                                    },
                                    onFailure = { exception ->
                                        Log.e("Firebase", "Error uploading image", exception)
                                        isLoading = false
                                    }
                                )
                            },
                            context = applicationContext
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Take Photo",
                        tint = if (isLoading) Color.Gray else Color.White
                    )
                }
            }
        }
    }
}

private fun takePhoto(
    controller: LifecycleCameraController,
    onPhotoTaken: (Bitmap) -> Unit,
    context: Context
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)

                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                val rotatedBitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    0,
                    0,
                    image.width,
                    image.height,
                    matrix,
                    true
                )
                onPhotoTaken(rotatedBitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                Log.e("Camera", "Error taking photo", exception)
            }
        }
    )
}

private fun saveImageToExternalStorage(bitmap: Bitmap, context: Context): Uri {
    val contentResolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FreshPlate")
    }

    val imageUri: Uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!

    val outputStream: OutputStream? = contentResolver.openOutputStream(imageUri)

    outputStream?.use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }

    return imageUri
}

private fun uploadImageToFirebase(
    bitmap: Bitmap,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val storageRef = FirebaseStorage.getInstance().reference
    val imageRef = storageRef.child("photos/${UUID.randomUUID()}.jpg")

    // Convert bitmap to byte array
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    val imageData = baos.toByteArray()

    // Upload file
    imageRef.putBytes(imageData)
        .addOnSuccessListener { taskSnapshot ->
            // Get download URL
            imageRef.downloadUrl
                .addOnSuccessListener { downloadUrl ->
                    onSuccess(downloadUrl.toString())
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}


@Composable
private fun hasRequiredPermissions(context: Context): Boolean {
    return Camera.CAMERAX_PERMISSION.all {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun String.toBase64(): String {
    return Base64.encodeToString(this.toByteArray(), Base64.NO_WRAP)
}


