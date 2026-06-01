package com.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*

// Globalny klient HTTP używany przez OAuth do komunikacji z API Google
val applicationHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
    }
}

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<UserSession>("MY_SESSION") {
            cookie.path = "/"
            cookie.extensions["SameSite"] = "lax"
        }
    }

    // Instalacja i konfiguracja autoryzacji OAuth dla Google
    install(Authentication) {
        oauth("auth-oauth-google") {
            // Ścieżka zwrotna (Callback), do której Google przekieruje usera po logowaniu
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = "197335707112-31fpin0odt1e56f71e5vtahgua98u5lt.apps.googleusercontent.com",
                    clientSecret = "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",
                    defaultScopes = listOf(
                        "https://www.googleapis.com/auth/userinfo.profile",
                        "https://www.googleapis.com/auth/userinfo.email"
                    )
                )
            }
            client = applicationHttpClient
        }
    }
}