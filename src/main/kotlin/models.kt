package com.example

import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

///For simplicity, we will use simulation of the real db
@Serializable
enum class Role {ADMIN, USER}

// model of user session
@Serializable
data class UserSession(val name: String, val role: Role)

//example of document for ABAC (has an owner)
data class Document(val id: Int, val content: String, val owner: String)