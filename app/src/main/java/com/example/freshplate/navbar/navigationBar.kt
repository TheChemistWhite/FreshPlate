package com.example.freshplate.navbar

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.freshplate.Camera.CameraPage
import com.example.freshplate.R
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.authentication.user
import com.example.freshplate.pages.HomePage
import com.example.freshplate.pages.LogIn
import com.example.freshplate.pages.ProfilePage
import com.example.freshplate.pages.SignUp
import com.example.freshplate.pages.UpdatePage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun NavigationBar(modifier: Modifier, authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.observeAsState()
    val navController = rememberNavController()

    val navItems = listOf(
        navItem("Home", R.drawable.home),
        navItem("Camera", R.drawable.camera),
        navItem("Profile", R.drawable.profile)
    )

    var selectedIndex by remember { mutableIntStateOf(0) }

    val user = remember { user() }
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            // Fetch user data from Firestore when authenticated
            val email = FirebaseAuth.getInstance().currentUser?.email ?: return@LaunchedEffect
            FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        for (document in it.result) {
                            Log.d(TAG, "UserData: ${document.id} => ${document.data}")
                            user.apply {
                                name = document.getString("name") ?: ""
                                surname = document.getString("surname") ?: ""
                                username = document.getString("username") ?: ""
                                bio = document.getString("bio") ?: ""
                                image = document.getString("image") ?: ""
                                posts = document.get("posts") as? List<String>
                                followers = document.get("followers") as? List<String>
                                following = document.get("following") as? List<String>
                            }
                            user.email = email
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", it.exception)
                    }
                }
        } else {
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Scaffold with navigation bar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (authState == AuthState.Authenticated) {
                NavigationBar {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = {
                                selectedIndex = index
                                // Only navigate if the destination exists
                                when (index) {
                                    0 -> navController.navigate("homepage")
                                    1 -> navController.navigate("camera")
                                    2 -> navController.navigate("profile")

                                    else -> Unit
                                }
                            },
                            label = { Text(item.label) },
                            icon = { Image(
                                painter = painterResource(id = item.icon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            ) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LogIn(modifier = Modifier, navController = navController, authViewModel = authViewModel)
            }
            composable("signup") {
                SignUp(modifier = Modifier, navController = navController, authViewModel = authViewModel)
            }
            composable("homepage") {
                HomePage(modifier = Modifier, authViewModel = authViewModel)
            }
            composable("profile") {
                ProfilePage(user = user, modifier = Modifier, navController = navController,authViewModel = authViewModel)
            }
            composable("update") {
                UpdatePage(user = user, modifier = Modifier, navController = navController, authViewModel = authViewModel)
            }
            composable("camera") {
                CameraPage()
            }
        }
    }
}

@Preview
@Composable
fun NavigationBarPreview(user: user, modifier: Modifier = Modifier, navController: NavHostController, authViewModel: AuthViewModel, selectedIndex: Int) {
    val authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        if (authState.value != AuthState.Authenticated) {
            navController.navigate("login") {
                popUpTo("login") { inclusive = true } // Avoids backstack buildup
            }
        }
    }

    if (authState.value == AuthState.Authenticated) {
        when (selectedIndex) {
            0 -> HomePage(modifier, authViewModel)
            1 -> CameraPage()
            2 -> ProfilePage(user, modifier, navController, authViewModel)
        }
    }
}
