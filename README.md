# URL Shortener

A small URL shortener built for learning how a frontend, backend, database, authentication, and deployment flow connect together.

## Stack

- Backend: Java 21, Spring Boot, Spring Security, JPA
- Database: PostgreSQL
- Frontend: React, Vite

## What it does

- Shortens long URLs into compact codes
- Supports custom aliases
- Redirects short links to the original destination
- Supports session-based authentication
- Lets signed-in users manage their own links
- Includes an admin view for accounts with `ROLE_ADMIN`

## Run locally

Start PostgreSQL, then run:

```bash
./run.sh
```

The script starts:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`

On the first run, it may ask for your Linux sudo password so it can create the local PostgreSQL role and database used by the app.

## Useful paths

- `backend/spring/` - current Spring Boot backend
- `frontend/react-app/` - current React frontend
- `frontend/old/` - preserved historical frontend prototype
- `backend/Main.java` - preserved historical Java prototype
- `docs/PROGRESS.md` - project progress notes
- `docs/ARCHITECTURE_DECISIONS.md` - architecture and security decisions
- `docs/CODE_REVIEW_NOTES.md` - review patterns and code boundaries
- `docs/SECURITY_REVIEW.md` - security risks and mitigations

## Notes

The repository is set up for local development first. Production deployment, Docker, and other infrastructure pieces can be added later.
