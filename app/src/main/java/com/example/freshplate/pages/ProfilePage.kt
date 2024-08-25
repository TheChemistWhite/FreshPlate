package com.example.freshplate.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel

@Composable
fun ProfilePage(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {

    val authState = authViewModel.authState.observeAsState()

    Column(modifier = modifier) {
        Button(
            onClick = {
                authViewModel.logout()
            },
            enabled = authState.value != AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Logout", fontSize = 18.sp)
        }
    }

}


