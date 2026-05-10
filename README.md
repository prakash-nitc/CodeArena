# CodeArena

A competitive coding platform built with **Spring Boot** — designed as a hands-on project for learning Java and Spring Boot from scratch.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT |
| Persistence | Spring Data JPA |
| Database | PostgreSQL |
| Validation | Bean Validation (Jakarta) |
| Utilities | Lombok |
| Build | Maven |

## Roadmap

The project follows a phased learning approach:

- [x] **Phase 1** — Project Setup & Hello World — [docs](docs/phase-1-project-setup.md)
- [x] **Phase 2** — REST API Basics (Controllers, DTOs, Endpoints) — [docs](docs/phase-2-rest-api-basics.md)
- [ ] **Phase 3** — Service Layer & Business Logic
- [ ] **Phase 4** — Database Integration with JPA & PostgreSQL
- [ ] **Phase 5** — Input Validation & Error Handling
- [ ] **Phase 6** — Authentication & Authorization (JWT)
- [ ] **Phase 7** — Problem & Submission Management
- [ ] **Phase 8** — Code Execution Engine Integration
- [ ] **Phase 9** — Leaderboards & Contests

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Run Locally

```bash
# Clone the repository
git clone https://github.com/prakash-nitc/CodeArena.git
cd CodeArena/codearena

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

### Configuration

Database and application settings are in `src/main/resources/application.properties`. Update the PostgreSQL connection details to match your local setup before running.

## Project Structure

```
codearena/
├── src/
│   ├── main/
│   │   ├── java/com/codearena/codearena/
│   │   │   ├── CodearenaApplication.java
│   │   │   ├── config/        # SecurityConfig (temporary permit-all)
│   │   │   ├── controller/    # ProblemController, HealthController
│   │   │   ├── dto/           # ProblemRequest, ProblemResponse
│   │   │   ├── model/         # Problem, Difficulty
│   │   │   └── service/       # ProblemService (in-memory store)
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── docs/                      # Per-phase documentation
├── pom.xml
└── README.md
```

## API Endpoints (Phase 2)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/ping` | Liveness check |
| `GET` | `/api/problems` | List all problems |
| `GET` | `/api/problems/{id}` | Get a problem by id |
| `POST` | `/api/problems` | Create a problem |
| `PUT` | `/api/problems/{id}` | Update a problem |
| `DELETE` | `/api/problems/{id}` | Delete a problem |

See [docs/phase-2-rest-api-basics.md](docs/phase-2-rest-api-basics.md) for a full walkthrough.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is for educational purposes.
