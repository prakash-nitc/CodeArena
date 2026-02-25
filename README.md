# CodeArena

A competitive coding platform built with **Spring Boot**.

## Tech Stack

- **Java 17**
- **Spring Boot 3.5**
- **Spring Security** – Authentication & authorization with JWT
- **Spring Data JPA** – Database persistence
- **PostgreSQL** – Relational database
- **Lombok** – Boilerplate reduction
- **Bean Validation** – Request validation
- **Maven** – Build & dependency management

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL

### Run Locally

```bash
# Clone the repository
git clone <repo-url>
cd codearena

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## Project Structure

```
src/
├── main/
│   ├── java/com/codearena/codearena/
│   └── resources/
└── test/
```

## License

This project is for educational purposes.
