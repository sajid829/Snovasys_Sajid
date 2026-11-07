# LedgerLine

LedgerLine is a financial application foundation for money movement with a traceable, balanced ledger. The Day 1 scope establishes the frontend shell, secured Spring API foundation, PostgreSQL/Flyway configuration, and project documentation. Financial operations and role-based accounts are not implemented yet.

## Challenge requirements and progress

The official [LedgerLine problem statement](https://hackathon.snovasys.com/project/768) describes safe, authorized, traceable money movement. The [official event instructions](https://hackathon.snovasys.com/event/3) require a working system, public GitHub repository, live website, tests, and a README that enables a stranger to run it. No separate marking scheme or scoring weights were published in the material checked on 7 November 2026.

| Requirement | Day 1 status |
|---|---|
| Administrator, Account Holder, and Read-only Auditor authentication and roles | Not implemented |
| Account creation, balance viewing, and statements | Not implemented |
| Deposits, withdrawals, and transfers | Not implemented |
| Double-entry accounting and money conservation | Not implemented |
| Server-side prevention of negative balances | Not implemented |
| Idempotency and duplicate request prevention | Not implemented |
| Concurrent transaction safety | Not implemented |
| Complete transaction and audit history, including refused attempts | Not implemented |
| Backend account ownership and authorization checks | Not implemented; future API routes are protected by default |
| Financial integrity, concurrency, idempotency, and access-control tests | Not implemented; Day 1 tests cover health, default route protection, and CORS only |
| Public deployment/live website | Not deployed |

These features must be implemented and tested before they are described as complete.

## Technology

- Frontend: React, Vite, JavaScript, Tailwind CSS 4, Axios
- Backend: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Validation
- Database: PostgreSQL 17, Flyway
- Tests: JUnit 5, Mockito, Spring Boot Test, Spring Security Test

## Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL 17
- Git

The Maven Wrapper is included, so a separate Maven installation is not required.

## Database setup

Start PostgreSQL, then create an application role and database. For example, from `psql` connected as a PostgreSQL administrator:

```sql
CREATE ROLE ledgerline_app LOGIN;
```

Set the role password interactively in `psql` so it does not need to be written in a command or committed:

```text
\password ledgerline_app
```

Then create the database:

```sql
CREATE DATABASE ledgerline_db OWNER ledgerline_app;
```

The backend reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from its process environment. [`.env.example`](./.env.example) contains placeholders for reference only; Spring Boot does not load `.env` files automatically. In PowerShell, set the variables in the same terminal used to start the backend:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/ledgerline_db"
$env:DB_USERNAME = "ledgerline_app"
$securePassword = Read-Host "Local database password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173"
```

Flyway applies `src/main/resources/db/migration` on backend startup. The initial migration creates the `ledgerline` schema; it intentionally creates no financial tables.

## Backend setup

From `backend/ledgerline` in PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The only public application route is `GET /api/health`. All other routes require authentication; authentication and user provisioning are future work, so no default user or password is created.

## Frontend setup

From `frontend`:

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev
```

The Vite development server starts at `http://localhost:5173`. Set `VITE_API_BASE_URL` in `frontend/.env.local` to change the API base URL. The initial dashboard checks the real health endpoint and displays no fabricated balances, accounts, or transactions.

## Run and build

Open two terminals:

1. Start PostgreSQL and the backend using the database environment variables above and `.\mvnw.cmd spring-boot:run`.
2. In `frontend`, run `npm run dev`.

Build the frontend with `npm run build`. Run backend tests with `.\mvnw.cmd test`.

## API health

```http
GET http://localhost:8080/api/health
```

Expected response:

```json
{"status":"UP"}
```

This endpoint confirms that the application process is responding. It does not report database connectivity or financial integrity.

## Security foundation

- CORS allows the two local Vite origins by default; override them with `CORS_ALLOWED_ORIGINS`.
- Only the health endpoint is public. Other API paths require an authenticated principal.
- Spring Security's generated development user is disabled until real authentication is implemented.
- Error responses use generic messages and do not return exception details.
- Never commit `.env` files, passwords, tokens, or real account data.

See [THREATS.md](./THREATS.md) for the initial threat model and planned financial controls.
