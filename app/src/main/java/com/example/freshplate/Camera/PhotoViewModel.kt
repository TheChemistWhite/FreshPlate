package com.example.freshplate.Camera

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PhotoViewModel : ViewModel() {

    private val _photoUrl = MutableStateFlow<String?>(null)
    val photoUrl = _photoUrl.asStateFlow()

    fun updatePhotoUrl(url: String) {
        _photoUrl.value = url
    }

    val capturedBitmap = mutableStateOf<Bitmap?>(null)
}
