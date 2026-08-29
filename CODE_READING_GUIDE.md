# URL Shortener — Code Reading Guide

Since this is your first structured repository, read it in layers: understand the purpose first, then follow one complete user action through the system.

## 1. Start with the project overview

Read:

1. `PROGRESS.md`

`README.md` is currently very short, so `PROGRESS.md` is the more useful guide. It explains the decisions, current architecture, API routes, and future plans.

Then run the project:

```bash
./run.sh
```

Use the application once:

1. Enter a long URL.
2. Create a short link.
3. Open the short link.
4. Try an invalid URL.
5. Try a duplicate alias.

This gives you something concrete to connect to the code.

## 2. Understand the frontend entry point

Read:

1. `frontend/react-app/index.html`
2. `frontend/react-app/src/main.jsx`
3. `frontend/react-app/src/App.jsx`

Focus mainly on `App.jsx`.

Trace this flow:

```text
User submits form
        ↓
shorten()
        ↓
fetch("POST /api/urls")
        ↓
Backend response
        ↓
result or error appears on screen
```

While reading `App.jsx`, identify:

- React state: `longUrl`, `alias`, `result`, `error`, `loading`
- Form submission
- HTTP request using `fetch`
- Handling success and failure
- Copy-to-clipboard behavior
- Conditional rendering

Read `frontend/react-app/src/styles.css` afterward. It controls appearance, but it does not contain the application’s core logic.

You can postpone `vite.config.js`, `package.json`, and `package-lock.json` until you want to understand the frontend tooling.

## 3. Understand the backend request flow

Read these files in this order:

1. `backend/spring/src/main/java/com/urlshortener/UrlShortenerApplication.java`
2. `backend/spring/src/main/java/com/urlshortener/url/ShortUrlController.java`
3. `backend/spring/src/main/java/com/urlshortener/url/CreateShortUrlRequest.java`
4. `backend/spring/src/main/java/com/urlshortener/url/ShortUrlService.java`
5. `backend/spring/src/main/java/com/urlshortener/url/ShortUrlResponse.java`

The main creation flow is:

```text
POST /api/urls
        ↓
ShortUrlController.create()
        ↓
CreateShortUrlRequest validation
        ↓
ShortUrlService.create()
        ↓
Validate URL
        ↓
Generate code or validate alias
        ↓
Save URL
        ↓
Return ShortUrlResponse
```

The most important file here is `ShortUrlService.java`. Spend the most time on it. It contains the actual business logic.

## 4. Understand persistence and the database

Read:

1. `backend/spring/src/main/java/com/urlshortener/url/ShortUrl.java`
2. `backend/spring/src/main/java/com/urlshortener/url/ShortUrlRepository.java`
3. `backend/spring/src/main/resources/application.properties`

Learn these concepts:

- `@Entity`: Java class mapped to a database table
- `@Id`: primary key
- `@Column`: database column rules
- `JpaRepository`: database operations provided by Spring
- `existsByCode`: checks for duplicate short codes
- `findByCode`: looks up a stored URL
- Environment variables: database settings supplied by `run.sh`

The redirect flow is:

```text
GET /abc123
        ↓
ShortUrlController.redirect()
        ↓
ShortUrlService.resolve()
        ↓
ShortUrlRepository.findByCode()
        ↓
RedirectView sends the browser to the original URL
```

## 5. Understand errors and cross-origin requests

Read:

1. `backend/spring/src/main/java/com/urlshortener/url/UrlExceptions.java`
2. `backend/spring/src/main/java/com/urlshortener/url/ApiExceptionHandler.java`
3. `backend/spring/src/main/java/com/urlshortener/config/WebConfig.java`

These explain:

- What happens when a URL is invalid
- What happens when an alias already exists
- What happens when a code is not found
- Why the frontend is allowed to communicate with the backend on another port

## 6. Read the startup script last

Read `run.sh`.

By this point, you will understand why the script:

- Checks for npm, Maven, and PostgreSQL
- Creates the database user and database
- Starts Spring Boot
- Starts the React development server
- Stops both processes when you press `Ctrl+C`

## 7. Explore the historical files afterward

Read these only after understanding the current implementation:

- `backend/Main.java`
- `frontend/old/index.html`
- `frontend/old/urlShortner.js`
- `frontend/old/urlShortner.css`

These are earlier experiments preserved for learning. They are useful for seeing how the project evolved, but they are not the current application.

## The single best reading strategy

Pick one scenario and trace it across every layer:

> “I submit `https://example.com` with the alias `my-site`.”

Follow it through:

```text
App.jsx
  → POST /api/urls
  → ShortUrlController
  → CreateShortUrlRequest
  → ShortUrlService
  → ShortUrlRepository
  → ShortUrl database table
  → ShortUrlResponse
  → App.jsx displays the result
```

Then trace the second scenario:

> “I open `/my-site`.”

That will teach you the entire core architecture. Once you can explain those two flows without looking at the code, you’ll be ready to improve the project confidently.