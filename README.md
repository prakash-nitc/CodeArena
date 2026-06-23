# CodeArena

A competitive coding platform built with **Spring Boot** — designed as a hands-on project for learning Java and Spring Boot from scratch.

> 📚 **Preparing for an interview?** Read the **[Complete Study & Interview Guide](docs/README.md)** — a single doc that explains the whole project and every concept from scratch, with cheat sheets and a question bank with model answers.

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
- [x] **Phase 7** — Problem & Submission Management (+ leaderboard) — [docs](docs/phase-7-submission-management.md)
- [ ] **Phase 8** — Code Execution Engine Integration — *future work* (needs a sandbox, e.g. Docker)
- [x] **Phase 9** — Leaderboards — delivered as part of Phase 7; full *contests* intentionally out of scope (see note below)

> **Note on Phases 8 & 9.** Judging real, untrusted code safely needs a sandbox (Docker/containers), so Phase 8 is left as documented future work; submissions are stored and "judged" by an admin endpoint in the meantime. The **leaderboard** from Phase 9 ships in Phase 7 (ranking by accepted submissions); full **contests** (time-windowed, auto-scored) were dropped because their scoring depends on a real judge.

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
│   │   │   ├── controller/    # Problem, Auth, Submission, Leaderboard, Health
│   │   │   ├── dto/           # request/response DTOs, ApiError, LeaderboardEntry, ...
│   │   │   ├── exception/     # Domain exceptions + GlobalExceptionHandler
│   │   │   ├── model/         # Problem, User, Submission (@Entity), enums
│   │   │   ├── repository/    # Problem, User, Submission repositories
│   │   │   ├── security/      # JwtService, JwtAuthenticationFilter, UserDetailsService, ...
│   │   │   └── service/       # ProblemService, AuthService, SubmissionService
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
| `POST` | `/api/problems/{id}/submissions` | authenticated | Submit a solution |
| `GET` | `/api/problems/{id}/submissions` | authenticated | List submissions (own; admin sees all) |
| `GET` | `/api/submissions/me` | authenticated | Your submissions |
| `GET` | `/api/submissions/{id}` | owner / **ADMIN** | Get a submission |
| `PUT` | `/api/submissions/{id}/status` | **ADMIN** | Judge a submission (ACCEPTED/REJECTED) |
| `GET` | `/api/leaderboard` | public | Users ranked by problems solved |

Authenticate by sending the token from login/register as `Authorization: Bearer <token>`. Seeded dev accounts: `admin`/`admin123` (ADMIN) and `user`/`user123` (USER).

All errors return a consistent JSON body (`status`, `message`, `path`, and `fieldErrors` for validation failures): `400` for invalid input, `404` for unknown ids, `409` for a duplicate title.

See the per-phase docs in [docs/](docs/) for full walkthroughs.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is for educational purposes.
