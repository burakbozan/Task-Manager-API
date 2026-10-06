# Task Manager API

A Spring Boot REST API for creating, listing, and deleting tasks. It uses Java 17,
Maven, Spring Web, Spring Data JPA, Lombok, and an in-memory H2 database.

## Run

With Java 17 and Maven installed, start the application from the project root:

```sh
mvn spring-boot:run
```

The API is available at `http://localhost:8080/api/tasks`. The H2 database is
in-memory, so task data is cleared when the application stops.

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/tasks` | Return all tasks |
| `POST` | `/api/tasks` | Create a task |
| `DELETE` | `/api/tasks/{id}` | Delete a task (returns `204 No Content`) |

Example request body for `POST /api/tasks`:

```json
{
  "title": "Prepare project",
  "description": "Set up the Task Manager API",
  "completed": false
}
```
