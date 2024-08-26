package com.example.freshplate.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.example.freshplate.authentication.AuthViewModel

@Composable
fun HomePage(modifier: Modifier = Modifier, authViewModel: AuthViewModel){

    val authState = authViewModel.authState.observeAsState()

}