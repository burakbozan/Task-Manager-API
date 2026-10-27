# Task Manager API Architecture

## Overview

The application is a modular Spring Boot REST API. It manages authenticated
users and tasks, and provides user-owned agent definitions with a controlled
development lifecycle. It is packaged as a container and can use H2 locally or
PostgreSQL through Docker Compose.
JPA sessions are closed before web response serialization; controllers return
explicit response records rather than relying on lazy-loading side effects.

## Components

| Component | Responsibility |
| --- | --- |
| `AuthController` / `UserService` | Registration, login, password hashing, and JWT issuance |
| `JwtAuthenticationFilter` / `SecurityConfig` | Stateless bearer-token authentication and route authorization |
| `TaskController` / `TaskService` | Authenticated task CRUD |
| `AgentController` / `AgentService` | User-owned agent CRUD and lifecycle transitions |
| JPA repositories | Persistence for users, tasks, and agents |
| Actuator health indicators | Application, liveness, readiness, and database health |

## Agent Development Life Cycle

Agents are owned by the authenticated user and are created in `DRAFT`. Lifecycle
transitions must move one stage at a time:

```text
DRAFT -> DEVELOPMENT -> TESTING -> DEPLOYED -> RETIRED
```

Repeating the current stage is idempotent. Skipping stages or moving backwards
returns `409 Conflict`. Agent records include creation/update timestamps. API
responses omit internal persistence relations.

Tasks are associated with their creating user. Repository queries resolve tasks
through the authenticated username, and API requests cannot set IDs or owner
fields. Existing task rows without an owner remain inaccessible after upgrade
until they are explicitly migrated to an account.

## Phoenix resilience

Persistence writes for task and agent operations retry transient Spring Data
access failures up to three times, with exponential backoff starting at 100 ms.
Retry count and initial delay can be configured with
`DB_RETRY_MAX_ATTEMPTS` and `DB_RETRY_INITIAL_DELAY_MS`. If retries are
exhausted, the failure is propagated; the API does not return a false success.

Spring Boot graceful shutdown allows up to 20 seconds for in-flight requests.
Actuator exposes liveness and readiness probe routes, which are unauthenticated
for orchestrator use; readiness includes database health. Docker Compose checks
application readiness and restarts the API process if it exits (unless
explicitly stopped). The PostgreSQL service has its own health check and must
be healthy before the API starts.

## Runtime topology

```text
Client
  | HTTPS / bearer JWT
  v
Spring Boot REST API
  | JPA
  v
H2 (local) / PostgreSQL (Compose)
```

The API also exposes OpenAPI documentation and Prometheus-compatible Actuator
metrics. Business endpoints require a valid JWT; authentication, API
documentation, and health probes are public.

## Architecture regeneration

This document reflects the current source structure and runtime configuration.
Update it when adding/removing API modules, persistence relationships,
authentication behavior, or operational infrastructure.


