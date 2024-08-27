package com.example.freshplate.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.freshplate.R
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun SignUp(modifier: Modifier = Modifier, navController: NavHostController, authViewModel: AuthViewModel) {

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope() // Define coroutine scope

    LaunchedEffect(authState.value){
        when(authState.value){
            is AuthState.Authenticated -> {
                navController.navigate("homepage")
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState.value as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            }else -> Unit
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp) // Add padding to the whole screen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF005b61)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Image at the top
            Image(
                painter = painterResource(id = R.drawable.freshplate),
                contentDescription = "Fresh Plate",
                modifier = Modifier
                    .size(200.dp) // Adjust the size of the image
                    .padding(top = 16.dp), // Optional padding from the top
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp)) // Space between image and card

            // Card to wrap inputs and buttons
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Adding elevation to give a card look
                colors = CardDefaults.cardColors(containerColor = Color.White) // Card background set to white
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp) // Padding inside the card
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Username input field with icon
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Username Icon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Space between fields

                    // Email input field with icon
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp)) // Space between fields

                    // Password input field with icon
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation() // Hide password characters
                    )

                    Spacer(modifier = Modifier.height(32.dp)) // Space before buttons

                    // Sign Up button
                    Button(
                        onClick = {
                            keyboard?.hide()
                            coroutineScope.launch {
                                authViewModel.signup(username, email, password)
                            }
                        },
                        enabled = authState.value != AuthState.Loading,
                        modifier = Modifier.fillMaxWidth() // Make the button stretch across the card width
                    ) {
                        Text("Sign Up", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Space between Sign Up and Log In label

                    // Clickable "Log In" label
                    TextButton(onClick = {
                        navController.navigate("login")
                    }){
                        Text(text = "Have you just an account? LogIn")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SignUpPreview() {
    SignUp(modifier = Modifier, navController = NavHostController(LocalContext.current), authViewModel = AuthViewModel())
}
