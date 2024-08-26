package com.example.freshplate.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.StorageReference

class AuthViewModel: ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var storageReference : StorageReference

    var uid = auth.currentUser?.uid
    var dbRef = FirebaseDatabase.getInstance().getReference("Users")

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus(){
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

    fun signup(username: String, email: String, password: String){

        if(email.isEmpty()){
            _authState.value = AuthState.Error("Email cannot be empty")
            return
        }

        if(password.isEmpty()){
            _authState.value = AuthState.Error("Password cannot be empty")
            return
        }

        if(username.isEmpty()){
            _authState.value = AuthState.Error("Username cannot be empty")
            return
        }

        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val user = user(username, email, password)
                    dbRef.child(uid!!).setValue(user).addOnCompleteListener {
                        if(it.isSuccessful){

                        }else{

                        }
                    }
                    _authState.value = AuthState.Authenticated

                }else{
                    _authState.value = AuthState.Error(task.exception?.message ?: "Registration failed")
                }
            }
    }

    fun logout(){
        auth.signOut()
        _authState.value = AuthState.UnAuthenticated
    }

}

sealed class AuthState{
    object Authenticated: AuthState()
    object UnAuthenticated: AuthState()
    object Loading:AuthState()

    data class Error(val message: String): AuthState()
}