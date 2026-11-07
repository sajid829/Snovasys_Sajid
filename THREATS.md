# LedgerLine threat model — initial

**Status:** Foundation-stage model, 7 November 2026. Financial services and their mitigations are planned, not yet implemented.

## Assets

- Account balances and the authoritative financial ledger
- User credentials, sessions/tokens, and role assignments
- Account ownership and authorization decisions
- Transaction requests, idempotency keys, and transaction outcomes
- Audit records, including denied or refused operations
- Database credentials, database contents, and application configuration

## Trust boundaries and attack surfaces

- Browser to frontend and frontend to REST API
- Public health endpoint versus authenticated API routes
- API services to PostgreSQL through JPA and Flyway
- Authentication, authorization, validation, and error-handling middleware
- Local developer environment, deployment configuration, logs, and Git history

## Threats and planned mitigations

| Threat | Risk | Planned mitigation | Day 1 control/status |
|---|---|---|---|
| Unauthorized transfers or access to another person's account | Theft or disclosure of funds and account data | Enforce role checks and account ownership on every server-side operation; deny by default; test cross-account access | Non-health API routes require authentication; real authentication, roles, and ownership checks are not implemented |
| Duplicate transfer or replayed request | A request is applied more than once | Require an idempotency key, persist its scope and final outcome with the ledger operation, and return the original result for a repeat | No financial operations exist yet |
| Overdraft or invalid balance change | Funds created or spent beyond the allowed balance | Validate amounts and balances server-side, use decimal monetary types, and make ledger entries the source of truth | No balances or financial types exist yet |
| Concurrent withdrawals or transfers | Race conditions produce a negative or inconsistent balance | Use database transactions and an explicit concurrency strategy (for example row locks or optimistic versions); test simultaneous requests | No financial operations or concurrency controls exist yet |
| Missing or altered history, including refused attempts | Transactions cannot be reconstructed or attributed | Record actor, request identity, outcome, and ledger references in a durable append-only audit model; restrict update/delete paths and test refused actions | No audit or transaction-history schema exists yet |
| Credential exposure through source, environment files, or logs | Database or user-account compromise | Keep secrets in environment/secret stores, ignore `.env` files, use placeholders in examples, and redact sensitive fields from API errors and logs | Environment placeholders and `.gitignore` are present; deployment secret handling remains future work |
| Cross-origin browser request abuse | Unauthorized browser-origin requests | Restrict CORS to explicitly configured origins; pair it with server-side authentication and appropriate CSRF protections for the chosen credential model | Local origins are allow-listed; production origins and authentication design remain future work |
| Sensitive internal details in error responses | Information leakage that assists an attacker | Return generic client-facing errors; log server-side details under access controls | Generic validation/unexpected-error responses are configured; production log policy remains future work |

## Security assumptions and next work

- The browser is untrusted; frontend visibility or validation never grants financial authority.
- A successful health response is not evidence that PostgreSQL is reachable or that financial invariants hold.
- Before enabling money movement, implement and test server-side authorization, account ownership, idempotency, transaction boundaries, concurrency control, double-entry conservation, overdraft prevention, and durable audit history.
- Before deployment, configure production CORS, TLS, secret storage, credential lifecycle, security logging/retention, and operational monitoring.
