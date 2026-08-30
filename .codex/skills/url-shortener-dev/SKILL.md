---
name: url-shortener-dev
description: Maintain this URL shortener across its Spring backend, React frontend, PostgreSQL data, authentication, tests, and project docs.
---

# URL Shortener Development

Use for implementation, debugging, testing, or architecture work in this repository.

## Context

- Backend: Java 21 + Spring Boot in `backend/spring`; PostgreSQL via JPA.
- Frontend: React + Vite in `frontend/react-app`.
- Current auth: manual email/password, BCrypt, session cookie, CSRF; Google OAuth is planned.
- Anonymous shortening remains public. Authenticated links belong to their session user; `/api/admin/**` requires `ROLE_ADMIN`.
- Preserve `backend/Main.java` and `frontend/old/` as historical experiments.

## Rules

- Inspect the relevant current files before editing; do not rediscover the whole tree.
- Keep ownership and authorization server-derived from the authenticated session.
- Never store, log, or return plaintext passwords or password hashes.
- Keep validation in the backend and add focused tests for behavior changes.
- Update `PROGRESS.md` and `ARCHITECTURE_DECISIONS.md` for durable decisions or flows.
- For code review, use `CODE_REVIEW_NOTES.md`; for security-sensitive work, use `SECURITY_REVIEW.md` and distinguish existing controls from open risks.
- Preserve unrelated user changes.

## Verification

```bash
cd backend/spring && mvn test
cd frontend/react-app && npm run build
bash -n run.sh
```

For auth or persistence changes, also run a short local HTTP smoke test when the database is available. Read `ARCHITECTURE_DECISIONS.md` for endpoint flows and test meanings; load it only when needed.
