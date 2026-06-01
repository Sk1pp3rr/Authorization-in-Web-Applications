package com.example

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.* // KLUCZOWY IMPORT: to on sprawia, że 'get' i 'post' działają jako routing!
import io.ktor.server.sessions.*
import kotlinx.html.*

fun Application.configureRouting() {
    routing {
        get("/") {
            val session = call.sessions.get<UserSession>()
            val visibleDocs = DataService.getVisibleDocuments(session)

            call.respondHtml {
                head { style { +CssAdd.getCss() } }
                body {
                    div("container") {
                        h1 { +"Authentication impl" }

                        if (session == null) {
                            a(href = "/login") { +"1. Login (sets session)" }
                        } else {
                            p { +"Logged in as: ${session.name} (${session.role})" }
                            a(href = "/logout") { b { +"Log out" } }
                            br()
                            a(href = "/dashboard") { +"Go to Dashboard" }
                            br()
                            a(href = "/add-document") { +"Add Document" }
                        }

                        if (session?.role == Role.ADMIN) {
                            p { b { +"Admin Options:" } }
                            a(href = "/admin/add-user") { +"[Add New User]" }
                            br()
                        }

                        hr()
                        h3 { +"Protected Areas:" }
                        ul {
                            li { a(href = "/admin/system-info") { +"Admin panel (RBAC)" } }

                            visibleDocs.forEach { doc ->
                                li {
                                    a(href = "/document/${doc.id}") {
                                        +"Document ${doc.id} - ${if (doc.owner == session?.name) "Your Own" else "Owner: ${doc.owner}"}"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }

        get("/login") {
            call.respondHtml {
                head { style { +CssAdd.getCss() } }
                body {
                    div("container") {
                        h1 { +"Login Page" }
                        form(action = "/login", method = FormMethod.post) {
                            p { +"Username: "; textInput(name = "user") }
                            p { +"Password: "; passwordInput(name = "pass") }
                            submitInput(classes = "btn") { value = "Login" }
                        }
                        hr()
                        // Przycisk logowania przez Google
                        a(href = "/login-google", classes = "btn") {
                            style = "background: #dd4b39;" // Kolor Google
                            +"Sign in with Google"
                        }
                    }
                }
            }
        }

        post("/login") {
            val params = call.receiveParameters()
            val user = params["user"] ?: ""
            val pass = params["pass"] ?: ""

            val role = DataService.validateUser(user, pass)
            if (role != null) {
                call.sessions.set(UserSession(user, role))
                call.respondRedirect("/")
            } else {
                call.respondText("Invalid username or password!", status = HttpStatusCode.Unauthorized)
            }
        }

        // --- BLOK OAUTH 2.0 ---
        authenticate("auth-oauth-google") {
            get("/login-google") {
                // To przekieruje do Google automatycznie, o ile skonfigurowałeś Security.kt
            }

            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()

                if (principal != null) {
                    // Wywołanie HTTP Clienta (zwróć uwagę na explicitly użyte applicationHttpClient)
                    val userInfo: GoogleUserInfo = applicationHttpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                        headers { append(HttpHeaders.Authorization, "Bearer ${principal.accessToken}") }
                    }.body()

                    // Pobieramy rolę lub rejestrujemy użytkownika "w locie"
                    val role = DataService.authenticateGoogleUser(userInfo.id, userInfo.name)

                    // Ustawienie sesji
                    val sessionName = "google_${userInfo.id}"
                    call.sessions.set(UserSession(sessionName, role))

                    call.respondRedirect("/")
                } else {
                    call.respondRedirect("/login")
                }
            }
        }
        // --- KONIEC BLOKU OAUTH ---

        // RBAC
        get("/admin/system-info") {
            val session = call.sessions.get<UserSession>()
            if (session?.role == Role.ADMIN) {
                call.respondText("Hello Administrator. Here they are your crazzy confidential information.")
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        // ABAC
        get("/document/{id}") {
            val session = call.sessions.get<UserSession>()
            val docId = call.parameters["id"]?.toIntOrNull()

            val doc = DataService.getDocumentIfAllowed(session, docId)

            if (doc != null) {
                call.respondText("Document: ${doc.content} | Owner: ${doc.owner}")
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        get("/admin/add-user") {
            val session = call.sessions.get<UserSession>()
            if (session?.role == Role.ADMIN) {
                call.respondHtml {
                    head { style { +CssAdd.getCss() } }
                    body {
                        div("container") {
                            h1 { +"Add New User (Admin Only)" }
                            form(action = "/admin/add-user", method = FormMethod.post) {
                                p { +"New Username: "; textInput(name = "new_user") }
                                p { +"New Password: "; passwordInput(name = "new_pass") }
                                p {
                                    +"Role: "
                                    select {
                                        name = "new_role"
                                        option { value = "USER"; +"User" }
                                        option { value = "ADMIN"; +"Admin" }
                                    }
                                }
                                submitInput(classes = "btn") { value = "Create User" }
                            }
                            br()
                            a(href = "/") { +"Back to Home" }
                        }
                    }
                }
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        post("/admin/add-user") {
            val session = call.sessions.get<UserSession>()
            val params = call.receiveParameters()
            val newUser = params["new_user"] ?: ""
            val newPass = params["new_pass"] ?: ""
            val newRole = try { Role.valueOf(params["new_role"] ?: "USER") } catch (e: Exception) { Role.USER }

            val success = DataService.addUser(session, newUser, newPass, newRole)

            if (success) {
                call.respondText("User $newUser added successfully with role $newRole!")
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        get("/add-document") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                call.respondHtml {
                    head { style { +CssAdd.getCss() } }
                    body {
                        div("container") {
                            h1 { +"Create New Document" }
                            form(action = "/add-document", method = FormMethod.post) {
                                div("form-group") {
                                    p { +"Content:" }
                                    textArea(cols = "40", rows = "5") { name = "content" }
                                }
                                submitInput(classes = "btn") { value = "Save Document" }
                            }
                            br()
                            a(href = "/") { +"Back to Home" }
                        }
                    }
                }
            } else {
                call.respondRedirect("/login")
            }
        }

        post("/add-document") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                val content = call.receiveParameters()["content"] ?: ""
                val success = DataService.addDocument(session.name, content)
                if (success) {
                    call.respondRedirect("/")
                } else {
                    call.respondText("Content cannot be empty", status = HttpStatusCode.BadRequest)
                }
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}