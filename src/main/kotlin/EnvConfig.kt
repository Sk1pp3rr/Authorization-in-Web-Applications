package com.example

import java.io.File

object EnvConfig {
    private val envMap: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        val envFile = File(".env")
        if (envFile.exists()) {
            envFile.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    val eqIndex = line.indexOf('=')
                    if (eqIndex != -1) {
                        val key = line.substring(0, eqIndex).trim()
                        val value = line.substring(eqIndex + 1).trim()
                            .removeSurrounding("\"")
                            .removeSurrounding("'")
                        map[key] = value
                    }
                }
            }
        }
        map
    }

    fun get(key: String, default: String = ""): String {
        return System.getenv(key) ?: envMap[key] ?: default
    }

    val googleClientId: String get() = get("GOOGLE_CLIENT_ID")
    val googleClientSecret: String get() = get("GOOGLE_CLIENT_SECRET")
    val googleCallbackUrl: String get() = get("GOOGLE_CALLBACK_URL", "http://localhost:8080/callback")
}
