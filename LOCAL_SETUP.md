# LedgerLine Local Setup (Windows)

This repository is a Java Spring Boot backend plus a React/Vite frontend. The code currently contains PostgreSQL and JWT-based authentication configuration, but no Redis integration and no active Redis config in the app.

## 1) Required prerequisites

### Required
- Java 25
- Node.js 20+ or 22+
- npm
- PostgreSQL 17
- Git

### Currently verified on this machine
- Java 21 is installed; install and select Java 25 for this upgraded backend
- Node.js and npm are installed
- PostgreSQL 17 is installed and the local PostgreSQL service is running
- Maven wrapper is included in the repo, so a separate Maven install is not required
- Redis is not installed and there is no Redis configuration in this repo

### Missing / not configured yet
- A PostgreSQL application role and database for LedgerLine must still be created
- A JWT secret must be generated in the local shell before starting the backend
- Redis is not currently used by the app code; it is not required for the current repo to run locally

## 2) Install and verify tools

### Java 25
Install Temurin JDK 25 or another Java 25 distribution.

```powershell
java -version
```

Expected: Java 25.x

### Node.js and npm
Install Node.js LTS.

```powershell
node -v
npm -v
```

Expected: Node version 20+ or 22+, with npm available.

### PostgreSQL 17
Install PostgreSQL 17 if not already installed, then start the local PostgreSQL service.

Check service:

```powershell
Get-Service postgres*
```

Check socket readiness:

```powershell
pg_isready -h localhost -p 5432
```

If PostgreSQL is not already running, start the PostgreSQL service or run the PostgreSQL server service from the installed PostgreSQL bundle.

## 3) Create the PostgreSQL database and role

Open PowerShell and connect to PostgreSQL as the local postgres user or another administrator role.

```powershell
psql -h localhost -U postgres
```

Then run:

```sql
CREATE ROLE ledgerline_app LOGIN;
ALTER ROLE ledgerline_app WITH PASSWORD 'YourStrongPasswordHere';
CREATE DATABASE ledgerline_db OWNER ledgerline_app;
```

Exit PostgreSQL:

```sql
\q
```

Important:
- Do not commit this password to source control.
- Use a strong password for local development.
- Do not run destructive SQL on an existing database.

## 4) Configure local environment variables

Open a fresh PowerShell session in the backend directory.

```powershell
Set-Location .\backend\ledgerline
$env:DB_URL = "jdbc:postgresql://localhost:5432/ledgerline_db"
$env:DB_USERNAME = "ledgerline_app"
$env:DB_PASSWORD = "YourStrongPasswordHere"

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = [byte[]]::new(32)
$rng.GetBytes($jwtBytes)
$env:LEDGERLINE_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$rng.Dispose()
[Array]::Clear($jwtBytes, 0, $jwtBytes.Length)

$env:LEDGERLINE_JWT_EXPIRATION_SECONDS = "900"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173"
```

Frontend environment variable:

```powershell
Set-Location .\frontend
$env:VITE_API_BASE_URL = "http://localhost:8080/api"
```

The frontend file [frontend/.env.example](./frontend/.env.example) uses this variable, and the frontend API client reads it from `import.meta.env.VITE_API_BASE_URL`.

## 5) Start the backend

From the backend directory:

```powershell
Set-Location .\backend\ledgerline
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Expected behavior:
- Flyway applies migrations from `src/main/resources/db/migration`
- Database schema is created in the `ledgerline` schema
- The backend listens on port 8080 by default
- The application logs show startup success once the database is reachable

If the backend fails due to connection or secret issues, verify:
- the database role and database exist
- the credentials and JDBC URL are correct
- the JWT secret is Base64-encoded and at least 32 bytes long
- CORS origins match the frontend URL

## 6) Start the frontend

Open a second PowerShell window and run:

```powershell
Set-Location .\frontend
npm install
npm run dev -- --host 0.0.0.0
```

The Vite dev server usually starts on:

```text
http://localhost:5173
```

This is the frontend URL for local browser access.

## 7) Health and readiness checks

### Backend health endpoint

```text
http://localhost:8080/api/health
```

Expected response:

```json
{"status":"UP"}
```

### Local frontend URL

```text
http://localhost:5173
```

### API calls from frontend

The React app uses:

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

This matches the Spring Boot backend and the local CORS configuration in `application.yml`.

## 8) Local CORS configuration

The backend CORS configuration is in:

- [backend/ledgerline/src/main/java/com/snovasys/ledgerline/config/CorsConfiguration.java](./backend/ledgerline/src/main/java/com/snovasys/ledgerline/config/CorsConfiguration.java)
- [backend/ledgerline/src/main/resources/application.yml](./backend/ledgerline/src/main/resources/application.yml)

The current local development default is:

```text
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

This matches the Vite dev server ports used by the frontend.

## 9) Redis status

The current repository does not contain Redis configuration, Redis client code, or Redis environment variables. The backend and frontend currently run without Redis.

If you later intentionally add Redis to the project, install Redis 7 locally and start it before using Redis-backed features. For the code in this repository today, Redis is not required for local execution.

## 10) Smoke-check results already verified

These checks were executed in this environment:
- Java 21 is installed; Java 25 must be selected for the upgraded backend
- Node.js and npm are installed
- PostgreSQL 17 is installed and the local server is accepting connections
- Frontend production build succeeds with `npm run build`
- Backend Maven test suite passes with `./mvnw test`
- Redis is not present in the project and is therefore not part of the current runtime flow

## 11) Local browser workflow

1. Start PostgreSQL if it is not already running
2. Create the LedgerLine database and role if missing
3. Set the backend environment variables in a PowerShell session
4. Run the backend with `./mvnw.cmd spring-boot:run`
5. In a second PowerShell session, set `VITE_API_BASE_URL` and run the frontend with `npm run dev -- --host 0.0.0.0`
6. Open `http://localhost:5173` in the browser
7. Confirm the app loads and calls the backend at `http://localhost:8080/api`

## 12) Important safety notes

- Do not hardcode credentials in source files
- Do not delete or recreate existing databases without explicit backup intent
- Do not run destructive migrations against a production-like database
- Do not change unrelated app logic just to make local startup work
- Keep environment variables in the current terminal session or in the hosting secret manager, not in committed files
