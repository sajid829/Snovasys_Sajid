# LedgerLine

LedgerLine is a financial application for authenticated banking accounts and, in later iterations, traceable money movement with a balanced ledger. Day 2 adds JWT authentication, server-enforced roles, account ownership, and read-only account views. Deposits, withdrawals, transfers, and ledger operations remain unimplemented.

## Challenge requirements and progress

The official [LedgerLine problem statement](https://hackathon.snovasys.com/project/768) describes safe, authorized, traceable money movement. The [official event instructions](https://hackathon.snovasys.com/event/3) require a working system, public GitHub repository, live website, tests, and a README that enables a stranger to run it. No separate marking scheme or scoring weights were published in the material checked on 7 November 2026.

| Requirement | Day 2 status |
|---|---|
| Administrator, Account Holder, and Read-only Auditor authentication and roles | Registration/login and API role authorization implemented; first administrator requires trusted DBA provisioning |
| Account creation, balance viewing, and statements | Account creation and balance viewing implemented; statements not implemented |
| Deposits, withdrawals, and transfers | Not implemented |
| Double-entry accounting and money conservation | Not implemented |
| Server-side prevention of negative balances | Not implemented |
| Idempotency and duplicate request prevention | Not implemented |
| Concurrent transaction safety | Not implemented |
| Complete transaction and audit history, including refused attempts | Not implemented |
| Backend account ownership and authorization checks | Implemented for account reads and creation; ownership comes from the authenticated JWT |
| Financial integrity, concurrency, idempotency, and access-control tests | Authentication, account ownership, and authorization tests implemented; financial integrity/concurrency/idempotency tests remain unimplemented |
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

The backend requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `LEDGERLINE_JWT_SECRET` in its process environment. The JWT secret must be Base64 that decodes to at least 32 cryptographically random bytes. Spring validates that the required variables are present and validates the JWT key before serving requests. Flyway explicitly uses the same URL, username, password, and PostgreSQL driver as the application datasource. [`.env.example`](./.env.example) contains placeholders for reference only; Spring Boot does not load `.env` files automatically.

In Windows PowerShell, open the backend directory, configure the variables in that terminal, and start the application from the same terminal:

```powershell
Set-Location .\backend\ledgerline
$env:DB_URL = "jdbc:postgresql://localhost:5432/ledgerline_db"
$env:DB_USERNAME = "ledgerline_app"
$securePassword = Read-Host "Local database password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = [byte[]]::new(32)
$rng.GetBytes($jwtBytes)
$env:LEDGERLINE_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$rng.Dispose()
[Array]::Clear($jwtBytes, 0, $jwtBytes.Length)
$env:LEDGERLINE_JWT_EXPIRATION_SECONDS = "900"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173"
.\mvnw.cmd spring-boot:run
```

Use the password you set for the PostgreSQL `ledgerline_app` role at the secure prompt. The command generates a new JWT signing secret in memory for this terminal session; generate and store a stable secret in your deployment secret manager for deployments. Do not put either secret in a command literal, committed file, or Copilot chat. If the role or database does not exist, connect to PostgreSQL as an administrator and create them first:

```sql
CREATE ROLE ledgerline_app LOGIN;
\password ledgerline_app
CREATE DATABASE ledgerline_db OWNER ledgerline_app;
```

The `\password` command prompts securely for a role password in `psql`. If the role already exists, use `\password ledgerline_app` to set/reset its password and skip `CREATE ROLE`.

Flyway applies `src/main/resources/db/migration` on backend startup. V1 creates the `ledgerline` schema; V2 creates the user and account tables, role constraints, indexes, foreign key, and `numeric(19,2)` balance column. Do not edit migrations after they have been applied.

Public registration always assigns `ACCOUNT_HOLDER`. For the first administrator, register the intended account normally, then have a trusted PostgreSQL administrator promote that specific account:

```sql
UPDATE ledgerline.app_user
SET role = 'ADMIN', token_version = token_version + 1
WHERE email = lower('admin@example.com');
```

Confirm exactly one row was updated. Sign in again to receive a token with the new role. Never expose an unauthenticated role-selection or administrator-registration endpoint.

## Backend setup

From `backend/ledgerline` in PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The public routes are `GET /api/health`, `POST /api/auth/register`, and `POST /api/auth/login`. All other routes require a valid Bearer token. The frontend keeps the access token in memory only; signing out increments the user's token version, invalidating all of their issued tokens.

## Authentication and authorization APIs

| Method and path | Access | Behavior |
|---|---|---|
| `POST /api/auth/register` | Public | Validates email and password, creates an `ACCOUNT_HOLDER`, returns a Bearer access token |
| `POST /api/auth/login` | Public | Returns a token for valid credentials; invalid credentials receive a generic `401` |
| `POST /api/auth/logout` | Authenticated | Increments token version and invalidates issued tokens; returns `204` |
| `GET /api/accounts` | Any role | Account holders receive only their accounts; ADMIN/AUDITOR receive the permitted account list |
| `POST /api/accounts` | ACCOUNT_HOLDER | Creates an owned account with a server-generated number and a zero balance |
| `GET /api/accounts/{accountId}` | Any role | Account holders can read only owned accounts; ADMIN/AUDITOR may read all |
| `GET /api/admin/users` | ADMIN | Lists user summaries without password hashes |
| `PATCH /api/admin/users/{userId}` | ADMIN | Changes role or enabled status; role/status changes revoke existing tokens |

Registration JSON: `{"email":"person@example.com","password":"at-least-12-characters"}`. Account creation JSON: `{"accountType":"CHECKING"}` or `{"accountType":"SAVINGS"}`. A client cannot submit a user ID, role, account owner, status, account number, or balance to these operations.

### Roles

- **ADMIN:** lists and manages users, and reads all account records.
- **ACCOUNT_HOLDER:** creates accounts and reads only their own accounts and balances.
- **AUDITOR:** reads account records without write access.

Role authorization is enforced in Spring Security; account ownership is additionally enforced by owner-filtered repository lookups. Admins cannot disable/demote themselves, and the final enabled admin cannot be removed.

### Authentication security decisions

- Passwords are BCrypt-hashed; hashes are never included in response DTOs.
- Access tokens are HMAC-SHA256 signed, short-lived (15 minutes by default, configurable from 60–3600 seconds), and validated for issuer, signature, expiry, user status, role, and token version.
- Logout and admin access changes increment `token_version`; validation checks the current database value, so earlier tokens stop working immediately.
- Tokens are held in frontend memory, not local storage. Reloading the page requires signing in again.
- The signing secret is `LEDGERLINE_JWT_SECRET`, Base64-encoded and at least 32 random bytes. Do not reuse the development key in deployment.
- The browser is not trusted to supply user identity, role, account owner, or balance.

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

Build and lint the frontend with `npm run build` and `npm run lint`. Run backend tests with `.\mvnw.cmd test`. The current backend tests are unit and MVC/security-slice tests; they do not prove PostgreSQL migration or live database connectivity.

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
- All API routes except health, registration, and login require JWT authentication; role and account-ownership checks are enforced server-side.
- Error responses use generic messages and do not return exception details.
- Never commit `.env` files, passwords, tokens, or real account data.

See [THREATS.md](./THREATS.md) for the initial threat model and planned financial controls.
