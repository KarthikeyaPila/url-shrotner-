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

Authentication will be developed in two stages:

1. Manual email/password authentication
2. Google OAuth login as a second provider

Manual authentication comes first because it establishes the core concepts and gives us a local user model before integrating an external identity provider.

Google login should eventually create or locate the same local user record rather than creating a separate kind of account.

## Planned manual authentication flow

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
