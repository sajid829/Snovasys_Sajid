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

## Day 2 — 8 November 2026

### Actual user prompt

The user requested implementation of registration/login, BCrypt, JWT authentication and logout invalidation, the ADMIN/ACCOUNT_HOLDER/AUDITOR roles, owner-scoped account creation and read APIs, a React auth/dashboard flow, additive Flyway migrations, meaningful backend security/service tests, and updates to README/PROMPTS/THREATS. The user noted that PostgreSQL credentials are not yet configured and explicitly prohibited weakening database authentication or Flyway to bypass this.

### Implementation decisions

- Public registration always provisions `ACCOUNT_HOLDER`; a role cannot be submitted at registration. A trusted DBA must promote the first administrator directly after registration. Admin user changes are ADMIN-only.
- Access tokens are short-lived HMAC-SHA256 JWTs. Database-backed `token_version`, enabled state, and current role are checked for each token; logout and access changes increment the version to revoke existing tokens.
- The frontend keeps the access token in memory only. Account API requests send only account type on creation; authenticated identity supplies the owner. Monetary balances are server-created zero values and serialized as decimal strings.
- Added V2 as a new Flyway migration; V1 was preserved. The new schema uses UUID keys, unique case-normalized emails/account numbers, role/type/status checks, ownership foreign keys/indexes, and `numeric(19,2)` balances.
- No PostgreSQL-backed test database or extra persistence framework was introduced. Tests exercise service behavior and Spring Security MVC authorization without pretending to verify live database persistence.

### Problems and corrections

- A first backend compile found that Spring's `Jwt` API has no `getClaimAsLong`; logout now reads the numeric token-version claim and converts it to a long.
- Initial MVC authorization tests used Spring Security's test JWT shortcut, which did not exercise the configured role-claim converter. They now pass a Bearer token through the real filter chain with a mocked decoder, so the role converter and request matchers are exercised.
- JWT tests found issuer claims must be valid URI values and that JWT timestamps serialize to second precision. The configured issuer is a URI and issued timestamps are truncated to seconds.
- The first frontend lint pass found effect state-update and Fast Refresh warnings. Data loading is asynchronous/cancellable, and the auth hook was moved into its own hook module.
- A security review found the unknown-user login path's dummy BCrypt hash used a lower work factor than real passwords. It is now generated with the configured encoder so both paths use the same cost.
- Existing, uncommitted PostgreSQL environment-variable fixes from the preceding user request were preserved and extended with the required JWT secret; neither DB passwords nor signing keys were added to source.

### Verification

- Backend `.\mvnw.cmd clean verify`: passed; 29 tests, 0 failures, 0 errors, 0 skipped.
- Frontend `npm install`: completed; npm reported 0 vulnerabilities.
- Frontend `npm run build`: passed.
- Frontend `npm run lint`: passed.
- PostgreSQL migration/connectivity: not verified because database credentials are not configured. No live database result is claimed.
- Git submission: final commit and push status is reported in the delivery summary.
