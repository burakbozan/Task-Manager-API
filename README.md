# Task Manager API

A Spring Boot REST API for managing tasks, with JWT-based authentication. It uses
Java 17, Maven, Spring Web, Spring Data JPA, Spring Security, Lombok, and an
in-memory H2 database.

## Run

With Java 17 and Maven installed, start the application from the project root:

```sh
mvn spring-boot:run
```

The API is available at `http://localhost:8080`. The H2 database is in-memory,
so user and task data is cleared when the application stops.

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
