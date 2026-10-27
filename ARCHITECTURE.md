# Task Manager API Architecture

## Purpose and status

This document describes the target architecture for evolving the Task Manager
API into a domain-oriented, cloud-native system, together with the current
repository baseline. The target is a direction for incremental implementation,
not a claim that the services, frontend, gateway, service mesh, or Kubernetes
deployment already exist.

The current application is a Java 25, Spring Boot REST API. It serves
authentication, tasks, and user-owned agents from one deployable application,
uses Spring Data JPA with H2 locally or PostgreSQL in Docker Compose, and
provides JWT authentication, Actuator health and metrics, JSON logs, and
graceful shutdown. The current agent stages are `DRAFT`, `DEVELOPMENT`,
`TESTING`, `DEPLOYED`, and `RETIRED`; they are not yet the full ADLC lifecycle
defined below. The files in `prompts/` and `orchestrator.py` describe and print
an agent workflow, but do not deploy independent agent services.

## Target architecture

The target separates independently deployable services along domain
boundaries. Each service owns its data and exposes supported APIs or events;
services must not read or write another service's database directly. A web
frontend calls the API gateway rather than depending on internal service
addresses.

```text
Browser
  |
  v
Web frontend (React, Angular, or Vue; selection pending)
  |
  v
API gateway (authentication enforcement, routing, rate limits)
  |
  +---- User service -------- User database
  |
  +---- Task service -------- Task database
  |
  +---- Workflow service ---- Agent and workflow database
             |
             +---- Planner / Researcher / Coder / Reviewer / Orchestrator

Service-to-service traffic: Istio-managed mTLS, timeouts, and resilience
Platform: Kubernetes or OpenShift; immutable container images from CI/CD
Telemetry: centralized logs, metrics, and distributed traces
```

The service mesh is an infrastructure concern, not a replacement for
application authorization or API gateway policy. Rate limits should be applied
at the gateway and, where needed, at sensitive service operations. Internal
services should be reachable only through approved ingress or service-to-service
paths.

### Domain and service ownership

| Service | Domain responsibilities | Data ownership |
| --- | --- | --- |
| User service | Account registration, login, password hashing, and issuing or validating identity claims | Users and credential metadata |
| Task service | Authenticated task CRUD, task ownership, and task lifecycle | Tasks and their owner identifiers |
| Workflow service | User-owned agent definitions, ADLC stage transitions, and orchestration workflow state | Agents, workflow state, and workflow history |
| API gateway | External routing, request limits, and edge policy; no domain persistence | None |
| Web frontend | User-facing task, account, and agent workflows | Client-side transient state only; no authoritative domain data |

The current application combines the user, task, and agent domains. Splitting
them is a migration: preserve JWT identity and per-user authorization, define
service API contracts, and move each domain's data to its owning service. Do not
perform cross-service transactions by sharing databases. Use explicit APIs or
events and design operations to tolerate retries and partial failure.

The frontend framework remains a decision for implementation planning. Select
React, Angular, or Vue based on product, team, and deployment requirements before
creating the frontend application; this document does not assume one.

### API and communication boundaries

- The gateway exposes versioned public routes and forwards requests to the
  owning domain service. Preserve existing `/api/auth`, `/api/tasks`, and
  `/api/agents` behavior during migration where compatibility is required.
- Services validate authorization for their own data. A valid JWT alone does
  not grant access to another user's tasks or agents.
- Define and version service request/response contracts. Keep internal service
  discovery and addresses out of the public API.
- Prefer synchronous HTTP for request/response operations that need an
  immediate result. Use events for decoupled workflow updates where eventual
  consistency is acceptable; document event ownership, delivery, and
  idempotency.
- Apply bounded timeouts and circuit breakers to remote calls. Retry only
  operations that are safe to retry, with bounded backoff; avoid multiplying
  mesh retries with application retries.

## Agent Development Life Cycle

Planner, Researcher, Coder, Reviewer, and Orchestrator have explicit role
definitions, inputs, outputs, domain boundaries, and quality expectations.
Keep those definitions in `agents/<agent>.md` when that structure is adopted.
The existing prompt files in `prompts/` are the current source material to
review and migrate; they are not yet ADLC role contracts.

Each agent and its outputs follow these stages:

1. **Definition**: document mission, responsibilities, constraints, inputs, and
   outputs.
2. **Design**: identify domain ownership, dependencies, interfaces, and failure
   behavior.
3. **Implementation**: use cohesive, testable components and established
   object-oriented design practices.
4. **Testing**: provide unit and integration coverage plus behavior-driven
   scenarios for important user and workflow outcomes.
5. **Review**: use peer and code review for correctness, security, compatibility,
   and operational readiness.
6. **Deployment**: package the service as an immutable image and deploy through
   the managed Kubernetes or OpenShift delivery process.
7. **Evolution**: version and review prompt and workflow changes, then regenerate
   derived artifacts from their maintained sources.

ADLC is the target governance lifecycle; it is distinct from an individual
agent's runtime state machine. The existing five-stage agent state machine
should be retained or deliberately migrated through a versioned API and data
migration, not silently renamed to these seven lifecycle stages.

## Phoenix architecture and operations

### Immutable infrastructure and regeneration

- Build deployable artifacts from reviewed source and pinned build inputs in
  CI. Promote the same image between environments; do not patch running
  containers.
- Keep runtime configuration external to images and credentials in an approved
  secrets manager. Never bake secrets into source, images, or workflow output.
- Treat prompts and workflow definitions as version-controlled inputs. Review
  regeneration changes and validate generated outputs before they are used.
- Make database schema evolution explicit, backward-compatible where practical,
  and independently deployable from application rollouts.

### Statelessness and self-healing

- Keep request-serving instances stateless. Store authoritative data in the
  owning service's database and externalize any shared cache or coordination
  state.
- Run services with readiness, liveness, and startup probes appropriate to
  their startup and dependency behavior. Kubernetes or OpenShift can replace
  unhealthy instances and reschedule workloads.
- Use Istio for service identity, mTLS, traffic policy, timeouts, and circuit
  breaking where the platform supports it. A circuit breaker limits the effect
  of an unhealthy dependency; it does not replace health checks or application
  error handling.
- Scale services independently against measured demand and resource limits.
  Protect stateful databases with their own availability, backup, and recovery
  strategy rather than assuming pod restarts protect data.

### Observability

- Emit structured logs with correlation identifiers and without passwords,
  bearer tokens, or other sensitive values.
- Publish service health and operational metrics. Use a common metrics
  vocabulary and dashboards for request rate, latency, errors, saturation, and
  dependency health.
- Propagate trace context across gateway and service boundaries and record
  distributed traces for important request and workflow paths.
- Define actionable alerts, ownership, retention, and access controls for
  telemetry before production rollout.

The current API already has Actuator health and Prometheus metrics, Logstash
JSON logging, and graceful shutdown. Distributed tracing, centralized
dashboards, platform alerts, gateway policies, and mesh telemetry are target
capabilities and require implementation and operational configuration.

## Deployment and repository evolution

The target repository organization is:

```text
backend/
  task-service/
  user-service/
  workflow-service/
  common-libs/          # Only stable, genuinely shared contracts/utilities
agents/                 # Role definitions and agent contracts
frontend/               # Framework selected during implementation planning
phoenix/
  regeneration.md
  immutable.md
  observability.md
devops/
  ci-cd-pipeline/
  k8s-manifests/
  helm-charts/
docs/
  ARCHITECTURE.md
  ADLC.md
  PHOENIX.md
  CODE_REVIEW_RULES.md
```

This is a proposed target layout, not the current tree. The repository
currently has a root Maven application, `src/`, `prompts/`, and Docker Compose
configuration. Avoid moving or splitting the application until service
boundaries, ownership, API contracts, migration sequencing, and deployment
requirements are agreed. Keep this architecture document at the repository
root unless a deliberate documentation reorganization updates its links.

Recommended evolution sequence:

1. Define domain ownership, public and internal API contracts, identity
   propagation, data ownership, frontend choice, and operational requirements.
2. Add role definitions, review rules, and contract and behavior tests while
   preserving the working monolith.
3. Extract one domain at a time behind stable APIs; migrate its data with
   explicit ownership and rollback planning.
4. Introduce the gateway and service-to-service policies, then deploy to
   Kubernetes or OpenShift with probes, resource limits, secrets, and telemetry.
5. Validate failure recovery, authorization isolation, rate limits, deployment
   rollback, backup restoration, and end-to-end user workflows before retiring
   old paths.

## Quality and security expectations

- Require unit and integration tests and behavior-driven scenarios for
  critical workflows. Target at least 80% coverage, with exclusions and
  per-service reporting made explicit; coverage does not replace meaningful
  assertions.
- Enforce Java naming conventions, camelCase identifiers, and PascalCase
  class names. Prefer cohesive design and avoid duplicating domain policy in a
  shared library.
- Use Conventional Commit prefixes (`feat:`, `fix:`, `docs:`, and related
  types) for changes.
- Run formatting or lint checks, tests, dependency vulnerability scanning, and
  secret detection in CI. Fail the pipeline on configured blocking findings.
- Apply least privilege, validate inputs, protect credentials, and test
  cross-user access controls at every service boundary.
- Keep database credentials, JWT signing keys, and deployment tokens in managed
  secrets; never commit them or print them in logs.

These are target quality gates. The current build and CI configuration should
be assessed and updated as the architecture is implemented; this document does
not assert that the 80% threshold, BDD suite, frontend, or security scanners are
already enforced.
