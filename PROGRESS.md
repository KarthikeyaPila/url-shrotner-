# URL Shortener — Progress Log

Last updated: 2026-08-30

## Project goal

This is a personal learning and portfolio project. The aim is to build a small URL shortener while learning how Java, HTTP, databases, APIs, frontend applications, and deployment fit together.

## Decisions made

- Backend: Java 21 + Spring Boot
- Database: local PostgreSQL
- New frontend: React + Vite
- Original frontend: preserve the hand-written HTML/CSS/JavaScript as a historical learning version
- MVP links persist indefinitely
- Generated short codes: random seven-character Base62-style strings
- Custom aliases: letters, numbers, `-`, and `_`
- Duplicate aliases: reject with `409 Conflict`
- Reusing an alias with the same long URL returns the existing link; reusing it for a different URL still returns `409 Conflict`
- Learning style: implement alongside the learner while explaining important decisions
- Docker: postpone until a later milestone
- Authentication: build manual email/password login first, then add Google as a second login provider
- Authentication storage: begin with cookie-based server sessions managed by Spring Security
- Anonymous shortening: preserve it while adding optional user ownership to links

## Current structure

- `frontend/` — original hand-written frontend; preserved intentionally
- `frontend/react-app/` — new React + Vite frontend
- `backend/Main.java` — original Java `HttpServer` experiment; preserved intentionally
- `backend/spring/` — current Spring Boot backend
- `run.sh` — starts the database setup, backend, and frontend together

## Implemented MVP

### API

- `POST /api/urls`
  - Request body: `{ "longUrl": "https://example.com", "alias": "optional" }`
  - Validates HTTP and HTTPS URLs
  - Generates a Base62-style code when no alias is supplied
- Returns `409 Conflict` for an existing alias
- Repeated identical alias + URL requests return the existing short link
- `GET /{code}`
  - Redirects to the stored long URL
  - Returns `404` for an unknown code

### Backend files

- `UrlShortenerApplication.java` — Spring Boot entry point
- `ShortUrl.java` — JPA entity
- `ShortUrlRepository.java` — database access
- `ShortUrlService.java` — validation, code generation, and URL resolution
- `ShortUrlController.java` — API routes
- `ApiExceptionHandler.java` — JSON error responses
- `WebConfig.java` — local frontend CORS configuration
- `application.properties` — local PostgreSQL configuration

### Frontend

The React frontend currently supports:

- Long URL input
- Optional custom alias input
- Loading state
- API error display
- Short URL result display
- Copy-to-clipboard button
- Responsive styling

## Verification completed

- React/Vite production build passed with `npm run build`
- Spring Boot backend compiled successfully using Maven
- PostgreSQL service is running
- `run.sh` has been syntax-checked

## Running the project

From the project root:

```bash
./run.sh
```

On the first run, the script may ask for the Linux sudo password. It creates a local development PostgreSQL role and the `url_shortener` database. It then starts:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

Press `Ctrl+C` to stop both services.

If Maven is not installed system-wide, install it with:

```bash
sudo apt-get install maven
```

## Authentication milestone — manual authentication implemented

The first authentication milestone is now implemented:

- Added a local `app_users` table through the `User` JPA entity.
- Added `POST /api/auth/register`.
- Added `POST /api/auth/login`.
- Added `POST /api/auth/logout`.
- Added `GET /api/auth/me` for session restoration.
- Added `GET /api/auth/csrf` for frontend CSRF-token setup.
- Passwords are hashed with BCrypt and are never returned in API responses.
- Authentication uses an HttpOnly session cookie.
- Anonymous URL shortening remains available.
- React now supports registration, login, logout, and session restoration.
- Added backend tests for email normalization, password hashing, and duplicate emails.
- Verified the complete authentication flow with a live HTTP smoke test against local PostgreSQL.
- Fixed a regression in `CreateShortUrlRequest` where a generated TODO accessor caused authenticated shortening to return `500 Internal Server Error`.
- Added a dismissible authentication overlay with login/register tabs and password visibility controls.
- Documented the testing strategy and verification commands in `ARCHITECTURE_DECISIONS.md`.

The next authentication stage is Google OAuth login as a second provider. User ownership and link history will follow once the account flow is established.

See `ARCHITECTURE_DECISIONS.md` for the detailed data flows, security decisions, and reasoning behind this plan.

## Next recommended milestones

1. Run the full application and test shortening, redirecting, invalid URLs, and duplicate aliases.
2. Add automated Spring Boot controller/service tests.
3. Add a `GET /api/urls/{code}` lookup endpoint if the frontend needs it.
4. Add link history in the React frontend.
5. Improve database constraints and handle rare generated-code collisions safely under concurrency.
6. Add expiration dates.
7. Add analytics such as click counts.
8. Add QR-code generation.
9. Add Docker and deployment configuration.

## Important context for resuming

The original files contain exploratory code and some known bugs. Do not delete or replace them unless explicitly requested; the newer implementation belongs in `backend/spring` and `frontend/react-app`.
