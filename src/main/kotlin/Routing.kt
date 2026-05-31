package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
                        h1 { +"Authentication impl (Topic 17)" }

                        if (session == null) {
                            a(href = "/login", classes = "btn") { +"1. Login (sets session)" }
                        } else {
                            p { +"Logged in as: ${session.name} | Role: ${session.role} | Dept: ${session.department}" }
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

                            // Show only what DataService allows
                            visibleDocs.forEach { doc ->
                                li {
                                    a(href = "/document/${doc.id}") {
                                        +"Document ${doc.id} - ${if (doc.department == session?.department) "Your Department" else "Dept: ${doc.department}"} (ABAC)"
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
                    }
                }
            }
        }

        post("/login") {
            val params = call.receiveParameters()
            val user = params["user"] ?: ""
            val pass = params["pass"] ?: ""

            // validateUser from DataService returns whole session with department logic
            val sessionRecord = DataService.validateUser(user, pass)
            if (sessionRecord != null) {
                call.sessions.set(sessionRecord)
                call.respondRedirect("/")
            } else {
                call.respondText("Invalid username or password!", status = HttpStatusCode.Unauthorized)
            }
        }

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
                call.respondText("Document: ${doc.content} | Department: ${doc.department}")
            } else {
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        // Form for admin GET
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
                                // DODANO: Pole na departament
                                p { +"Department: "; textInput(name = "new_dept") }
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

        // User addition POST
        post("/admin/add-user") {
            val session = call.sessions.get<UserSession>()
            val params = call.receiveParameters()
            val newUser = params["new_user"] ?: ""
            val newPass = params["new_pass"] ?: ""
            val newDept = params["new_dept"] ?: "GENERAL" // DODANO: Odbiór departamentu
            val newRole = try { Role.valueOf(params["new_role"] ?: "USER") } catch (e: Exception) { Role.USER }

            val success = DataService.addUser(session, newUser, newPass, newRole, newDept)

            if (success) {
                call.respondText("User $newUser added successfully to $newDept with role $newRole!")
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
                            h1 { +"Create New Document for ${session.department}" }
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

        // Document add POST
        post("/add-document") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                val content = call.receiveParameters()["content"] ?: ""

                val success = DataService.addDocument(session.department, content)

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