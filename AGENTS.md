# Agent Instructions

## Project map

- Current backend: `backend/spring` (Java 21, Spring Boot, PostgreSQL, JPA).
- Current frontend: `frontend/react-app` (React + Vite).
- Startup: `./run.sh`.
- Historical code: `backend/Main.java` and `frontend/old/`; preserve unless explicitly asked.
- Decisions and flows: read `ARCHITECTURE_DECISIONS.md` only when the task needs design context.
- Progress: `PROGRESS.md`.
- Code review patterns and optimization notes: `CODE_REVIEW_NOTES.md`.
- Security risks and mitigations: `SECURITY_REVIEW.md` when changing auth, input validation, redirects, ownership, admin access, or deployment.

## Working rules

- Keep anonymous URL shortening working.
- Auth uses BCrypt, session cookies, and CSRF; never store or return plaintext passwords.
- Authenticated data must be scoped from the server-side session, never a client-supplied user ID.
- Keep API validation and authorization in the backend; treat frontend checks as UX only.
- Add or update focused tests for behavior changes.
- After backend changes run `cd backend/spring && mvn test`.
- After frontend changes run `cd frontend/react-app && npm run build`.
- Update `PROGRESS.md` and `ARCHITECTURE_DECISIONS.md` when behavior or architecture changes.
- Preserve unrelated user changes, including `.vscode/`.
- Treat security documentation as a source-review checklist, not proof of a completed penetration test.

## Reusable skill

For repository work, use `.codex/skills/url-shortener-dev/SKILL.md`.
