package com.example

import kotlinx.serialization.Serializable

@Serializable
enum class Role { ADMIN, USER, PENDING } // Dodana rola PENDING dla nowych kont OAuth

@Serializable
data class UserSession(
    val username: String,
    val name: String,
    val role: Role,
    val department: String?
)
// ABAC: Department is what determine document access
data class Document(val id: Int, val content: String, val department: String)

@Serializable
data class GoogleUserInfo(
    val id: String,
    val name: String,
    val picture: String? = null
)

// User Model
data class UserRecord(
    val passwordHash: String,
    var role: Role,
    var department: String?,
    val googleId: String? = null
)