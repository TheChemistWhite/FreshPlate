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
import androidx.compose.runtime.mutableStateOf
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
fun navigationBar(modifier: Modifier, authViewModel: AuthViewModel){

    val authState = authViewModel.authState.observeAsState()
    val navController = rememberNavController()
    val navItemLists = listOf(
        navItem("Home", Icons.Default.Home),
        navItem("Settings", Icons.Default.Settings),
        navItem("Profile", Icons.Default.Person)
    )

    var selectedIndex by remember {
        mutableStateOf(0)
    }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar ={
            if(authState.value == AuthState.Authenticated){
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
        }
    ) {innerPadding ->
        navigationBarPreview(modifier = Modifier.padding(innerPadding), navController, authViewModel, selectedIndex)
    }
}

@Preview
@Composable
fun navigationBarPreview(modifier: Modifier = Modifier, navController: NavHostController, authViewModel: AuthViewModel, selectedIndex: Int){

    val authState = authViewModel.authState.observeAsState()
    if(authState.value == AuthState.Authenticated) {
        when(selectedIndex) {
            0->HomePage(modifier, authViewModel)
            1->ProfilePage(modifier, authViewModel)
            2->ProfilePage(modifier, authViewModel)
        }
    }else{
        LogIn(modifier, navController, authViewModel)
    }
}