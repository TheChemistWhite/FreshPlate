package com.example.freshplate.Components

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun LoadingLogo(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoView = remember {
        LoadingLogoView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    AndroidView(
        factory = { logoView },
        modifier = modifier
    )
}