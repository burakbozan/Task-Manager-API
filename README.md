# Task Manager API

A Spring Boot REST API for managing tasks, with JWT-based authentication. It uses
Java 25, Maven, Spring Web, Spring Data JPA, Spring Security, Lombok, and H2 for
local development. Docker Compose runs the application with PostgreSQL.

## Run locally

With Java 25 and Maven installed, start the application from the project root:

```sh
mvn spring-boot:run
```

The API is available at `http://localhost:8080`. By default it uses an
in-memory H2 database, so user and task data is cleared when the application
stops. Agent data is cleared as well.
Interactive API documentation is available at
`http://localhost:8080/swagger-ui.html`; use its **Authorize** button to enter
the JWT returned by the authentication endpoints.

## Monitoring and logs

Spring Boot Actuator exposes `/actuator/health` without authentication.
`/actuator/info`, `/actuator/metrics`, and `/actuator/prometheus` require a
valid Bearer JWT. Prometheus can scrape `/actuator/prometheus` with the same
authentication. Logs are emitted as JSON to standard output for collection by
Logstash or another ELK-compatible log shipper. Enable the `dev` profile for
detailed application, web, and SQL logs; use the `prod` profile for concise
application logs.

## Run with Docker Compose

Set the JWT signing key and, optionally, database credentials before starting
the PostgreSQL and API containers:

```sh
$env:JWT_SECRET = "<Base64-encoded key of at least 256 bits>"
$env:DB_USERNAME = "taskmanager"
$env:DB_PASSWORD = "<database password>"
docker compose up --build
```

Compose defaults the local development database credentials to `taskmanager`;
override them for any shared or production deployment. Other configurable
variables are `DB_NAME` (default `taskmanager`) and `JWT_EXPIRATION_MS`
(default `86400000`).

## CI and image publishing

The workflow in `.github/workflows/ci.yml` runs `mvn clean install` against
PostgreSQL on pushes and pull requests to `main`. Pushes to `main` also build
and publish `ghcr.io/<owner>/<repository>`; pull requests build the image
without publishing it.

The workflow uses `GITHUB_TOKEN` for GHCR by default. If desired, configure
`GHCR_TOKEN` (a token with package write access) and `GHCR_USERNAME` as
repository secrets. PostgreSQL CI credentials can be overridden with the
`DB_USERNAME` and `DB_PASSWORD` secrets; CI-only fallback values are used when
they are absent. `JWT_SECRET` can also be configured as a repository secret.

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a user and receive a JWT |
| `POST` | `/api/auth/login` | Authenticate and receive a JWT |
| `GET` | `/api/tasks` | Return the authenticated user's tasks |
| `GET` | `/api/tasks/{id}` | Get an owned task |
| `POST` | `/api/tasks` | Create a task (`201 Created`) |
| `PUT` | `/api/tasks/{id}` | Update an owned task |
| `DELETE` | `/api/tasks/{id}` | Delete an owned task (`204 No Content`) |
| `GET` | `/api/agents` | List the authenticated user's agents |
| `POST` | `/api/agents` | Create an agent in the `DRAFT` stage |
| `GET` | `/api/agents/{id}` | Get an owned agent |
| `PUT` | `/api/agents/{id}` | Update an owned agent's name and description |
| `POST` | `/api/agents/{id}/lifecycle` | Advance an agent by one lifecycle stage |
| `DELETE` | `/api/agents/{id}` | Delete an owned agent |

Register or log in with a JSON body:

```json
{
  "username": "alice",
  "password": "a-secure-password"
}
```

Usernames and passwords are required. Passwords are limited to 72 UTF-8 bytes
to match BCrypt's input limit; longer inputs are rejected instead of being
silently truncated.

Send the returned token on task requests using

`Authorization: Bearer <token>`. Set `JWT_SECRET` to a Base64-encoded key of at
least 256 bits to use a stable signing key across restarts; when it is unset, a
new random key is generated at startup and existing tokens expire when the
application restarts. Token lifetime defaults to 24 hours and can be configured
with `JWT_EXPIRATION_MS`.

Agent lifecycle stages are `DRAFT`, `DEVELOPMENT`, `TESTING`, `DEPLOYED`, and
`RETIRED`. Transitions advance one stage at a time; invalid transitions return
`409 Conflict`. For example, create an agent with:

```json
{
  "name": "Research assistant",
  "description": "Summarizes project research"
}
```

Advance it by sending `{"stage":"DEVELOPMENT"}` to the lifecycle endpoint.
Each user's agent records are isolated by ownership.

Tasks are also scoped to their creating user. Task creation accepts `title`,
optional `description`, and optional `completed`; task responses do not accept
or expose ownership identifiers. Existing task records created before
ownership was added have no owner and are intentionally not exposed to any
account; migrate or remove those records before upgrading a populated database.

Transient persistence failures for task and agent writes are retried up to
three times with exponential backoff. Configure this Phoenix resilience
behavior using `DB_RETRY_MAX_ATTEMPTS` and `DB_RETRY_INITIAL_DELAY_MS`.
`/actuator/health/liveness` and `/actuator/health/readiness` are available for
orchestrator probes, with readiness including database health. Graceful
shutdown allows 20 seconds for in-flight work.
See [ARCHITECTURE.md](ARCHITECTURE.md) for component and runtime details.
