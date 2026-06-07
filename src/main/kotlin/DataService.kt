package com.example

import org.mindrot.jbcrypt.BCrypt

object DataService {
    private val userDB = mutableMapOf(
        "admin" to UserRecord(BCrypt.hashpw("p4ss123", BCrypt.gensalt()), Role.ADMIN, "IT"),
        "chillGuy" to UserRecord(BCrypt.hashpw("chill123", BCrypt.gensalt()), Role.USER, "HR")
    )

    private val documents = mutableListOf(
        Document(1, "Tajne plany wdrożenia infrastruktury Ktor", "IT"),
        Document(2, "Procedury rekrutacyjne i wnioski urlopowe", "HR")
    )

    // --- USERS ---

    fun validateUser(username: String, password: String): UserSession? {
        val user = userDB[username]
        if (user != null && BCrypt.checkpw(password, user.passwordHash)) {
            return UserSession(username, username, user.role, user.department)
        }
        return null
    }

    // Step-up Authentication: Sprawdzamy hasło admina przed wykonaniem groźnej akcji
    fun verifyAdminPassword(adminUsername: String, passwordInput: String): Boolean {
        val user = userDB[adminUsername] ?: return false
        if (user.role != Role.ADMIN) return false
        return BCrypt.checkpw(passwordInput, user.passwordHash)
    }

    fun getAllUsers(): Map<String, UserRecord> {
        return userDB.toMap()
    }

    fun modifyUser(targetUser: String, newRole: Role, newDepartment: String?, newUsername: String? = null): Boolean {
        val user = userDB[targetUser] ?: return false
        user.role = newRole
        user.department = newDepartment.takeIf { it?.isNotBlank() == true }

        if (!newUsername.isNullOrBlank() && newUsername != targetUser && !userDB.containsKey(newUsername)) {
            userDB[newUsername] = user
            userDB.remove(targetUser)
        }

        return true
    }

    // --- OAUTH 2.0 ---

    fun authenticateGoogleUser(googleId: String, realName: String): UserSession {
        val existingUser = userDB.entries.find { it.value.googleId == googleId }

        val usernameToUse: String
        val roleToUse: Role
        val deptToUse: String?

        if (existingUser == null) {
            // Registration temporary googleID as username
            usernameToUse = "google_${googleId.take(6)}..."
            val dummyHash = BCrypt.hashpw("oauth_dummy_${System.currentTimeMillis()}", BCrypt.gensalt())

            // googleID for the record
            userDB[usernameToUse] = UserRecord(dummyHash, Role.PENDING, null, googleId)

            roleToUse = Role.PENDING
            deptToUse = null
        } else {
            //log changed by ADMIN
            usernameToUse = existingUser.key
            roleToUse = existingUser.value.role
            deptToUse = existingUser.value.department
        }

        return UserSession(usernameToUse, realName, roleToUse, deptToUse)
    }

    // --- DOCS (ABAC) ---

    fun getVisibleDocuments(session: UserSession?): List<Document> {
        if (session == null || session.role == Role.PENDING) return emptyList()
        return documents.filter { it.department == session.department }
    }

    fun getDocumentIfAllowed(session: UserSession?, id: Int?): Document? {
        if (session == null || id == null || session.role == Role.PENDING) return null
        val doc = documents.find { it.id == id } ?: return null
        // ABAC
        return if ( doc.department == session.department) doc else null
    }

    fun addDocument(session: UserSession, content: String): Boolean {
        if (content.isEmpty() || session.department == null) return false
        val newId = (documents.maxOfOrNull { it.id } ?: 0) + 1
        // Document has the department attribute from its owner
        documents.add(Document(newId, content, session.department))
        return true
    }

    // --- SEARCH AND FILTER ---

    // Only user with exact username
    fun searchUsers(query: String): Map<String, UserRecord> {
        if (query.isBlank()) return emptyMap()
        return userDB.filter { it.key.contains(query, ignoreCase = true) }
    }

    // User with PENDING role
    fun getPendingUsers(): Map<String, UserRecord> {
        return userDB.filter { it.value.role == Role.PENDING }
    }
}