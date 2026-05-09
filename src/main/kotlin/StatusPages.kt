package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.html.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        // (403 Forbidden) RBAC/ABAC
        status(HttpStatusCode.Forbidden) { call, _ ->
            call.respondHtml {
                head {style { +CssAdd.getCss() }}
                body {
                    div("container") {
                        h1 { +"Forbidden access (403)" }
                        p { +"Your role or document permissions do not allow access." }
                        a(href = "/") { +"Return to home page" }
                    }
                }
            }
        }

        exception<Throwable> { call, cause ->
            call.respondText(text = "Server Error: ${cause.localizedMessage}" , status = HttpStatusCode.InternalServerError)
        }
    }
}
