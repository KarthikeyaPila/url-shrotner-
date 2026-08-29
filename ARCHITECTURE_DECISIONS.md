# URL Shortener — Architecture and Decision Record

This document records the important product, architecture, and security decisions made during the project. It is intended to explain not only what the project does, but why it is built this way.

## Project purpose

This is a learning and portfolio project. It is designed to build practical understanding of Java, Spring Boot, HTTP, APIs, databases, React, authentication, and deployment.

The application is a URL shortener: a user submits a long URL, the backend stores it, and the application returns a shorter code that redirects to the original URL.

## Current technology decisions

- Backend: Java 21 with Spring Boot
- Frontend: React with Vite
- Database: local PostgreSQL during development
- Persistence: Spring Data JPA and Hibernate
- Development startup: `run.sh` starts PostgreSQL setup, the backend, and the frontend
- Docker: postponed until a later milestone
- Historical experiments: preserved rather than deleted for learning purposes

## Current repository structure

```text
frontend/react-app/       Current React frontend
frontend/old/             Earlier hand-written frontend, preserved for reference
backend/spring/           Current Spring Boot backend
backend/Main.java         Earlier Java HttpServer experiment
run.sh                    Local development startup script
PROGRESS.md               Short progress summary
CODE_READING_GUIDE.md     Suggested order for learning the codebase
```

## Current URL-shortening flow

### Creating a short URL

```text
User fills in the React form
        ↓
App.jsx sends POST /api/urls
        ↓
ShortUrlController receives the request
        ↓
CreateShortUrlRequest validates basic request fields
        ↓
ShortUrlService validates the URL
        ↓
Service generates a code or validates the custom alias
        ↓
ShortUrlRepository saves a ShortUrl entity
        ↓
ShortUrlResponse is returned as JSON
        ↓
React displays the short link
```

### Redirecting to the original URL

```text
Browser requests GET /{code}
        ↓
ShortUrlController extracts the code
        ↓
ShortUrlService asks the repository for the code
        ↓
The stored long URL is returned
        ↓
Spring RedirectView redirects the browser
```

### Error flow

```text
Validation or lookup problem
        ↓
A domain exception is thrown
        ↓
ApiExceptionHandler catches it
        ↓
Backend returns JSON with an appropriate HTTP status
        ↓
React displays the error message
```

Current status choices include:

- `400 Bad Request` for invalid input
- `404 Not Found` for an unknown short code
- `409 Conflict` for an already-used alias

## URL-shortening decisions

- Links persist indefinitely in the MVP.
- Generated codes are seven-character random Base62-style strings.
- Custom aliases may contain letters, numbers, `-`, and `_`.
- Alias collisions return `409 Conflict`.
- Repeating the same alias with the same long URL returns the existing link; using that alias for a different URL is rejected.
- Anonymous shortening remains supported while authentication is introduced.
- A future `user_id` on `short_urls` may be nullable so existing anonymous links continue to work.

## Authentication direction

Authentication is being developed in two stages:

1. Manual email/password authentication
2. Google OAuth login as a second provider

The manual authentication stage is implemented. Google OAuth remains planned.

### Implemented manual authentication components

- `User` is a JPA entity stored in the `app_users` table.
- `UserRepository` looks users up by normalized email.
- `AuthService` validates uniqueness, hashes passwords, and creates users.
- `SecurityConfig` configures Spring Security, BCrypt, sessions, CSRF, and endpoint access.
- `AuthController` exposes registration, login, logout, CSRF setup, and current-user endpoints.
- `UserResponse` deliberately contains no password or password hash.
- The React application restores the session when it loads and sends credentials with API requests.

The current authentication API is:

```text
GET  /api/auth/csrf       Public CSRF-token setup
POST /api/auth/register   Public account creation
POST /api/auth/login      Public login; creates a session
POST /api/auth/logout     Ends the current session
GET  /api/auth/me         Returns the logged-in user; requires authentication
```

Manual authentication comes first because it establishes the core concepts and gives us a local user model before integrating an external identity provider.

Google login should eventually create or locate the same local user record rather than creating a separate kind of account.

## Manual authentication flow

The planned endpoints are:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

Registration flow:

```text
React registration form
        ↓
POST /api/auth/register
        ↓
Validate email and password
        ↓
Hash the password
        ↓
Save the user record
        ↓
Return a safe user response
```

Login flow:

```text
React login form
        ↓
POST /api/auth/login
        ↓
Spring Security finds the user
        ↓
Stored password hash is verified
        ↓
Secure session cookie is created
        ↓
Later requests identify the logged-in user
```

`GET /api/auth/me` will allow React to restore authentication state after a page refresh.

## Authentication storage decision

The initial recommendation is cookie-based server sessions rather than storing JWTs in browser `localStorage`.

Reasons:

- HttpOnly cookies cannot be read by ordinary frontend JavaScript.
- Session invalidation is straightforward during logout.
- Spring Security supports this model directly.
- The browser handles sending the session cookie with requests.
- It avoids making token storage and token refresh part of the first authentication milestone.

Because the frontend and backend currently run on different local ports, CORS and credential settings must be configured carefully. Production should use HTTPS and secure cookie settings.

## Planned user and ownership model

The initial user table is expected to contain:

```text
users
├── id
├── email
├── password_hash
├── display_name
├── role
└── created_at
```

The first authenticated version should preserve anonymous use:

```text
Anonymous user:
- Can create a short URL
- The link continues to work
- No personal link history

Logged-in user:
- Can create a short URL
- The link belongs to their account
- Can view their own links
```

This lets authentication enhance the application without breaking the existing MVP.

## Security decisions

- Never store plaintext passwords.
- Use a slow password hashing algorithm such as BCrypt or Argon2.
- Enforce a minimum password length and validate input on the backend.
- Keep email addresses unique.
- Return generic login failures such as `Invalid email or password` rather than revealing whether an email exists.
- Use HttpOnly cookies for the session.
- Use secure cookies and HTTPS in production.
- Configure CSRF protection appropriately for cookie-based authentication.
- Configure CORS to allow only known frontend origins.
- Keep Google client IDs and secrets in environment variables, never in committed source code.
- Identify Google accounts by Google’s stable provider ID, not only by email.
- Add rate limiting and stronger abuse protection in a later security milestone.

## Testing strategy

The project uses several kinds of verification. They answer different questions, so a successful build alone does not prove that the whole feature works.

### 1. Unit tests

Unit tests exercise one class or service in isolation using mocks for its dependencies. They are fast and useful for business rules.

Current example:

`backend/spring/src/test/java/com/urlshortener/auth/AuthServiceTest.java` verifies that:

- Registration normalizes an email address to lowercase.
- The display name is trimmed.
- The plaintext password is not stored.
- The stored BCrypt hash can verify the original password.
- The API response contains safe user information.
- Duplicate emails are rejected.

Run backend unit tests with:

```bash
cd backend/spring
mvn test
```

The test output may contain a Mockito/Java agent warning. It is currently a warning from the test tooling, not a failed test.

### 2. Compilation and production-build tests

These tests verify that the source code can be compiled and that the frontend can be bundled for production.

Backend:

```bash
cd backend/spring
mvn test
```

The Maven command compiles the Java application and test sources before running the tests.

Frontend:

```bash
cd frontend/react-app
npm run build
```

This catches invalid imports, JSX errors, and bundling problems. It does not test browser interactions by itself.

### 3. Shell syntax test

The startup script is checked without starting any services:

```bash
bash -n run.sh
```

This catches malformed Bash syntax, but it does not prove that PostgreSQL, Maven, or npm are available.

### 4. Application startup test

An application startup test launches Spring Boot and checks whether the application context can initialize. This verifies several things together:

- Spring can discover the controllers, services, entities, and repositories.
- Dependencies are present.
- PostgreSQL credentials work.
- Hibernate can connect to the database and update the schema.
- Security configuration is valid.
- The embedded web server can start.

Normally the application is started with:

```bash
./run.sh
```

During development, if port `8080` is already in use, the backend can be checked on another port:

```bash
cd backend/spring
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

The existing process on port `8080` should not be interrupted just to perform this check.

### 5. Smoke test

A smoke test is a small end-to-end check of the most important path through a running application. It does not test every edge case. Its purpose is to answer:

> “Is the application alive, and can the main feature complete a realistic request flow?”

For authentication, the smoke-test flow is:

```text
Start the backend
        ↓
Request a CSRF token
        ↓
Register a new user
        ↓
Log in with that user
        ↓
Request /api/auth/me
        ↓
Log out
        ↓
Verify the session is no longer authenticated
```

The flow was tested against the backend on port `8081` with `curl`. A cookie jar was used so that the session cookie and CSRF cookie behaved like they do in a browser.

The important expected status codes are:

```text
GET  /api/auth/csrf     200
POST /api/auth/register 201
POST /api/auth/login    200
GET  /api/auth/me       200 while logged in
POST /api/auth/logout   204
GET  /api/auth/me       401 after logout
```

A smoke test is different from a unit test. The unit test checks password hashing in one Java service. The smoke test checks the complete route through HTTP, Spring Security, sessions, CSRF, PostgreSQL, and the controller.

The smoke test created a temporary local development account. It was not a production account and should not be used as real credentials.

### 6. Manual browser test

After automated checks pass, use the actual React interface:

1. Start the project with `./run.sh`.
2. Register a new account.
3. Refresh the page and confirm the greeting remains.
4. Log out and confirm the login form returns.
5. Try an incorrect password and confirm a generic error appears.
6. Shorten a URL while logged out.
7. Shorten a URL while logged in.

This catches presentation and browser-specific issues that command-line tests cannot see.

### What has been verified so far

- Backend Maven tests pass.
- Frontend `npm run build` passes.
- `run.sh` passes `bash -n` syntax validation.
- Spring Boot connected to local PostgreSQL and initialized the `app_users` table.
- The live authentication smoke test completed registration, login, session lookup, and logout.
- The frontend sends cookies and CSRF headers for API requests.
- Authenticated URL creation returns `201` after login and persists the short URL normally.

### Recent regression and fix

Authenticated shortening once returned `500 Internal Server Error` even though login worked. The server log showed:

```text
UnsupportedOperationException: Unimplemented method 'longUrl'
```

The cause was a generated TODO method inside the `CreateShortUrlRequest` record. It overrode the record-generated `longUrl()` accessor and always threw an exception. Removing that method restored the normal record accessor. The authenticated smoke test was rerun afterward and URL creation returned `201`.

### What is not tested yet

- Automated controller/HTTP tests using MockMvc.
- Automated browser tests.
- Password-reset and email-verification flows.
- Concurrent registration and duplicate-email race conditions.
- Production HTTPS, secure-cookie, and deployment behavior.
- Google OAuth login.

## Google login and account linking

Google login will be added after manual authentication works.

Planned flow:

```text
User selects “Continue with Google”
        ↓
Spring redirects the user to Google
        ↓
Google authenticates and gives Spring the result
        ↓
Spring finds or creates a local user
        ↓
Spring creates the normal application session
        ↓
The user is logged into the application
```

A future `user_identities` table can represent external providers:

```text
user_identities
├── id
├── user_id
├── provider
├── provider_id
└── created_at
```

Manual and Google credentials should point to one local user. Automatically merging a Google identity into an existing local account based only on matching email is risky, so explicit account linking is preferred.

## Planned authenticated features

After the basic login system is complete, likely additions are:

- `GET /api/urls/mine` for link history
- `DELETE /api/urls/{code}` for link management
- A React login and registration interface
- A user identity indicator and logout control
- Ownership-aware access checks
- Google login
- Expiration dates
- Click analytics
- QR-code generation
- Docker and deployment configuration

## Decision history

### Manual login before Google login

Manual login will be implemented first so the local user model, password handling, session behavior, and protected API flow are understood before adding OAuth complexity.

### Cookie sessions before JWTs

Cookie-based sessions are preferred for this browser-based application because they reduce frontend token-handling responsibility and work naturally with Spring Security.

### Preserve anonymous shortening

Authentication should add accounts and ownership without removing the existing ability to shorten a URL without signing in.

### Preserve earlier experiments

The old frontend and original Java server remain in the repository as learning references. New implementation work belongs in `frontend/react-app` and `backend/spring`.

## How to use this document

When a new architectural or security decision is made:

1. Add it to the relevant section.
2. Explain the reason, not only the outcome.
3. Update the flow diagrams if request or data movement changes.
4. Add a dated entry to the decision history when the decision is significant.
