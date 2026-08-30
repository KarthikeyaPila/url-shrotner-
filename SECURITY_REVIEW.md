# URL Shortener — Security Review

Last reviewed: 2026-08-30

This is a practical source-code review for the current MVP. It identifies realistic attack classes, records protections already implemented, and lists hardening work for later. It is not a penetration test, production certification, or guarantee that no vulnerability exists.

## Security boundaries

The application has three important boundaries:

```text
Browser / React UI
        ↓ HTTP + cookies + CSRF header
Spring Boot API
        ↓ JPA repositories
PostgreSQL
```

The browser is an untrusted client. It may request an action, but the backend must validate input, identify the session, check ownership, enforce roles, and decide what data is returned.

## Risk summary

| Area | Current state | Priority |
|---|---|---|
| Plaintext password storage | BCrypt is used; plaintext passwords are not stored or returned | Protected |
| Session theft through JavaScript | Session is cookie-based; frontend does not store a JWT in local storage | Protected by design; verify production cookie flags |
| CSRF | Spring CSRF protection and frontend token header are enabled | Protected for current cookie flow |
| Cross-origin abuse | CORS is restricted to local frontend origins and credentials are enabled intentionally | Review before production |
| Broken access control | Link operations query by code and authenticated owner; admin routes require `ROLE_ADMIN` | Protected in current routes |
| SQL injection | JPA repository methods are used instead of string-built SQL | Low current risk |
| Brute force and abuse | No rate limiting is implemented | Open risk |
| Alias/code race conditions | Database uniqueness exists, but service retries are not fully race-safe | Open correctness/security hardening |
| Production transport/configuration | Local development configuration is not production hardening | Open risk |
| Dependency vulnerabilities | No recurring dependency audit is configured | Open process gap |

## Attack classes and current approach

### 1. Password database leak

**Attack:** An attacker obtains the `app_users` table and tries to recover user passwords.

**Current approach:** Registration hashes passwords with BCrypt. `UserResponse` does not expose the password or password hash.

**Remaining work:** Use a strong BCrypt cost appropriate for production, protect database backups, rotate secrets, and consider Argon2id when the project is ready for a security-focused upgrade. Password reuse outside this application cannot be controlled.

### 2. Credential stuffing and brute-force login

**Attack:** An attacker repeatedly tries passwords from previous breaches or guesses passwords against `/api/auth/login`.

**Current approach:** Login failures use a generic invalid-credentials response, so the password check does not intentionally reveal whether a specific account exists.

**Remaining risk:** There is no rate limiting, IP throttling, account lockout strategy, CAPTCHA step, or anomaly monitoring.

**Recommended hardening:** Add rate limiting at the API or reverse-proxy layer, use progressive delays, monitor repeated failures, and add email verification or MFA later.

### 3. Account/email enumeration

**Attack:** An attacker submits many registration attempts and learns which email addresses already have accounts from the duplicate-email response.

**Current approach:** Login errors are generic.

**Remaining risk:** Registration explicitly reports an existing email. This is convenient UX but can reveal account existence.

**Trade-off:** For a small learning application this may be acceptable. For a public production service, use a less revealing registration response where appropriate and protect the endpoint with rate limiting.

### 4. Session theft and session fixation

**Attack:** An attacker steals or manipulates the session identifier and acts as the user.

**Current approach:** Spring Security manages the server session. The application does not put authentication tokens in `localStorage`, and logout invalidates the authenticated flow.

**Remaining work:** Confirm production cookies use `Secure`, `HttpOnly`, and an appropriate `SameSite` policy. Use HTTPS everywhere in production, set a session timeout, and verify session ID rotation on login with an integration test.

### 5. Cross-site request forgery (CSRF)

**Attack:** A malicious website causes a logged-in browser to submit a state-changing request such as delete or disable.

**Current approach:** Cookie-based sessions are paired with Spring CSRF protection. The frontend requests a CSRF token and sends it in `X-XSRF-TOKEN`. CORS also restricts allowed frontend origins.

**Remaining work:** Add automated tests for missing, invalid, and valid CSRF tokens on login-sensitive state-changing routes. Do not disable CSRF merely to make a frontend request easier.

### 6. Cross-origin resource sharing (CORS) abuse

**Attack:** An untrusted origin tries to read authenticated API responses or perform credentialed requests.

**Current approach:** `WebConfig` allows known local frontend origins, selected methods, selected headers, and credentials. It does not use a wildcard origin with credentials.

**Remaining work:** Replace local origins with the exact production frontend origin at deployment. Never combine `Access-Control-Allow-Origin: *` with credentialed cookies. Keep the allowed methods and headers minimal.

### 7. Broken object-level authorization / IDOR

**Attack:** User A changes a link code in a request and reads, disables, or deletes User B’s link.

**Current approach:** Management endpoints derive the user from Spring Security’s `Authentication`. The service uses `findByCodeAndOwner(code, owner)`, not a user ID supplied by React.

**Remaining work:** Add tests proving User A receives no access to User B’s links, including codes guessed or copied from another account. Continue applying this rule to every future user-owned resource.

### 8. Admin privilege escalation

**Attack:** A normal user changes a frontend value or manually calls `/api/admin/**` to view all users and links.

**Current approach:** Spring Security requires `ROLE_ADMIN` for every `/api/admin/**` endpoint. The React role check only hides or shows the button; it is not trusted as security.

**Remaining work:** Add automated tests for anonymous `401`, normal-user `403`, and admin success on each admin endpoint. Create a secure production admin bootstrap process instead of manual database promotion.

### 9. SQL injection

**Attack:** Malicious URL, alias, email, or code input changes a database query.

**Current approach:** The application uses Spring Data JPA repository methods and does not concatenate user input into SQL strings.

**Remaining work:** Keep using parameterized repository/query APIs if custom queries are added. Review any future native SQL carefully.

### 10. Cross-site scripting (XSS)

**Attack:** An attacker stores script content in a URL or alias and causes it to execute in another user’s browser.

**Current approach:** The React UI renders values through normal JSX, which escapes text. URL validation accepts only HTTP/HTTPS schemes, so `javascript:` is not accepted as a destination. There is no `dangerouslySetInnerHTML` in the current frontend.

**Remaining work:** Keep output escaping enabled, add a restrictive Content Security Policy in production, and be careful if HTML rendering or rich notes are added later. A shortened URL can still point to a malicious external website; that is phishing/abuse risk even when it is not XSS inside this application.

### 11. Open redirect and unsafe redirect abuse

**Attack:** The service is used to create convincing links to phishing or malware pages, or an attacker injects a non-HTTP scheme.

**Current approach:** Destinations are restricted to HTTP and HTTPS URIs with a host. Public resolution only follows active, non-deleted links.

**Remaining risk:** Any URL shortener can be abused as a reputation-hiding redirect. The current application does not maintain a blocklist, malware reputation check, abuse-report workflow, or destination warning page.

**Important distinction:** The current server returns a redirect; it does not fetch the destination itself. That means this is primarily a user-safety and abuse problem, not automatically a server-side request forgery problem.

### 12. Server-side request forgery (SSRF)

**Attack:** The server is tricked into fetching internal addresses such as `localhost`, cloud metadata endpoints, or private network services.

**Current approach:** The current create and resolve flows store a URL and return a browser redirect; they do not make an HTTP request to the submitted destination.

**Remaining risk:** SSRF could appear later if link validation, previews, screenshots, metadata extraction, malware scanning, or analytics begin fetching destinations. Any such feature must block private, loopback, link-local, and metadata IP ranges and re-check redirects.

### 13. Denial of service (DoS)

**Attack:** An attacker consumes CPU, memory, database connections, or storage with huge or repeated requests.

**Current approach:** Basic request validation and database column lengths limit some input sizes. Generated codes use a bounded length.

**Remaining risk:** Public shortening has no rate limit, quota, abuse detection, pagination enforcement, or storage policy. Admin summary currently loads all links before counting statuses, which is inefficient as data grows.

**Recommended hardening:** Add request/body limits, rate limits, per-user quotas, pagination, database count queries, and monitoring. Add maximum alias validation aligned with the database column length.

### 14. Race conditions and uniqueness bypass attempts

**Attack:** Concurrent requests attempt to claim the same alias or generated code, causing inconsistent behavior or errors.

**Current approach:** The database has a unique constraint on `short_urls.code`, and the service checks for existing values before saving.

**Remaining risk:** A check-then-save sequence is not by itself atomic. Concurrent requests can pass the check simultaneously. A collision may surface as a database exception instead of a clean retry or conflict response.

**Recommended hardening:** Treat the database constraint as authoritative, catch the unique-constraint failure, retry generated codes, and return a controlled conflict for aliases. Add concurrent integration tests.

### 15. Sensitive data exposure

**Attack:** A response, log, URL, or admin page exposes passwords, session data, private links, or personal information.

**Current approach:** Password hashes are not included in `UserResponse`. Admin data is protected behind the admin role. The browser does not connect directly to PostgreSQL.

**Remaining risk:** Link destinations may themselves contain tokens or private query parameters. Admin users can view all stored destinations. Current local development logs and database backups have not been designed as a privacy system.

**Recommended hardening:** Avoid logging full URLs where possible, document admin access expectations, protect backups, consider URL redaction in logs, and define a retention policy.

### 16. Dependency and supply-chain vulnerabilities

**Attack:** A vulnerable Spring, Maven, npm, or transitive dependency is exploited.

**Current approach:** Dependencies are managed through Maven and npm lockfiles.

**Remaining work:** Run regular `mvn dependency-check` or an equivalent scanner, `npm audit` with human review, keep lockfiles committed, update dependencies deliberately, and use automated dependency alerts in the repository host.

### 17. Clickjacking and browser security headers

**Attack:** The app is embedded in an attacker-controlled frame or browser behavior weakens protections.

**Current approach:** Spring Security supplies several default security headers.

**Remaining work:** Verify the actual production response headers and explicitly configure a Content Security Policy, frame policy, referrer policy, and permissions policy appropriate for the deployed frontend.

## Security decisions already made

- Passwords are hashed with BCrypt.
- Passwords and password hashes are never returned in user responses.
- Cookie-based server sessions were chosen instead of browser-stored JWTs.
- CSRF protection is enabled for the cookie session model.
- CORS is restricted to known local origins during development.
- Anonymous URL creation remains public, but user-owned operations require the authenticated session.
- Ownership is derived server-side from `Authentication`.
- Admin APIs require `ROLE_ADMIN`.
- URL schemes are limited to HTTP and HTTPS.
- Deleted and disabled links do not resolve publicly.
- JPA repositories are used instead of string-built SQL.
- Earlier code and credentials are not used as a reason to weaken the current security boundary.

## Recommended security work sequence

1. Add automated authorization and CSRF tests.
2. Add rate limiting for login, registration, and public shortening.
3. Add safe production environment and cookie configuration.
4. Make alias/code collision handling concurrency-safe.
5. Add input-size limits and align validation with database lengths.
6. Replace full-table admin counts with database queries and enforce pagination.
7. Add dependency scanning and security response headers.
8. Define privacy, URL retention, abuse reporting, and administrator-access policies.
9. Perform a real staging penetration test before public deployment.

## Review checklist for future features

Before adding a feature, ask:

- Can an anonymous user call this endpoint?
- What exact session or role is required?
- Is ownership checked on the backend?
- Can the input be oversized, malformed, or attacker-controlled?
- Does the feature fetch a user-supplied URL from the server?
- Could the response reveal private data?
- Does a state-changing browser request need CSRF protection?
- Does the database constraint still protect uniqueness under concurrency?
- Are tests covering both allowed and denied behavior?
