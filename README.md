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
- [x] **Phase 3** — Service Layer & Business Logic — [docs](docs/phase-3-service-layer.md)
- [x] **Phase 4** — Database Integration with JPA & PostgreSQL — [docs](docs/phase-4-database-integration.md)
- [ ] **Phase 5** — Input Validation & Error Handling
- [ ] **Phase 6** — Authentication & Authorization (JWT)
- [ ] **Phase 7** — Problem & Submission Management
- [ ] **Phase 8** — Code Execution Engine Integration
- [ ] **Phase 9** — Leaderboards & Contests

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+ (or use the bundled `./mvnw` wrapper)
- PostgreSQL 14+ — **optional**; the app runs on an in-memory H2 database by default

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

The application will start on `http://localhost:8080` using an **in-memory H2 database** (no setup required). Browse it at <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:codearena`, user `sa`, empty password).

### Configuration

Settings live in `src/main/resources/application.properties` (H2 defaults). To run against **PostgreSQL** instead, create the database and activate the `postgres` profile:

```bash
createdb codearena
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

PostgreSQL connection details are in `application-postgres.properties`. See [docs/phase-4-database-integration.md](docs/phase-4-database-integration.md) for details.

## Project Structure

```
codearena/
├── src/
│   ├── main/
│   │   ├── java/com/codearena/codearena/
│   │   │   ├── CodearenaApplication.java
│   │   │   ├── config/        # SecurityConfig, DataSeeder
│   │   │   ├── controller/    # ProblemController, HealthController
│   │   │   ├── dto/           # ProblemRequest, ProblemResponse, ProblemStatsResponse
│   │   │   ├── model/         # Problem (@Entity), Difficulty
│   │   │   ├── repository/    # ProblemRepository (Spring Data JPA)
│   │   │   └── service/       # ProblemService (business logic)
│   │   └── resources/
│   │       ├── application.properties           # H2 (default)
│   │       └── application-postgres.properties  # PostgreSQL profile
│   └── test/
├── docs/                      # Per-phase documentation
├── pom.xml
└── README.md
```

## API Endpoints (Phase 2)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/ping` | Liveness check |
| `GET` | `/api/problems` | List problems (optional `?difficulty=` and `?search=` filters) |
| `GET` | `/api/problems/stats` | Catalogue statistics (total + per-difficulty counts) |
| `GET` | `/api/problems/{id}` | Get a problem by id |
| `POST` | `/api/problems` | Create a problem |
| `PUT` | `/api/problems/{id}` | Update a problem |
| `DELETE` | `/api/problems/{id}` | Delete a problem |

See the per-phase docs in [docs/](docs/) for full walkthroughs.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is for educational purposes.
