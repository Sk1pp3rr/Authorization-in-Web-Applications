package com.example

import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun Application.configureSecurity() {

    install(Sessions) {
        // session config to remember loged in user
        cookie<UserSession>("MY_SESSION") {
            cookie.path = "/"
            cookie.extensions["SameSite"] = "lax"
        }
    }
}