# LedgerLine threat model

**Status:** Updated 12 November 2026. Authentication, account access, and financial transaction code are implemented and covered by mock-based unit/MVC tests. PostgreSQL migration execution and live database behavior have not been verified in this environment. Failed-attempt auditing remains unimplemented.

## Assets

- Account balances and the authoritative financial ledger
- User credentials, sessions/tokens, and role assignments
- JWT signing secret and BCrypt password hashes
- Account ownership and authorization decisions
- Transaction requests, idempotency keys, and transaction outcomes
- Audit records, including denied or refused operations
- Database credentials, database contents, and application configuration

## Trust boundaries and attack surfaces

- Browser to frontend and frontend to REST API
- Public health endpoint versus authenticated API routes
- API services to PostgreSQL through JPA and Flyway
- Authentication, authorization, validation, and error-handling middleware
- Public registration/login and Bearer-token endpoints
- Local developer environment, deployment configuration, logs, and Git history

## Threats and planned mitigations

| Threat | Risk | Planned mitigation | Current control/status |
|---|---|---|---|
| Unauthorized transfers or access to another person's account | Theft or disclosure of funds and account data | Enforce roles and ownership on every server-side operation; deny by default; test cross-account access | Money movement is ACCOUNT_HOLDER-only; transaction services derive actor from JWT and require source/destination ownership. Non-owned resources return 404. MVC tests verify denied roles; service tests verify ownership rejection |
| Weak credentials or credential stuffing | Account takeover | BCrypt password hashes, input limits, generic login errors, rate limiting, and secure password handling | BCrypt strength 12, 12-character minimum, generic invalid-login response and dummy hash check are implemented; production rate limiting and MFA are not |
| JWT forgery, theft, or continued use after logout/role change | Impersonation or stale privileges | Use a strong protected signing key, short expiry, signature/issuer validation, least-privilege claims, and revocation | HMAC-SHA256 signed JWT; startup requires a Base64 secret of at least 32 bytes; issuer and expiry validation; token version, active status, and current role are checked against the database on requests. Frontend stores tokens in memory |
| Public user registration grants elevated privileges | Privilege escalation | Fix the public registration role and restrict role changes to trusted administrators | Public registration assigns only ACCOUNT_HOLDER; admin user changes require ADMIN authorization. Initial ADMIN requires trusted DBA promotion |
| Unauthorized changes to accounts or balances | Theft or corrupt financial records | Restrict write APIs by role, derive identity from validated token, and never accept owner or balance values from clients | Account create and money movement are ACCOUNT_HOLDER-only; transaction actor derives from token; balances and ownership are not request fields |
| Auditor modifies users or accounts | Concealed or unauthorized changes | Read-only role enforcement on every write route | AUDITOR can read account records; account create and admin APIs are denied by Spring Security and tested |
| Duplicate transfer or replayed request | A request is applied more than once | Require an idempotency key, persist its scope and final outcome with the ledger operation, and return the original result for a repeat | Unique `(actor_id, idempotency_key)` database constraint; exact replays return the original transaction and mismatched reuse conflicts. Concurrent duplicate behavior has not been tested against PostgreSQL |
| Overdraft or invalid balance change | Funds created or spent beyond the allowed balance | Validate amounts and balances server-side, use decimal monetary types, and make ledger entries the source of truth | `BigDecimal` and `numeric(19,2)`; strict positive amount validation, debit checks, and existing nonnegative DB constraint. Financial tests use mocks |
| Concurrent withdrawals or transfers | Race conditions produce a negative or inconsistent balance | Use database transactions and an explicit concurrency strategy (for example row locks or optimistic versions); test simultaneous requests | `@Transactional` operations lock account rows pessimistically; transfers lock in UUID order. Concurrent behavior is not verified against a live database |
| Missing or altered history, including refused attempts | Transactions cannot be reconstructed or attributed | Record actor, request identity, outcome, and ledger references in a durable append-only audit model; restrict update/delete paths and test refused actions | Successful transaction/ledger rows persist actor and idempotency key; DB triggers block update/delete. Transaction history is read-only. Rejected-attempt audit events are not persisted |
| Credential exposure through source, environment files, or logs | Database or user-account compromise | Keep secrets in environment/secret stores, ignore `.env` files, use placeholders in examples, and redact sensitive fields from API errors and logs | Required DB and JWT environment variables; `.env` files ignored; hashes/tokens excluded from user DTOs; API error messages generic. Production secret management and log controls remain deployment work |
| Cross-origin browser request abuse | Unauthorized browser-origin requests | Restrict CORS to explicitly configured origins; pair it with server-side authentication and appropriate CSRF protections for the chosen credential model | Local origins are allow-listed; production origins and authentication design remain future work |
| Sensitive internal details in error responses | Information leakage that assists an attacker | Return generic client-facing errors; log server-side details under access controls | Validation, auth, authorization, and unexpected errors use generic response DTOs; unexpected logging avoids exception messages and stack traces. Production log policy remains future work |

## Security assumptions and next work

- The browser is untrusted; frontend visibility or validation never grants financial authority.
- A successful health response is not evidence that PostgreSQL is reachable or that financial invariants hold.
- JWT user, enabled state, token version, and role are checked against persisted data on each authenticated request. This favors immediate revocation over reduced database reads.
- The initial admin is a trusted operator-provisioning task; public sign-up never accepts a role.
- Before submission/deployment, run V3 against PostgreSQL 17 and verify Flyway, deferred balance/immutability triggers, rollback behavior, idempotency races, and concurrent withdrawals using an integration database. Add rejected-attempt auditing if required.
- Before deployment, configure production CORS, TLS, secret storage, credential lifecycle, security logging/retention, and operational monitoring.
