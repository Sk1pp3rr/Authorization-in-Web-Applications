package com.example

import org.mindrot.jbcrypt.BCrypt

object DataService {
    private val userDB = mutableMapOf(
        "admin" to Pair(BCrypt.hashpw("p4ss123", BCrypt.gensalt()), Role.ADMIN),
        "chillGuy" to Pair(BCrypt.hashpw("chill123", BCrypt.gensalt()), Role.USER)
    )

    private val documents = mutableListOf(
        Document(1, "Secret crazy Administrator data or whatever", "admin"),
        Document(2, "Chill Guy's music sheets", "chillGuy")
    )

    // Abstract for users

    fun validateUser(username: String, password: String): Role? {
        val entry = userDB[username]
        return if (entry != null && BCrypt.checkpw(password, entry.first)) entry.second else null
    }

    fun addUser(adminSession: UserSession?, newUser: String, newPass: String, newRole: Role): Boolean {
        // Additional security
        if (adminSession?.role != Role.ADMIN) return false
        if (userDB.containsKey(newUser)) return false

        userDB[newUser] = Pair(BCrypt.hashpw(newPass, BCrypt.gensalt()), newRole)
        return true
    }

    // Abstract for documents

    fun getVisibleDocuments(session: UserSession?): List<Document> {
        if (session == null) return emptyList()
        // ABAC
        return if (session.role == Role.ADMIN) {
            documents.toList()
        } else {
            documents.filter { it.owner == session.name }
        }
    }

    fun getDocumentIfAllowed(session: UserSession?, id: Int?): Document? {
        if (session == null || id == null) return null
        val doc = documents.find { it.id == id } ?: return null
        return if (session.role == Role.ADMIN || doc.owner == session.name) doc else null
    }

    fun addDocument(session: String, content: String): Boolean {
        if (session == null || content.isEmpty()) return false
        val newId = (documents.maxOfOrNull { it.id } ?: 0) + 1
        documents.add(Document(newId, content, session))
        return true
    }
}