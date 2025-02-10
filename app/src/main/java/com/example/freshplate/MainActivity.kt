package com.example.freshplate

import android.Manifest
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
import com.example.freshplate.navbar.NavigationBar
import com.example.freshplate.ui.theme.FreshPlateTheme
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        FirebaseFirestore.getInstance()

        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
            == ConnectionResult.SUCCESS) {
            // Play Services is available


            val authViewModel: AuthViewModel by viewModels()
            setContent {
                FreshPlateTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavigationBar(modifier = Modifier.padding(innerPadding), authViewModel)
                    }
                }
            }
        }
    }
    companion object Camera{
        internal val CAMERAX_PERMISSION = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
}