package com.example

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.html.*

fun Application.configureHttp() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("MyCustomHeader")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    routing {
        swaggerUI(path = "swagger", swaggerFile = "documentation.yaml")

        get("/dashboard") {
            val session = call.sessions.get<UserSession>() // Using session plugin
            if (session == null) {
                call.respondRedirect("/login")
                return@get
            }

            val visibleDocs = DataService.getVisibleDocuments(session)

            call.respondHtml {
                head {style {+CssAdd.getCss()}}
                body {
                    div("container") {
                        h1 { +"Hello, ${session.name}!" }
                        p { +"Your role: ${session.role}" }

                        h2 { +"Method 1: RBAC" }
                        if (session.role == Role.ADMIN) {
                            p { b { +"Acces granted: Secret settings only for administrators." } }
                        } else {
                            p { +"[Access denied]" }
                        }

                        h2 { +"Method 2: ABAC" }
                        ul {
                            visibleDocs.forEach { doc ->
                                li { +"Doc ID ${doc.id}: ${doc.content}" }
                            }
                        }
                        a(href = "/logout") { +"Log out" }
                    }
                }
            }
        }
    }
}
