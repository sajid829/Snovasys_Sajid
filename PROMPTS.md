# AI-assisted development log

**Date:** 7 November 2026

This log records the actual Day 1 work in this repository. No commits or test results are claimed here unless verified during this session.

## Prompts and guidance

- The user requested inspection-first Day 1 setup for LedgerLine: initialize the existing React/Vite frontend, establish the Java 21/Spring backend and PostgreSQL/Flyway configuration, create an honest dashboard shell, add security/error-handling foundations, document the project, test it, and submit a daily commit to the existing GitHub branch.
- The user required the official problem statement and challenge instructions to be checked before implementation and prohibited inventing rubric criteria or claiming unfinished financial features.
- The official [problem statement](https://hackathon.snovasys.com/project/768) and [event instructions](https://hackathon.snovasys.com/event/3) were retrieved. The pages contained general challenge expectations but no detailed scoring weights. The user confirmed that no separate rubric was available and directed development to continue using only published requirements.

## Implementation decisions

- Preserved the existing React/Vite project and added Tailwind CSS 4 using its Vite plugin and Axios for centralized API requests.
- The dashboard shows an actual `/api/health` connectivity check, but no sample financial data or fake success state.
- Selected Spring Boot 3.5.16, the latest stable patch in the 3.5 line found in Maven Central during this work, with Java 21 and JUnit 5.
- Kept `/api/health` public and required authentication for every other API route. Disabled Spring Boot's generated default development user rather than introducing a known or ephemeral login.
- Configured local CORS origins, environment-based PostgreSQL settings, Flyway, and generic API error responses. The initial migration creates the `ledgerline` schema; financial tables and operations are deferred.
- Added a README checklist that explicitly marks all financial requirements as unfinished.

## Problems and corrections

- An initial `npm install` and one build/lint command ran from the repository root rather than `frontend`. The generated root npm artifacts were confirmed new in this session and removed. Dependencies were installed in `frontend`, and subsequent frontend commands explicitly changed to that directory.
- A Windows case-insensitive path collision between `HealthContext.jsx` and `healthContext.js` caused an initial production build error. The provider file was renamed to `HealthProvider.jsx`; the later build and lint completed successfully.
- Maven was not installed on `PATH`. A temporary Maven 3.9.16 distribution was used to generate the Maven Wrapper. The first wrapper generation attempt failed because PowerShell split the unquoted `-Dmaven=3.9.16` argument; quoting it corrected the command.
- Backend test output showed Spring Security creating a random development password in the MVC test context despite the initial auto-configuration exclusion. An explicit empty `UserDetailsService` was added so no default user is provisioned while authentication remains unimplemented.
- PostgreSQL 17's service is running, but no database environment variables are configured. A non-interactive `psql` connection check failed because no password was supplied, so database connectivity and starting the application against PostgreSQL remain unverified.

## Verification and submission

- Frontend production build: passed with `npm run build`.
- Frontend lint: passed with `npm run lint`.
- Backend `.\mvnw.cmd clean verify`: passed; 3 tests, 0 failures, 0 errors, 0 skipped. The corrected test context did not generate a default development password.
- Browser check: the Vite dashboard rendered at 375 px and 1440 px widths; desktop navigation appeared at the wide breakpoint, the page had no horizontal document overflow, and an unavailable backend was reported without showing financial data.
- PostgreSQL connection: not verified; `psql -w -h localhost -p 5432 -U ledgerline_app -d ledgerline_db -c "SELECT 1;"` failed because no password was supplied.
- API runtime health request: not run against a started application because database credentials are unavailable. The health endpoint is covered by the backend MVC test.
- Credential-pattern scan found no matches. The only `.env*` files are the root and frontend `.env.example` placeholders.
- Git submission outcome is reported in the final delivery summary; no commit is claimed in this prompt log.
