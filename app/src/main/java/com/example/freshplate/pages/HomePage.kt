package com.example.freshplate.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.navbar.navItem

@Composable
fun HomePage(modifier: Modifier = Modifier, authViewModel: AuthViewModel){

    val navItemLists = listOf(
        navItem("Home", Icons.Default.Home),
        navItem("Profile", Icons.Default.Person),
        navItem("Settings", Icons.Default.Settings),
    )

    var selectedIndex by remember {
        mutableStateOf(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar ={
            NavigationBar{
                navItemLists.forEachIndexed { index, navItem ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        label = { Text(text = navItem.label) },
                        icon = { Icon(imageVector = navItem.icon, contentDescription = "Icon") }
                    )
                }
            }
        }
    ) {innerPadding ->
        ContentScreen(modifier=Modifier.padding(innerPadding), authViewModel, selectedIndex)
    }
}

@Composable
fun ContentScreen(modifier: Modifier = Modifier, authViewModel: AuthViewModel, selectedIndex: Int) {

    val authState = authViewModel.authState.observeAsState()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login", builder = {
        composable("login"){
            LogIn(modifier, navController, authViewModel)
        }
        composable("signup"){
            SignUp(modifier, navController, authViewModel)
        }
        composable("homepage"){
            HomePage(modifier, authViewModel)
        }
    })


    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.UnAuthenticated -> {
                // Navigate to the login screen
                navController.navigate("login")
            }else -> Unit
        }
    }



    Column(modifier = modifier) {
        Button(
            onClick = {
                authViewModel.logout()
            },
            enabled = authState.value != AuthState.Loading,
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Logout", fontSize = 18.sp)
        }
    }
}