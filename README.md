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
- [x] **Phase 5** — Input Validation & Error Handling — [docs](docs/phase-5-validation-error-handling.md)
- [x] **Phase 6** — Authentication & Authorization (JWT) — [docs](docs/phase-6-authentication-jwt.md)
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
│   │   │   ├── config/        # SecurityConfig, SecurityBeansConfig, DataSeeder
│   │   │   ├── controller/    # ProblemController, AuthController, HealthController
│   │   │   ├── dto/           # ProblemRequest, AuthResponse, ApiError, ...
│   │   │   ├── exception/     # Domain exceptions + GlobalExceptionHandler
│   │   │   ├── model/         # Problem, User (@Entity), Difficulty, Role
│   │   │   ├── repository/    # ProblemRepository, UserRepository
│   │   │   ├── security/      # JwtService, JwtAuthenticationFilter, UserDetailsService, ...
│   │   │   └── service/       # ProblemService, AuthService
│   │   └── resources/
│   │       ├── application.properties           # H2 (default)
│   │       └── application-postgres.properties  # PostgreSQL profile
│   └── test/
├── docs/                      # Per-phase documentation
├── pom.xml
└── README.md
```

## API Endpoints (Phase 2)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | public | Create an account, returns a JWT |
| `POST` | `/api/auth/login` | public | Log in, returns a JWT |
| `GET` | `/api/ping` | public | Liveness check |
| `GET` | `/api/problems` | public | List problems (optional `?difficulty=` and `?search=` filters) |
| `GET` | `/api/problems/stats` | public | Catalogue statistics (total + per-difficulty counts) |
| `GET` | `/api/problems/{id}` | public | Get a problem by id |
| `POST` | `/api/problems` | authenticated | Create a problem |
| `PUT` | `/api/problems/{id}` | authenticated | Update a problem |
| `DELETE` | `/api/problems/{id}` | **ADMIN** | Delete a problem |

Authenticate by sending the token from login/register as `Authorization: Bearer <token>`. Seeded dev accounts: `admin`/`admin123` (ADMIN) and `user`/`user123` (USER).

All errors return a consistent JSON body (`status`, `message`, `path`, and `fieldErrors` for validation failures): `400` for invalid input, `404` for unknown ids, `409` for a duplicate title.

See the per-phase docs in [docs/](docs/) for full walkthroughs.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is for educational purposes.
