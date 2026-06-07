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
                        h1 { +"Awesome Application" }

                        if (session == null) {
                            a(href = "/login") { +"1. Login (sets session)" }
                        } else {
                            p { +"Logged in as: ${session.name} (${session.role})" }
                            if (session.department != null) {
                                p { +"Department: ${session.department}" }
                            }
                            a(href = "/logout") { b { +"Log out" } }
                            br()

                            // HANDLE TEMPORARY ACCOUNTS
                            if (session.role == Role.PENDING) {
                                div("warning") {
                                    h3 { style = "color: orange;"; +"Account pending approval" }
                                    p { +"Your temporary system ID is: "; b { +session.username } }
                                    p { +"Report it to the IT administrator so they can locate your account and assign you to the correct department." }
                                }
                            } else {
                                a(href = "/add-document") { +"[Add Document to your department]" }
                                br()
                                hr()
                                h3 { +"Protected Areas:" }
                                ul {
                                    li { a(href = "/admin/system-info") { +"Admin panel (RBAC)" } }

                                    visibleDocs.forEach { doc ->
                                        li {
                                            a(href = "/document/${doc.id}") {
                                                +"Document ${doc.id} [Department: ${doc.department}]"
                                            }
                                        }
                                    }
                                }
                            }

                            if (session.role == Role.ADMIN) {
                                hr()
                                p { b { +"Admin Options:" } }
                                a(href = "/admin/manage-users") { +"[Manage Users]" }
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
                        a(href = "/login-google", classes = "btn") {
                            style = "background: #dd4b39;"
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

            val userSession = DataService.validateUser(user, pass)
            if (userSession != null) {
                call.sessions.set(userSession)
                call.respondRedirect("/")
            } else {
                call.respondText("Invalid username or password!", status = HttpStatusCode.Unauthorized)
            }
        }

        // --- BLOK OAUTH 2.0 ---
        authenticate("auth-oauth-google") {
            get("/login-google") {}

            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                if (principal != null) {
                    val userInfo: GoogleUserInfo =
                        applicationHttpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                            headers { append(HttpHeaders.Authorization, "Bearer ${principal.accessToken}") }
                        }.body()

                    // Otrzymujemy pełną sesję (Imię i Nazwisko, Status PENDING)
                    val newSession = DataService.authenticateGoogleUser(userInfo.id, userInfo.name)
                    call.sessions.set(newSession)
                    call.respondRedirect("/")
                } else {
                    call.respondRedirect("/login")
                }
            }
        }

        // RBAC
        get("/admin/system-info") {
            val session = call.sessions.get<UserSession>()
            if (session?.role == Role.ADMIN) {
                call.respondText("Hello Administrator. System status: Stable.")
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
                call.respondText("CONTENT: ${doc.content} | Department ownership: ${doc.department}")
            } else {
                // Rzuca 403 jeśli np. ktoś z HR próbuje czytać dokument IT
                call.respond(HttpStatusCode.Forbidden)
            }
        }

        // ZARZĄDZANIE UŻYTKOWNIKAMI
        get("/admin/manage-users") {
            val session = call.sessions.get<UserSession>()
            if (session?.role != Role.ADMIN) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            // Pobieramy frazę z query param: /admin/manage-users?search=fraza
            val searchQuery = call.request.queryParameters["search"] ?: ""
            val users = if (searchQuery.isNotBlank()) DataService.searchUsers(searchQuery) else emptyMap()

            call.respondHtml {
                head { style { +CssAdd.getCss() } }
                body {
                    div("container") {
                        h1 { +"Administrator Panel: User Search" }

                        // Zakładki nawigacyjne
                        div {
                            style = "margin-bottom: 20px; padding: 10px; background: #eee;"
                            b { +"Navigation: " }
                            span { +"[ Search Accounts ]" }
                            +" | "
                            a(href = "/admin/pending-users") { +"[ Pending Approval (PENDING) ]" }
                            +" | "
                            a(href = "/") { +"[ Home Page ]" }
                        }

                        // Formularz wyszukiwarki
                        form(action = "/admin/manage-users", method = FormMethod.get) {
                            p {
                                +"Enter user ID/username: "
                                textInput(name = "search") { value = searchQuery }
                                +" "
                                submitInput(classes = "btn") { value = "Filter" }
                            }
                        }

                        hr()

                        if (searchQuery.isBlank()) {
                            p { i { +"Enter a phrase above to search the employee database." } }
                        } else if (users.isEmpty()) {
                            p {
                                style =
                                    "color: red;"; +"No user found with username containing: '$searchQuery'"
                            }
                        } else {
                            h3 { +"Search results for: '$searchQuery'" }
                            users.forEach { (username, record) ->
                                div {
                                    style =
                                        "border: 1px solid #ccc; padding: 15px; margin-bottom: 15px; background: #fdfdfd;"
                                    p { b { +"ID/Username in database: " }; +"$username" }
                                    p { +"Role: ${record.role} | Assigned Department: ${record.department ?: "None"}" }

                                    form(action = "/admin/manage-users", method = FormMethod.post) {
                                        hiddenInput(name = "target_user") { value = username }
                                        hiddenInput(name = "redirect_to") {
                                            value = "/admin/manage-users?search=$searchQuery"
                                        }

                                        p {
                                            +"Change Role: "
                                            select {
                                                name = "new_role"
                                                option {
                                                    value = "USER"; selected = (record.role == Role.USER); +"User"
                                                }
                                                option {
                                                    value = "ADMIN"; selected = (record.role == Role.ADMIN); +"Admin"
                                                }
                                                option {
                                                    value = "PENDING"; selected =
                                                    (record.role == Role.PENDING); +"Pending"
                                                }
                                            }
                                        }
                                        p {
                                            +"Change Department: "; textInput(name = "new_department") {
                                            value = record.department ?: ""
                                        }
                                        }
                                        p {
                                            b {
                                                style = "color:red;"; +"Confirm with your password (Step-Up): "
                                            }; passwordInput(name = "admin_password")
                                        }
                                        submitInput(classes = "btn") { value = "Save changes" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        get("/admin/pending-users") {
            val session = call.sessions.get<UserSession>()
            if (session?.role != Role.ADMIN) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            val pendingUsers = DataService.getPendingUsers()

            call.respondHtml {
                head { style { +CssAdd.getCss() } }
                body {
                    div("container") {
                        h1 { +"Accounts Pending Activation (Quarantine)" }

                        div {
                            style = "margin-bottom: 20px; padding: 10px; background: #eee;"
                            b { +"Navigation: " }
                            a(href = "/admin/manage-users") { +"[ Search Accounts ]" }
                            +" | "
                            span { b { +"[ Pending Approval (PENDING) ]" } }
                            +" | "
                            a(href = "/") { +"[ Home Page ]" }
                        }

                        if (pendingUsers.isEmpty()) {
                            p {
                                style =
                                    "color: green; font-weight: bold;"; +"No new requests. All accounts have been verified."
                            }
                        } else {
                            p { +"The following accounts registered via OAuth protocol but have not been granted corporate permissions." }

                            pendingUsers.forEach { (username, record) ->
                                div {
                                    style =
                                        "border: 1px solid orange; padding: 15px; margin-bottom: 15px; background: #fffcf5;"
                                    h3 { +"New request: $username" }

                                    form(action = "/admin/manage-users", method = FormMethod.post) {
                                        hiddenInput(name = "target_user") { value = username }
                                        hiddenInput(name = "redirect_to") { value = "/admin/pending-users" }

                                        p {
                                            +"Activate by assigning role: "
                                            select {
                                                name = "new_role"
                                                option { value = "USER"; +"User (Employee)" }
                                                option { value = "ADMIN"; +"Admin (Management)" }
                                                option {
                                                    value = "PENDING"; selected = true; +"Leave as Pending"
                                                }
                                            }
                                        }
                                        p {
                                            +"Assign Department (Required for ABAC): "; textInput(name = "new_department") {
                                            placeholder = "e.g. HR, IT, Sales"
                                        }
                                        }
                                        p {
                                            +"Change system Login (optional): "; textInput(name = "new_username") {
                                            placeholder = "e.g. k.cogo"
                                        } }
                                        p {
                                            b {
                                                style = "color:red;"; +"Activation authorization (Your password): "
                                            }; passwordInput(name = "admin_password")
                                        }
                                        submitInput(classes = "btn") { value = "Approve employee" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        post("/admin/manage-users") {
            val session = call.sessions.get<UserSession>()
            if (session?.role != Role.ADMIN) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }

            val params = call.receiveParameters()
            val targetUser = params["target_user"] ?: ""
            val redirectUri = params["redirect_to"] ?: "/admin/manage-users"
            val newRole = try {
                Role.valueOf(params["new_role"] ?: "USER")
            } catch (e: Exception) {
                Role.USER
            }
            val newDepartment = params["new_department"]
            val newUsername = params["new_username"]
            val adminPassword = params["admin_password"] ?: ""

            // Step-UP auth
            if (!DataService.verifyAdminPassword(session.username, adminPassword)) {
                call.respondText(
                    "Authorization error! Invalid administrator password.",
                    status = HttpStatusCode.Unauthorized
                )
                return@post
            }

            if (DataService.modifyUser(targetUser, newRole, newDepartment, newUsername)) {
                call.respondRedirect(redirectUri)
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }


        // --- ADD DOCUMENT ---
        get("/add-document") {
            val session = call.sessions.get<UserSession>()
            if (session != null && session.role != Role.PENDING) {
                call.respondHtml {
                    head { style { +CssAdd.getCss() } }
                    body {
                        div("container") {
                            h1 { +"Create Document" }
                            p { +"The document will be automatically assigned to your department: "; b { +"${session.department}" } }
                            form(action = "/add-document", method = FormMethod.post) {
                                div("form-group") {
                                    textArea(cols = "40", rows = "5") { name = "content" }
                                }
                                submitInput(classes = "btn") { value = "Save Document" }
                            }
                        }
                    }
                }
            } else {
                call.respondRedirect("/")
            }
        }

        post("/add-document") {
            val session = call.sessions.get<UserSession>()
            if (session != null && session.role != Role.PENDING) {
                val content = call.receiveParameters()["content"] ?: ""
                val success = DataService.addDocument(session, content)
                if (success) {
                    call.respondRedirect("/")
                } else {
                    call.respondText(
                        "Error. Empty field or no department assigned.",
                        status = HttpStatusCode.BadRequest
                    )
                }
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}