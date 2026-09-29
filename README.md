# Task Manager API

A Spring Boot–based REST API for managing tasks, categories, and deadlines.  
Designed as a microservice-friendly project with clean architecture and room for extensions.

## 🚀 Features
- User registration & JWT authentication
- CRUD operations for tasks
- Task categories & deadlines
- RESTful API design
- Ready for Docker deployment

## 🛠️ Tech Stack
- Java 17
- Spring Boot
- Spring Security (JWT)
- H2/PostgreSQL (configurable)
- Maven

## 📂 Project Structure
/backend
├── src
├── pom.xml
/docs
├── ARCHITECTURE.md
/frontend (optional later)


## ▶️ Getting Started
1. Clone the repo
2. Run `mvn spring-boot:run`
3. Access API at `http://localhost:8080/api/tasks`

## 📌 Roadmap
- [ ] Add Swagger/OpenAPI docs
- [ ] Implement role-based access
- [ ] Add Docker & CI/CD pipeline
- [ ] Extend with microservices (notifications, reporting)

