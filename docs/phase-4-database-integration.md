# Phase 4 — Database Integration with JPA & PostgreSQL

> Goal: make problems actually persist. Replace the in-memory store with a real database via **Spring Data JPA**, running on **H2** out of the box and on **PostgreSQL** through a profile.

This is where the repository abstraction from Phase 3 earns its keep. Because the service was written against the `ProblemRepository` interface, switching from a hand-written `HashMap` to a database-backed repository barely touches the business logic.

---

## 1. The concepts (from scratch)

### What is JPA, Hibernate, and Spring Data?

Three layers, often confused:

- **JPA (Jakarta Persistence API)** — a *specification*. It defines annotations like `@Entity`, `@Id`, `@Column` and an API (`EntityManager`) for mapping Java objects to database rows (**ORM** = Object-Relational Mapping). JPA itself is just interfaces.
- **Hibernate** — the most common *implementation* of JPA. It does the real work: generating SQL, managing a "persistence context", tracking changes, and translating between objects and tables. Spring Boot uses Hibernate by default.
- **Spring Data JPA** — a layer *on top* of JPA/Hibernate that removes boilerplate. You declare a repository **interface**; Spring Data generates the implementation at runtime, including queries derived from method names.

So: you write annotations (JPA) + an interface (Spring Data); Hibernate executes the SQL.

### ORM in one sentence

Instead of writing `INSERT INTO problems ...` and mapping `ResultSet` columns by hand, you save a `Problem` object and the ORM figures out the SQL.

---

## 2. What changed in Phase 4

| Concern | Phase 3 | Phase 4 |
|--------|---------|---------|
| Storage | `ConcurrentHashMap` in `InMemoryProblemRepository` | A real database (H2 / PostgreSQL) |
| Repository | Custom interface + hand-written impl | `interface ProblemRepository extends JpaRepository<Problem, Long>` (no impl!) |
| `Problem` | Plain POJO | JPA `@Entity` mapped to the `problems` table |
| Id generation | `AtomicLong` in the repo | Database identity column (`@GeneratedValue`) |
| Seeding | `@PostConstruct` in the service | `DataSeeder` (`CommandLineRunner`) |
| Service | — | Unchanged except `delete` (Spring Data's `deleteById` is `void`) |

The service's `findAll`, `findById`, `save`, `create`, `update`, filtering, search, and stats are otherwise identical. That stability is the payoff of depending on an interface.

---

## 3. Walkthrough

### 3.1 `pom.xml` — the H2 driver

`spring-boot-starter-data-jpa` (already present since Phase 1) brings in Hibernate and Spring Data JPA. We add the **H2** database (`runtime` scope) for local dev; the PostgreSQL driver was already there.

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3.2 `model/Problem.java` — now an `@Entity`

```java
@Entity
@Table(name = "problems")
public class Problem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_tags", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "tag")
    private List<String> tags;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
```

Key mapping decisions:

- **`@GeneratedValue(IDENTITY)`** — the database assigns the primary key (auto-increment in H2, identity/serial in PostgreSQL). `save()` of a new entity returns it with the id filled in.
- **`@Enumerated(EnumType.STRING)`** — store `"EASY"`, not the ordinal `0`. Storing ordinals is a classic bug source: reorder the enum and every existing row's meaning shifts.
- **`@ElementCollection`** — `tags` is a list of plain strings, not its own entity, so Hibernate stores them in a child table `problem_tags(problem_id, tag)`. `EAGER` because tags are tiny and always displayed with the problem.
- **`@Column(updatable = false)`** on `createdAt` — set once at insert, never changed by updates.
- We deliberately use Lombok **`@Getter`/`@Setter`** instead of `@Data` here, because `@Data`'s generated `equals`/`hashCode` over all fields is an anti-pattern for entities (identity should be the id; relationship fields can cause lazy-loading surprises).

### 3.3 `repository/ProblemRepository.java` — an interface, no implementation

```java
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByDifficulty(Difficulty difficulty);
}
```

Extending `JpaRepository<Problem, Long>` gives us `findAll`, `findById`, `save`, `deleteById`, `existsById`, `count`, paging and sorting — implemented by Spring Data at runtime. The hand-written `InMemoryProblemRepository` is **deleted**.

`findByDifficulty` is a **derived query method**: Spring Data parses the method name (`findBy` + `Difficulty`) and generates `SELECT ... WHERE difficulty = ?`. No SQL, no `@Query`.

### 3.4 `service/ProblemService.java` — almost unchanged

The only real change is `delete`, because Spring Data's `deleteById` returns `void` (and throws if the id is absent), whereas the old in-memory version returned a boolean:

```java
public boolean delete(Long id) {
    if (!problemRepository.existsById(id)) return false;
    problemRepository.deleteById(id);
    return true;
}
```

`findProblems` now uses the derived `findByDifficulty` query as its base set when a difficulty filter is supplied (pushing that filter to the database), then applies the text search in memory.

### 3.5 `config/DataSeeder.java` — seeding the right way

```java
@Component
public class DataSeeder implements CommandLineRunner {
    public void run(String... args) {
        if (problemRepository.count() > 0) return;   // idempotent
        problemService.create(/* Two Sum */);
        problemService.create(/* Longest Substring */);
    }
}
```

A `CommandLineRunner` runs **after** the context and database are ready — the correct lifecycle point to write data. The `count() > 0` guard means a persistent PostgreSQL DB won't accumulate duplicate seed rows on every restart.

### 3.6 Configuration

`application.properties` (default → H2 in-memory):

```properties
spring.datasource.url=jdbc:h2:mem:codearena;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.h2.console.enabled=true
```

`application-postgres.properties` (activated with `--spring.profiles.active=postgres`) overrides the datasource URL/credentials and sets the PostgreSQL dialect. **Profiles** let one build target multiple environments by swapping property files, not code.

Two settings worth understanding:

- **`ddl-auto=update`** — Hibernate creates/updates tables from the entity mappings on startup. Great for dev; in real production you'd use migrations (Flyway/Liquibase) and `validate` instead, because `update` never drops or safely alters columns.
- **`open-in-view=false`** — turns off "Open Session In View". OSIV keeps the persistence context open for the whole HTTP request so lazy collections can load in the view layer; disabling it (recommended) keeps DB work inside the service and surfaces lazy-loading mistakes early.

`SecurityConfig` was given `frameOptions(sameOrigin)` so the H2 console (which renders inside frames) isn't blocked.

---

## 4. Running it

**H2 (default — zero setup):**

```bash
./mvnw spring-boot:run
curl http://localhost:8080/api/problems     # the two seeded problems
```

Open the H2 console at <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:codearena`, user `sa`, no password) to see the `PROBLEMS` and `PROBLEM_TAGS` tables.

**PostgreSQL:**

```bash
createdb codearena    # once
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

> H2 is in-memory, so its data resets on restart. PostgreSQL persists across restarts (and the seeder won't re-seed thanks to the count guard).

---

## 5. Testing notes

- **`ProblemServiceTest`** is still a *pure unit test* — but since the in-memory repository is gone, it now **mocks** `ProblemRepository` with Mockito, backing `save`/`findById`/etc. with a small `Map`. This keeps it fast and isolates the service logic from any database.
- **`ProblemControllerTest`** (`@SpringBootTest`) now exercises the *real* JPA stack on H2: the context boots Hibernate, `DataSeeder` runs, and the MockMvc requests hit actual database reads/writes. The seeded-data assertions confirm the `CommandLineRunner` executed.

```bash
./mvnw test    # 22 tests, all green on H2
```

---

## 6. Interview questions

**JPA / ORM fundamentals**

1. Distinguish JPA, Hibernate, and Spring Data JPA. Which is a spec, which is an implementation, which is a convenience layer?
2. What is ORM and what problem does it solve? What's the "object-relational impedance mismatch"?
3. What does `@Entity` require at minimum? Why must an entity have a no-arg constructor?
4. `@GeneratedValue` strategies: `IDENTITY` vs `SEQUENCE` vs `AUTO` vs `TABLE` — trade-offs?
5. Why store an enum with `EnumType.STRING` rather than `EnumType.ORDINAL`?
6. What is the persistence context / first-level cache? What does it mean for an entity to be "managed"?

**Spring Data**

7. How can `ProblemRepository` work with no implementation class? Who provides it?
8. What is a derived query method? How does Spring Data turn `findByDifficulty` into SQL?
9. What does extending `JpaRepository` give you over `CrudRepository` or `PagingAndSortingRepository`?
10. Why did `delete` need to change when moving from the in-memory repo to Spring Data?

**Configuration & operations**

11. What does `spring.jpa.hibernate.ddl-auto` control? Why is `update` risky in production, and what's the alternative?
12. What is "Open Session In View" and why is disabling it often recommended?
13. How do Spring profiles let the same build run on H2 and PostgreSQL? How are profile-specific properties resolved/overridden?
14. Where should you seed data, and why is `CommandLineRunner` better than the service's `@PostConstruct` here? What makes the seeding idempotent?

**Modelling & testing**

15. Why is Lombok's `@Data` discouraged on JPA entities? What should `equals`/`hashCode` be based on?
16. How are the `tags` stored in the database, and what is `@ElementCollection` vs mapping a separate `@Entity` with `@OneToMany`?
17. What is the N+1 select problem, and how do `FetchType.EAGER`/`LAZY` and join fetches relate to it?
18. Contrast the unit test (mocked repository) with the integration test (real H2). When would you also write a `@DataJpaTest`?

**Stretch / follow-ups**

19. The text search still filters in memory after loading rows. How would you push it into the database (derived query, `@Query`, JPQL, or Criteria API)? What about searching inside the `@ElementCollection` tags?
20. How would you add pagination (`Pageable`) to the listing endpoint, and what does the repository return then?
21. Why should writes be wrapped in a transaction? What does `@Transactional` do, and what is the default propagation?
