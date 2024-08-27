package com.example.freshplate.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.freshplate.R
import com.example.freshplate.authentication.AuthState
import com.example.freshplate.authentication.AuthViewModel
import com.example.freshplate.authentication.user
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun ProfilePage(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {

    val authState = authViewModel.authState.observeAsState()

    // Mock data for now
    var username by remember { mutableStateOf("John Doe") }
    var bio by remember { mutableStateOf("This is my bio.") }
    val postsCount by remember { mutableIntStateOf(10) }
    val followersCount by remember { mutableIntStateOf(250) }
    val followingCount by remember { mutableIntStateOf(180) }
    var profileImageUrl by remember { mutableStateOf("") } // Image URL from Firebase Storage

    LaunchedEffect(authState.value) {
        if (authState.value == AuthState.Authenticated) {
            // Fetch user data from Firebase when authenticated
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid!!)

            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Assuming user has username, bio, profileImage, etc.
                    val user = snapshot.getValue(user::class.java)
                    username = user?.username ?: "John Doe"
                    //bio = user?.bio ?: "This is my bio."
                    //profileImageUrl = user?.image ?: ""
                    // You can also update postsCount, followersCount, followingCount as needed
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Profile Section with Image, Username and Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image
            Image(
                painter = if (profileImageUrl.isEmpty())
                    painterResource(id = R.drawable.freshplate)
                else painterResource(id = R.drawable.freshplate),
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // User Stats (Posts, Followers, Following)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$postsCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "Posts", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$followersCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "Followers", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$followingCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "Following", fontSize = 14.sp)
            }
        }

        // Username and Bio
        Text(text = username, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = bio, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Edit Profile Button
        Button(
            onClick = {
                // Handle edit profile logic
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Edit Profile")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Button(
            onClick = {
                authViewModel.logout()
            },
            enabled = authState.value != AuthState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Grid Layout for User Posts (Simple Placeholder for now)
        Text(text = "Posts", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        // TODO: Add grid of user posts
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
        ) {
            Text(text = "User's posts will be displayed here", modifier = Modifier.align(Alignment.Center))
        }
    }
}


