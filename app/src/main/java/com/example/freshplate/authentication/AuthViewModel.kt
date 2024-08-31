package com.example.freshplate.authentication

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class AuthViewModel: ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private var db = Firebase.firestore

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus(){
        if(auth.currentUser != null){
            _authState.value = AuthState.Authenticated
        }else{
            _authState.value = AuthState.UnAuthenticated
        }
    }

    fun login(email: String, password: String){

        if(email.isEmpty()){
            _authState.value = AuthState.Error("Email cannot be empty")
            return
        }

        if(password.isEmpty()){
            _authState.value = AuthState.Error("Password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                }else{
                    _authState.value = AuthState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signup(name: String, surname: String, email: String, password: String){

        if(email.isEmpty()){
            _authState.value = AuthState.Error("Email cannot be empty")
            return
        }

        if(password.isEmpty()){
            _authState.value = AuthState.Error("Password cannot be empty")
            return
        }
        if(password.length < 6){
            _authState.value = AuthState.Error("Password must be at least 6 characters long")
            return
        }

        if(name.isEmpty()){
            _authState.value = AuthState.Error("Username cannot be empty")
            return
        }

        if(surname.isEmpty()){
            _authState.value = AuthState.Error("Username cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        val user = hashMapOf(
            "name" to name,
            "surname" to surname,
            "email" to email,
            "bio" to "",
            "username" to "",
            "image" to "",
            "posts" to listOf<String>(),
            "followers" to listOf<String>(),
            "following" to listOf<String>()
        )
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                    db.collection("users")
                        .document(auth.currentUser!!.uid)
                        .set(user).addOnSuccessListener {
                            Log.d("TAG", "User added")
                        }.addOnFailureListener {
                            Log.w("TAG", "Error adding user", it)
                        }
                }else{
                    _authState.value = AuthState.Error(task.exception?.message ?: "Registration failed")
                }
            }
    }

    fun logout(user: user){
        user.name = ""
        user.surname = ""
        user.email = ""
        user.username = ""
        user.bio = ""
        user.image = ""
        user.posts = listOf()
        user.followers = listOf()
        user.following = listOf()
        auth.signOut()
        _authState.value = AuthState.UnAuthenticated
    }

    fun update(user: user){

        if(user.name?.isEmpty() == true){
            _authState.value = AuthState.Error("Username cannot be empty")
            return
        }

        if(user.surname?.isEmpty() == true){
            _authState.value = AuthState.Error("Username cannot be empty")
            return
        }
        _authState.value = AuthState.Loading
        db.collection("users").document(auth.currentUser!!.uid)
                .update(
                mapOf(
                    "name" to user.name,
                    "surname" to user.surname,
                    "bio" to user.bio,
                    "username" to user.username,
                    "image" to user.image
                )
            ).addOnCompleteListener {
                if(it.isSuccessful) {
                    Log.d("TAG", "User updated")
                    _authState.value = AuthState.Authenticated
                }else{
                    _authState.value = AuthState.Error(it.exception?.message ?: "Update failed")
                }
            }.addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Update failed")
            }
    }

}

sealed class AuthState{
    data object Authenticated: AuthState()
    data object UnAuthenticated: AuthState()
    data object Loading:AuthState()

    data class Error(val message: String): AuthState()
}