package com.example.freshplate.navbar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.pages.HomePage
import com.example.freshplate.pages.LogIn
import com.example.freshplate.pages.ProfilePage
import com.example.freshplate.pages.SignUp

@Composable
fun NavigationBar(modifier: Modifier, authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.observeAsState()
    val navController = rememberNavController()

    val navItems = listOf(
        navItem("Home", Icons.Default.Home),
        navItem("Settings", Icons.Default.Settings),
        navItem("Profile", Icons.Default.Person)
    )

    var selectedIndex by remember { mutableIntStateOf(0) }

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.UnAuthenticated -> {
                // Navigate to login if the user is unauthenticated
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.Authenticated -> {
                // Ensure we navigate to the homepage after login
                navController.navigate("homepage") {
                    popUpTo("login") { inclusive = true }
                }
            }
            else -> Unit
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
                                    1 -> navController.navigate("settings")
                                    2 -> navController.navigate("profile")

                                    else -> Unit
                                }
                            },
                            label = { Text(item.label) },
                            icon = { Icon(imageVector = item.icon, contentDescription = null) }
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
                ProfilePage(modifier = Modifier, authViewModel = authViewModel)
            }
            composable("settings") {
                // Add your Settings page here
            }
        }
    }
}


@Preview
@Composable
fun NavigationBarPreview(modifier: Modifier = Modifier, navController: NavHostController, authViewModel: AuthViewModel, selectedIndex: Int) {
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
            1 -> ProfilePage(modifier, authViewModel)
            2 -> ProfilePage(modifier, authViewModel)
        }
    }
}
