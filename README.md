# Task Manager API

A Spring Boot REST API for managing tasks, with JWT-based authentication. It uses
Java 17, Maven, Spring Web, Spring Data JPA, Spring Security, Lombok, and H2 for
local development. Docker Compose runs the application with PostgreSQL.

## Run locally

With Java 17 and Maven installed, start the application from the project root:

```sh
mvn spring-boot:run
```

The API is available at `http://localhost:8080`. By default it uses an
in-memory H2 database, so user and task data is cleared when the application
stops.
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
| `GET` | `/api/tasks` | Return all tasks (authenticated) |
| `POST` | `/api/tasks` | Create a task (authenticated) |
| `DELETE` | `/api/tasks/{id}` | Delete a task (authenticated; returns `204 No Content`) |

Register or log in with a JSON body:

```json
{
  "username": "alice",
  "password": "a-secure-password"
}
```

Send the returned token on task requests using
`Authorization: Bearer <token>`. Set `JWT_SECRET` to a Base64-encoded key of at
least 256 bits to use a stable signing key across restarts; when it is unset, a
new random key is generated at startup and existing tokens expire when the
application restarts. Token lifetime defaults to 24 hours and can be configured
with `JWT_EXPIRATION_MS`.
