package com.example

import org.mindrot.jbcrypt.BCrypt

data class UserRecord(val passHash: String, val role: Role, val department: String)

object DataService {
    private val userDB = mutableMapOf(
        "admin" to UserRecord(BCrypt.hashpw("p4ss123", BCrypt.gensalt()), Role.ADMIN, "IT_SECURITY"),
        "chillGuy" to UserRecord(BCrypt.hashpw("chill123", BCrypt.gensalt()), Role.USER, "HR")
    )

    private val documents = mutableListOf(
        Document(1, "Secret crazy Administrator data or whatever", "IT_SECURITY"),
        Document(2, "Chill Guy's music sheets", "HR")
    )

    // User logic

    fun validateUser(username: String, password: String): UserSession? {
        val record = userDB[username]
        return if (record != null && BCrypt.checkpw(password, record.passHash)) {
            UserSession(username, record.role, record.department)
        } else null
    }

    fun addUser(adminSession: UserSession?, newUser: String, newPass: String, newRole: Role, newDept: String): Boolean {
        if (adminSession?.role != Role.ADMIN) return false
        if (userDB.containsKey(newUser)) return false

        userDB[newUser] = UserRecord(BCrypt.hashpw(newPass, BCrypt.gensalt()), newRole, newDept)
        return true
    }

    // DOC logic (STRICT ABAC)

    fun getVisibleDocuments(session: UserSession?): List<Document> {
        if (session == null) return emptyList()
        // WSZYSCY (nawet Admin) widzą tylko dokumenty ze swojego departamentu
        return documents.filter { it.department == session.department }
    }

    fun getDocumentIfAllowed(session: UserSession?, id: Int?): Document? {
        if (session == null || id == null) return null
        val doc = documents.find { it.id == id } ?: return null

        // Access is verified only through the department attribute
        return if (doc.department == session.department) doc else null
    }

    fun addDocument(department: String, content: String): Boolean {
        if (department.isEmpty() || content.isEmpty()) return false
        val newId = (documents.maxOfOrNull { it.id } ?: 0) + 1
        documents.add(Document(newId, content, department))
        return true
    }
}