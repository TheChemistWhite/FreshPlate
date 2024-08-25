package com.example.freshplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.navbar.navigationBar
import com.example.freshplate.ui.theme.FreshPlateTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authViewModel : AuthViewModel by viewModels()
        // Set up content using Compose
        setContent {
            FreshPlateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    navigationBar(modifier =  Modifier.padding(innerPadding), authViewModel)
                }
            }
        }
    }
}