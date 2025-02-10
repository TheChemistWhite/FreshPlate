package com.example.freshplate.navbar

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.freshplate.API.RecipeViewModel
import com.example.freshplate.pages.CameraPage
import com.example.freshplate.pages.PhotoPreviewPage
import com.example.freshplate.Camera.PhotoViewModel
import com.example.freshplate.R
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.authentication.user
import com.example.freshplate.pages.HomePage
import com.example.freshplate.pages.LogIn
import com.example.freshplate.pages.PostDetailPage
import com.example.freshplate.pages.ProfilePage
import com.example.freshplate.pages.RecipeResultPage
import com.example.freshplate.pages.SignUp
import com.example.freshplate.pages.UpdatePage
import com.example.freshplate.pages.UserProfilePage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

@Composable
fun NavigationBar(modifier: Modifier, authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.observeAsState()
    val navController = rememberNavController()
    val photoViewModel: PhotoViewModel = viewModel()
    val db = Firebase.firestore

    val recipeViewModel: RecipeViewModel = viewModel(viewModelStoreOwner = LocalViewModelStoreOwner.current!!)

    val navItems = listOf(
        navItem("Home", R.drawable.home),
        navItem("Camera", R.drawable.camera),
        navItem("Profile", R.drawable.profile)
    )

    var selectedIndex by remember { mutableIntStateOf(0) }

    val user = remember { user() }
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
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
                HomePage(modifier = Modifier, authViewModel = authViewModel, navController = navController)
            }
            composable("profile") {
                ProfilePage(user = user, modifier = Modifier, navController = navController,authViewModel = authViewModel)
            }
            composable("update") {
                UpdatePage(user = user, modifier = Modifier, navController = navController, authViewModel = authViewModel)
            }
            composable("camera") {
                CameraPage(
                    navController = navController,
                    photoViewModel = photoViewModel,
                    recipeViewModel = recipeViewModel,
                    onPhotoUrlReceived = { url ->

                        photoViewModel.updatePhotoUrl(url)
                        db.collection("photos")
                            .add(mapOf(
                                "imageUrl" to url,
                                "timestamp" to FieldValue.serverTimestamp()
                            ))
                    }
                )
            }
            composable(
                route = "photo_preview/{encodedUri}/{encodedPhotoUrl}",
                arguments = listOf(
                    navArgument("encodedUri") {
                        type = NavType.StringType
                    },
                    navArgument("encodedPhotoUrl") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("encodedUri")
                val encodedPhotoUrl = backStackEntry.arguments?.getString("encodedPhotoUrl")

                if (encodedUri != null && encodedPhotoUrl != null) {
                    val imageUri = Uri.parse(encodedUri.fromBase64())
                    val photoUrl = encodedPhotoUrl.fromBase64()

                    PhotoPreviewPage(
                        navController = navController,
                        recipeViewModel = recipeViewModel,
                        imageUri = imageUri,
                        photoUrl = photoUrl
                    )
                }
            }
            composable(
                route = "recipeResult/{title}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val recipe by recipeViewModel.currentRecipe.collectAsState()
                val userPhotoUrl by recipeViewModel.userPhotoUrl.collectAsState()

                if (recipe != null && userPhotoUrl != null) {
                    RecipeResultPage(
                        navController = navController,
                        recipe = recipe!!,
                        userPhotoUrl = userPhotoUrl!!
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            composable(
                route = "post/{postId}",
                arguments = listOf(
                    navArgument("postId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId")
                postId?.let {
                    PostDetailPage(
                        navController = navController,
                        postId = it
                    )
                }
            }
            composable(
                route = "user_profile/{userId}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                userId?.let {
                    UserProfilePage(
                        navController = navController,
                        userId = it
                    )
                }
            }

        }
    }
}

@Preview
@Composable
fun NavigationBarPreview(
    user: user,
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    recipeViewModel: RecipeViewModel,
    selectedIndex: Int,
    photoViewModel: PhotoViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val db = Firebase.firestore

    LaunchedEffect(authState.value) {
        if (authState.value != AuthState.Authenticated) {
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    if (authState.value == AuthState.Authenticated) {
        when (selectedIndex) {
            0 -> HomePage(modifier, authViewModel, navController)
            1 -> CameraPage(
                navController = navController,
                photoViewModel = photoViewModel,
                recipeViewModel = recipeViewModel,
                onPhotoUrlReceived = { url ->
                    photoViewModel.updatePhotoUrl(url)
                    db.collection("photos")
                        .add(mapOf(
                            "imageUrl" to url,
                            "timestamp" to FieldValue.serverTimestamp()
                        ))
                }
            )
            2 -> ProfilePage(user, modifier, navController, authViewModel)
        }
    }
}

private fun String.fromBase64(): String {
    return String(Base64.decode(this, Base64.NO_WRAP))
}