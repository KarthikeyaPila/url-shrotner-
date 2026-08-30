# URL Shortener - Code Reading Guide

This repository now has a Spring Boot backend, a React frontend, auth, ownership, admin views, and a preserved historical prototype. Read it in layers: first the project docs, then one user flow at a time.

## 1. Start with the project state

Read these first:

1. `PROGRESS.md`
2. `ARCHITECTURE_DECISIONS.md`
3. `AGENTS.md`

These explain the current scope, what is implemented, and the rules to keep in mind while reading or changing code.

Then run the app:

```bash
./run.sh
```

Try these flows in the browser:

1. Register or log in.
2. Shorten a long URL.
3. Open the short link.
4. Load your saved links.
5. Disable and re-enable a link.
6. Delete a link.
7. Switch to the admin view if your account has `ROLE_ADMIN`.

That gives you the concrete behavior to map back to the code.

## 2. Read the frontend from the entry point outward

Start here:

1. `frontend/react-app/index.html`
2. `frontend/react-app/src/main.jsx`
3. `frontend/react-app/src/App.jsx`
4. `frontend/react-app/src/AdminPanel.jsx`
5. `frontend/react-app/src/styles.css`

`App.jsx` is the main surface for the product. It handles:

- session restore
- CSRF token setup
- login and registration
- shortening URLs
- loading the current user's links
- enabling, disabling, and deleting links
- the anonymous UX
- the admin toggle

`AdminPanel.jsx` is the read-only admin UI. Read it after `App.jsx` so the app-level state makes sense first.

While reading `App.jsx`, trace these request groups:

```text
Session restore
  → GET /api/auth/csrf
  → GET /api/auth/me

Auth
  → POST /api/auth/register
  → POST /api/auth/login
  → POST /api/auth/logout

Link management
  → POST /api/urls
  → GET /api/urls/mine
  → PATCH /api/urls/{code}/status
  → DELETE /api/urls/{code}

Redirect handling
  → opening the short link in the browser
```

`styles.css` is useful for layout and state cues, but it is not the core application logic.

## 3. Read the backend entry and security setup first

Before the controllers, read:

1. `backend/spring/src/main/java/com/urlshortener/UrlShortenerApplication.java`
2. `backend/spring/src/main/java/com/urlshortener/config/SecurityConfig.java`
3. `backend/spring/src/main/java/com/urlshortener/config/WebConfig.java`

These files explain:

- how Spring Boot starts
- which endpoints are public
- which endpoints require authentication
- how `ROLE_ADMIN` is enforced
- how CSRF is configured
- why the React app can talk to the API from `localhost:5173`

`SecurityConfig.java` is especially important because it defines the real access rules. Read it before assuming any controller is public or private.

## 4. Follow the auth flow

Read these next:

1. `backend/spring/src/main/java/com/urlshortener/auth/AuthController.java`
2. `backend/spring/src/main/java/com/urlshortener/auth/AuthService.java`
3. `backend/spring/src/main/java/com/urlshortener/auth/User.java`
4. `backend/spring/src/main/java/com/urlshortener/auth/UserRepository.java`
5. `backend/spring/src/main/java/com/urlshortener/auth/RegisterRequest.java`
6. `backend/spring/src/main/java/com/urlshortener/auth/LoginRequest.java`
7. `backend/spring/src/main/java/com/urlshortener/auth/UserResponse.java`
8. `backend/spring/src/main/java/com/urlshortener/auth/InvalidCredentialsException.java`
9. `backend/spring/src/main/java/com/urlshortener/auth/EmailAlreadyExistsException.java`

The auth flow is:

```text
React login/register form
        ↓
AuthController
        ↓
AuthService
        ↓
UserRepository
        ↓
BCrypt password check or password hash creation
        ↓
Session cookie and safe user response
```

Important rules to notice:

- emails are normalized
- passwords are hashed with BCrypt
- plaintext passwords are never returned
- the frontend restores the session with `/api/auth/me`
- the browser sends the session cookie and CSRF token with later requests

## 5. Follow the URL shortening flow

Read these files in this order:

1. `backend/spring/src/main/java/com/urlshortener/url/ShortUrlController.java`
2. `backend/spring/src/main/java/com/urlshortener/url/CreateShortUrlRequest.java`
3. `backend/spring/src/main/java/com/urlshortener/url/LinkStatusRequest.java`
4. `backend/spring/src/main/java/com/urlshortener/url/ShortUrlService.java`
5. `backend/spring/src/main/java/com/urlshortener/url/ShortUrl.java`
6. `backend/spring/src/main/java/com/urlshortener/url/ShortUrlRepository.java`
7. `backend/spring/src/main/java/com/urlshortener/url/ShortUrlResponse.java`
8. `backend/spring/src/main/java/com/urlshortener/url/UrlExceptions.java`
9. `backend/spring/src/main/java/com/urlshortener/url/ApiExceptionHandler.java`

The core create flow is:

```text
POST /api/urls
        ↓
ShortUrlController.create()
        ↓
CreateShortUrlRequest validation
        ↓
ShortUrlService.create()
        ↓
Validate the long URL
        ↓
Generate a random code or validate a custom alias
        ↓
Save the ShortUrl entity
        ↓
Return ShortUrlResponse
```

The redirect flow is:

```text
GET /{code}
        ↓
ShortUrlController.redirect()
        ↓
ShortUrlService.resolve()
        ↓
ShortUrlRepository.findByCodeAndActiveTrue()
        ↓
RedirectView sends the browser to the destination URL
```

The ownership and history flow is:

```text
GET /api/urls/mine
PATCH /api/urls/{code}/status
DELETE /api/urls/{code}
```

These operations are server-scoped to the authenticated session user. Do not assume a client-supplied user id is trusted.

When reading `ShortUrlService.java`, focus on:

- URL validation
- generated code creation
- custom alias rules
- alias reuse and conflict behavior
- restore behavior for deleted links owned by the same user
- active versus deleted state

## 6. Read the admin path last

Read these files after the main URL flow:

1. `backend/spring/src/main/java/com/urlshortener/admin/AdminController.java`
2. `backend/spring/src/main/java/com/urlshortener/admin/AdminService.java`
3. `backend/spring/src/main/java/com/urlshortener/admin/AdminSummaryResponse.java`
4. `backend/spring/src/main/java/com/urlshortener/admin/AdminUserResponse.java`
5. `backend/spring/src/main/java/com/urlshortener/admin/AdminLinkResponse.java`

The admin path is read-only in the current UI. It summarizes users and links and is protected by `ROLE_ADMIN`.

## 7. Read persistence and configuration as supporting context

These files help you understand how the app is wired to PostgreSQL:

1. `backend/spring/src/main/resources/application.properties`
2. `backend/spring/pom.xml`
3. `run.sh`

Learn these concepts:

- `@Entity` maps a Java class to a database table
- `@Id` marks the primary key
- `JpaRepository` provides the database operations
- `run.sh` sets up the local database, backend, and frontend together

Read `run.sh` after the app flow makes sense. It becomes easier to understand once you already know why the backend needs PostgreSQL, Maven, and the frontend dev server.

## 8. Read the tests after the code

Start with:

1. `backend/spring/src/test/java/com/urlshortener/auth/AuthServiceTest.java`

Then look for additional tests in `backend/spring/src/test/java/com/urlshortener/` as they are added.

The tests are useful because they show the expected behavior in small, focused examples. Use them to confirm:

- email normalization
- password hashing
- duplicate-email rejection
- future behavior changes in auth, URL handling, or admin logic

## 9. Explore the historical files last

Only after the current implementation makes sense, read:

- `backend/Main.java`
- `frontend/old/index.html`
- `frontend/old/urlShortner.js`
- `frontend/old/urlShortner.css`

These are preserved experiments. They are useful for learning how the project evolved, but they are not the current application.

## Best reading strategy

Trace one real scenario end to end:

```text
App.jsx
  → POST /api/auth/login
  → SecurityConfig
  → AuthController
  → AuthService
  → UserRepository
  → session restored in App.jsx
```

Then trace a link:

```text
App.jsx
  → POST /api/urls
  → ShortUrlController
  → ShortUrlService
  → ShortUrlRepository
  → ShortUrl entity
  → ShortUrlResponse
  → App.jsx renders the result
```

Finally trace a redirect:

```text
browser requests /some-code
  → ShortUrlController
  → ShortUrlService.resolve()
  → RedirectView
```

If you can explain those three flows without looking at the code, you understand the core architecture of this project.
