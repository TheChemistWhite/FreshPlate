package com.example.freshplate.authentication

data class user(
    var username: String? = null,
    var email: String? = null,
    var password: String?=null,
    var bio: String? = null,
    var image: String? = null
)