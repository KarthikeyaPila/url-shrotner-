# URL Shortener — Code Review Notes

This document is a practical companion to `CODE_READING_GUIDE.md`. It focuses on the patterns worth remembering, the boundaries between parts of the application, and improvements that can be made safely after understanding the current code.

## How to review this repository

Read one layer at a time and always ask:

1. What responsibility belongs here?
2. What data enters this layer?
3. What does this layer return or change?
4. What errors can it produce?
5. Which layer should own the next decision?

The current implementation has this shape:

```text
React component
    ↓ HTTP request
Spring controller
    ↓ validated DTO
Service / business rules
    ↓ repository methods
JPA entity and PostgreSQL
```

The frontend should not contain business rules that the backend must enforce. The controller should not contain database logic. The entity should protect its own state transitions where possible.

## 1. The main architectural pattern

The backend follows a Controller → Service → Repository pattern.

### Controller

Examples:

- `url/ShortUrlController.java`
- `auth/AuthController.java`
- `admin/AdminController.java`

Controllers are responsible for HTTP concerns:

- Route and HTTP method
- Request body binding
- Validation annotations
- Authentication context access
- HTTP status codes
- Calling the correct service method

They should remain thin. A controller should answer “which operation is requested?” rather than “how does the operation work?”

### Service

Examples:

- `url/ShortUrlService.java`
- `auth/AuthService.java`
- `admin/AdminService.java`

Services contain business decisions:

- Whether a URL is valid
- How aliases behave
- Whether a user owns a link
- Whether a deleted link may be restored
- How passwords are hashed
- How admin data is assembled

This makes the rules reusable from controllers, tests, or future interfaces.

### Repository

Examples:

- `url/ShortUrlRepository.java`
- `auth/UserRepository.java`

Repositories describe data access. Spring Data derives many queries from method names, such as:

```java
findByCodeAndOwner(code, owner)
findAllByOwnerOrderByCreatedAtDesc(owner)
findByCodeAndActiveTrue(code)
```

The repository should answer “how do we retrieve this data?” The service should decide “are we allowed to use it?”

## 2. DTOs protect the API boundary

Request and response records are used instead of exposing entities directly:

- `CreateShortUrlRequest`
- `LoginRequest`
- `RegisterRequest`
- `LinkStatusRequest`
- `ShortUrlResponse`
- `UserResponse`

This is important because a JPA entity is a database model, not automatically a safe public API model.

For example, `UserResponse` deliberately exposes safe fields but never exposes:

- Plaintext passwords
- BCrypt hashes
- Internal persistence details

Remember this rule:

> Database shape and API shape may overlap, but they should not be coupled accidentally.

## 3. Business logic worth studying in `ShortUrlService`

`ShortUrlService` is currently the most valuable backend file to understand.

### URL validation

The service parses the value as a `URI` and permits only HTTP and HTTPS URLs with a host. This prevents values such as arbitrary text or unsupported schemes from becoming redirects.

Validation belongs on the backend even though the React form uses `type="url"`. Browser validation improves user experience; backend validation is the actual security and correctness boundary.

### Generated-code creation

Generated codes use `SecureRandom` and a Base62-style alphabet. The loop checks whether a generated code already exists before saving it.

The pattern is easy to understand:

```text
generate a candidate
    ↓
check the database
    ↓
repeat if needed
    ↓
save the link
```

This is acceptable for a small local MVP, but it is not fully race-safe under concurrent requests. Two requests could both check the same unused code before either one saves it. The database unique constraint is the final protection; a future version should catch a collision and retry the save.

### Alias rules

Aliases use a simple allow-list:

```text
[A-Za-z0-9_-]+
```

The service handles several cases:

- New alias → create a link
- Existing active alias with the same URL → return the existing link
- Existing alias with a different URL → conflict
- Deleted alias owned by the same user → restore it with the new destination
- Deleted alias owned by someone else → conflict

This is a good example of why the service layer exists: these decisions would become difficult to follow if distributed across React, controllers, and repositories.

## 4. Entity state and lifecycle methods

`ShortUrl` has three effective states:

```text
active=true,  deletedAt=null → ACTIVE
active=false, deletedAt=null → DISABLED
active=false, deletedAt!=null → DELETED
```

The entity exposes intent-based methods:

```java
setActive(boolean active)
delete()
restore(String longUrl)
```

This is better than allowing callers to change `active`, `deletedAt`, and `disabledAt` independently. Related fields are updated together, reducing invalid combinations.

The deletion is a soft deletion: the row remains in the database so the owner can see its history, while public resolution ignores it.

## 5. Ownership is an authorization boundary

The frontend displays the current user, but it is never trusted to decide ownership.

The backend obtains the user from the authenticated session:

```java
service.delete(code, requiredUser(authentication));
```

The repository lookup includes both the link code and owner:

```java
findByCodeAndOwner(code, owner)
```

That prevents a user from managing another user’s link simply by changing a code in the browser.

The important security rule is:

> The client may request an action, but the server decides whether the current session may perform it.

## 6. Authentication and security patterns

The authentication flow is deliberately session-based:

```text
Login request
    ↓
AuthenticationManager
    ↓
UserDetailsService loads the normalized email
    ↓
BCrypt verifies the password
    ↓
Spring stores authentication in the session
    ↓
Browser sends the session cookie later
```

Important details in `SecurityConfig`:

- BCrypt hashes passwords.
- Session policy is `IF_REQUIRED`.
- The session cookie is used instead of a JWT in `localStorage`.
- CSRF protection is enabled because authentication uses cookies.
- CORS allows only the local frontend origins.
- `/api/admin/**` requires the `ADMIN` role.
- `/api/urls` and public redirects remain available anonymously.

The CSRF token is not the same thing as the session cookie:

- Session cookie: proves the browser has an authenticated session.
- CSRF token: helps prove that a state-changing request came from the application’s legitimate frontend.

## 7. Error handling pattern

`ApiExceptionHandler` is a global `@RestControllerAdvice`. Instead of every controller repeating error responses, domain exceptions are translated in one place:

```text
Service throws InvalidUrlException
    ↓
ApiExceptionHandler catches it
    ↓
JSON response: { "error": "..." }
```

This gives the frontend a predictable response shape and keeps controllers cleaner.

When adding a new domain error, check three things:

1. Create or reuse an appropriate exception.
2. Map it to the correct HTTP status.
3. Make sure the frontend displays the returned message usefully.

## 8. Frontend patterns worth remembering

`App.jsx` currently acts as the application shell. Important patterns include:

### Centralized API helper

`apiRequest()` centralizes:

- API base URL
- JSON headers
- CSRF header
- Credentials/cookies
- Response parsing
- Error conversion

Without this helper, every request would need to repeat the same security and parsing behavior.

### State represents UI facts

Examples:

```text
authReady       → initial session check completed
loading         → shorten request is running
linksLoading    → history request is running
showAuthOverlay → login/register modal is visible
linkActionTarget→ manage modal’s selected link
```

Good React state describes facts. Derived values, such as whether a link should show an Open button, are calculated from the link data rather than stored as extra state.

### Conditional rendering is a permission hint, not security

The frontend hides admin controls from non-admin users and hides actions from deleted links. This improves UX, but it is not the authorization mechanism. The backend still protects every sensitive endpoint.

### New-tab safety

Active links use:

```html
target="_blank" rel="noopener noreferrer"
```

`noopener` prevents the opened page from controlling the original tab through `window.opener`.

## 9. Admin console pattern

The admin console is read-only and separated from normal user history.

```text
Admin React view
    ↓
/api/admin/summary
/api/admin/users
/api/admin/urls
    ↓
ROLE_ADMIN authorization
    ↓
AdminService
    ↓
Database queries
```

The browser never connects directly to PostgreSQL. This is essential: database access and role enforcement remain backend responsibilities.

The role check in React only controls what is displayed. A user could manually call an admin endpoint, so Spring Security must remain the real boundary.

## 10. Optimization opportunities

Optimize in this order: correctness, security, database behavior, then convenience and performance.

### High priority after the review

1. Add service tests for alias behavior, ownership, status transitions, and deleted-link resolution.
2. Add controller tests with MockMvc for HTTP statuses, validation, authentication, and CSRF.
3. Make generated-code collision handling retry safely when the database unique constraint is hit.
4. Handle concurrent registration using the database unique constraint as well as the initial existence check.
5. Move `API_BASE` to a Vite environment variable so deployment does not require source changes.

### Medium priority

1. Add pagination to `GET /api/urls/mine` and admin list endpoints.
2. Add search/filtering once link history becomes larger.
3. Replace admin summary’s full link load with database count queries.
4. Review the admin user link-count query for an N+1 pattern; `countByOwner` currently runs once per returned user.
5. Add explicit sorting to admin user and link queries so pagination remains stable.
6. Add database indexes for frequent lookups such as `code` and `user_id` if PostgreSQL query volume grows.

### Later product and production work

1. Add expiration dates.
2. Add click counts and last-clicked timestamps.
3. Add rate limiting and abuse protection.
4. Add password reset and email verification.
5. Add Google OAuth through a provider identity table.
6. Add HTTPS, secure production cookies, environment-based configuration, Docker, and deployment automation.

## 11. Things to watch for during future changes

- Do not return JPA entities directly from controllers.
- Do not trust user IDs or roles supplied by the frontend.
- Do not store passwords or session tokens in browser `localStorage`.
- Do not rely only on frontend validation.
- Do not remove the database unique constraints because the service checks first.
- Do not put database queries in React components.
- Do not make a link “deleted” by only hiding it in the UI.
- Do not add a new state field when the value can be derived from existing state.
- Do not optimize before measuring or before automated tests protect the behavior.

## 12. Suggested review order

For a focused code review, use this sequence:

1. `App.jsx` — frontend state and API calls
2. `ShortUrlController.java` — HTTP boundary
3. `CreateShortUrlRequest.java` and `ShortUrlResponse.java` — API shapes
4. `ShortUrlService.java` — core business rules
5. `ShortUrl.java` — persistence and lifecycle state
6. `ShortUrlRepository.java` — database queries
7. `AuthController.java` and `AuthService.java` — authentication flow
8. `SecurityConfig.java` and `WebConfig.java` — security and browser boundaries
9. `ApiExceptionHandler.java` — error translation
10. `AdminService.java`, `AdminController.java`, and `AdminPanel.jsx` — protected reporting
11. Tests and `ARCHITECTURE_DECISIONS.md` — verification and reasoning

The goal is not to memorize every line. The goal is to recognize where a responsibility belongs and to be able to trace one request through every layer.
