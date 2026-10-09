# Authorization-in-Web-Application

This project was created using the [Ktor Project Generator](https://start.ktor.io).

### Project Overview

The objective of this project is to implement and demonstrate practical web authorization mechanisms using modern development standards. The application serves as a demonstration of access control models, specifically comparing Role-Based Access Control (RBAC) and Attribute-Based Access Control (ABAC)

### Technology Stack
The application is built using a type-safe and modular stack to ensure high security standards:
- Programming Language: Kotlin 2.x
- Web Framework: Ktor 3.4.0 (Server)
- Build System: Gradle (Kotlin DSL)
- Serialization: kotlinx.serialization (JSON)
- Security: jBCrypt for secure password hashing and Ktor Auth for identity management.



### Features
Here's a list of features included in this project:

| Name | Description |
|------|-------------|
| [Swagger](https://start.ktor.io/p/io.ktor/server-swagger) | Serves Swagger UI for your project |
| [Call Logging](https://start.ktor.io/p/io.ktor/server-call-logging) | Logs client requests |
| [Status Pages](https://start.ktor.io/p/io.ktor/server-status-pages) | Provides exception handling for routes |
| [CORS](https://start.ktor.io/p/io.ktor/server-cors) | Enables Cross-Origin Resource Sharing (CORS) |
| [kotlinx.serialization](https://start.ktor.io/p/io.ktor/server-kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library |
| [Content Negotiation](https://start.ktor.io/p/io.ktor/server-content-negotiation) | Provides automatic content conversion according to Content-Type and Accept headers |
| [Sessions](https://start.ktor.io/p/io.ktor/server-sessions) | Adds support for persistent sessions through cookies or headers |
| [Authentication](https://start.ktor.io/p/io.ktor/server-auth) | Provides extension point for handling the Authorization header |
| [Authentication JWT](https://start.ktor.io/p/io.ktor/server-auth-jwt) | Handles JSON Web Token (JWT) bearer authentication scheme |

### Implemented Control Methods
In accordance with the project requirements, two distinct methods of access control have been implemented:
- Role-Based Access Control (RBAC): Restricts access to administrative functions based on predefined user roles (e.g., Admin vs. User).
- Attribute-Based Access Control (ABAC): Implements fine-grained permissions based on resource ownership, ensuring users can only interact with their own data.


### Deployment Instructions
To build and run the application for evaluation:
1. Configure environment variables:
   Copy `.env.example` to `.env` and fill in your Google OAuth credentials if needed:
   ```bash
   cp .env.example .env
   ```
2. Build the project using the Gradle wrapper: `./gradlew build`
3. Start the server: `./gradlew run`
4. The server will be accessible at http://localhost:8080.
5. Interactive API documentation and testing are available via Swagger UI at http://localhost:8080/swagger.

