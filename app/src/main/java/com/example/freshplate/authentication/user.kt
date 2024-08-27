package com.example.freshplate.authentication

data class user(
    var username: String? = null,
    var email: String? = null,
    var bio: String? = null,
    var image: String? = null,
    var posts: List<String>? = null,
    var followers: List<String>? = null,
    var following: List<String>? = null
)