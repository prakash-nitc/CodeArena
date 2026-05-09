# Phase 2 — REST API Basics (Controllers, DTOs, Endpoints)

> Goal: expose the first real HTTP API — a full set of CRUD endpoints for **coding problems** — and learn how Spring MVC turns HTTP requests into Java method calls and Java objects into JSON.

This phase introduces no database yet (that's Phase 4) and no real authentication (that's Phase 6). Everything is held in memory so we can focus purely on REST mechanics. This document explains every class added, the reasoning behind the structure, how to try it, and interview questions.

---

## 1. The big picture: how a request flows

```
HTTP request
   │
   ▼
DispatcherServlet          (Spring's front controller — receives every request)
   │  routes by URL + HTTP method
   ▼
ProblemController          (@RestController — translates HTTP ⇆ Java)
   │  delegates business logic
   ▼
ProblemService             (@Service — holds the in-memory store, the "brain")
   │
   ▼
ConcurrentHashMap<Long, Problem>   (our temporary "database")
```

On the way **in**, Jackson (JSON library) deserializes the request body into a `ProblemRequest` object. On the way **out**, it serializes the returned `ProblemResponse` (or list) into JSON. We never touch JSON by hand.

---

## 2. The layers and why they're separated

A common beginner mistake is to put everything in the controller. We deliberately split responsibilities:

| Layer | Class(es) | Responsibility |
|-------|-----------|----------------|
| **Model** | `Problem`, `Difficulty` | The core domain concept. |
| **DTO** | `ProblemRequest`, `ProblemResponse` | The *contract* with the outside world — what clients send and receive. |
| **Service** | `ProblemService` | Business logic + data storage. |
| **Controller** | `ProblemController`, `HealthController` | HTTP plumbing only. |
| **Config** | `SecurityConfig` | Cross-cutting setup (temporary security). |

The key principle: **keep controllers thin**. They translate between HTTP and Java and delegate everything else. This makes logic testable without HTTP and keeps each class focused.

---

## 3. Walkthrough of every class

### 3.1 `model/Difficulty.java` — an enum

```java
public enum Difficulty { EASY, MEDIUM, HARD }
```

An **enum** fixes the set of legal values at compile time. A field of type `Difficulty` can only ever be one of these three constants — no typos, no invalid states. Jackson serializes enums to/from their names (`"EASY"`).

### 3.2 `model/Problem.java` — the domain model

A plain Java object (POJO) with fields `id`, `title`, `description`, `difficulty`, `tags`, `createdAt`. It uses Lombok so we don't hand-write boilerplate:

- `@Data` → getters, setters, `equals`, `hashCode`, `toString`.
- `@Builder` → a fluent builder: `Problem.builder().title("...").build()`.
- `@NoArgsConstructor` / `@AllArgsConstructor` → the two constructors.

It is **not** a JPA `@Entity` yet — persistence comes in Phase 4. Right now it's just an in-memory record.

### 3.3 DTOs: `dto/ProblemRequest.java` and `dto/ProblemResponse.java`

A **DTO (Data Transfer Object)** defines exactly what crosses the API boundary.

- `ProblemRequest` is what a client may **send**. Notice it has *no* `id` and *no* `createdAt` — those are server-controlled. If we accepted the `Problem` model directly, a client could try to set the id or overwrite the creation time. Separating the request type prevents that ("mass-assignment" protection).
- `ProblemResponse` is what we **return**. Keeping it separate from the model means we can change internal storage without breaking the public JSON contract, and we choose precisely which fields are exposed.

> Rule of thumb: never expose your persistence/domain entities directly through the API. Map to DTOs.

### 3.4 `service/ProblemService.java` — the business layer

```java
@Service
public class ProblemService {
    private final Map<Long, Problem> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);
    ...
}
```

- `@Service` registers it as a Spring-managed **singleton bean** so it can be injected into the controller.
- Because a single instance is shared across all concurrent HTTP threads, the store uses thread-safe types: `ConcurrentHashMap` for storage and `AtomicLong` for generating ids (`incrementAndGet()` is atomic — no two requests get the same id).
- CRUD methods (`findAll`, `findById`, `create`, `update`, `delete`) return DTOs. `findById`/`update` return `Optional<...>` so the controller can cleanly turn "not found" into a 404 without using `null`.
- `@PostConstruct seedSampleData()` runs once after construction to insert two example problems, so the API returns data immediately.

When Phase 4 arrives, the `ConcurrentHashMap` is replaced by a Spring Data JPA repository — and the controller doesn't change at all. That's the payoff of the service layer.

### 3.5 `controller/ProblemController.java` — the REST endpoints

```java
@RestController
@RequestMapping("/api/problems")
public class ProblemController { ... }
```

Annotations doing the work:

- `@RestController` = `@Controller` + `@ResponseBody`. The `@ResponseBody` part means each method's return value is written **directly to the HTTP response body as JSON**, rather than interpreted as a view/template name.
- `@RequestMapping("/api/problems")` sets the common base path.
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` map HTTP verbs to methods.
- `@PathVariable Long id` binds the `{id}` in the URL to the method parameter.
- `@RequestBody ProblemRequest request` tells Spring to deserialize the JSON body into a `ProblemRequest`.

The endpoints:

| Method & path | Action | Success status | Missing → |
|---------------|--------|----------------|-----------|
| `GET /api/problems` | list all | 200 | — |
| `GET /api/problems/{id}` | fetch one | 200 | 404 |
| `POST /api/problems` | create | 201 + `Location` header | — |
| `PUT /api/problems/{id}` | replace | 200 | 404 |
| `DELETE /api/problems/{id}` | delete | 204 | 404 |

**Why `ResponseEntity`?** When a method just returns an object, Spring sends `200 OK`. For creation we want `201 Created` plus a `Location` header, and for delete we want `204 No Content`; `ResponseEntity` gives us explicit control over status code, headers, and body. The `Location` header (built with `ServletUriComponentsBuilder`) points at the URL of the newly created resource — a REST convention.

**Constructor injection.** The controller receives its `ProblemService` through its constructor rather than a `@Autowired` field. This makes the dependency explicit, allows the field to be `final`, and makes the class trivial to unit-test by passing a mock.

### 3.6 `controller/HealthController.java` — the "hello world" endpoint

`GET /api/ping` returns a small JSON object (`{status, service, timestamp}`). It's the simplest possible handler — returning a `Map` that Jackson serializes — and doubles as a liveness smoke test.

### 3.7 `config/SecurityConfig.java` — temporary permit-all

Because `spring-boot-starter-security` is on the classpath, Spring Security would otherwise lock down **every** endpoint behind an HTTP Basic login. This config defines a `SecurityFilterChain` bean that permits all requests (and disables CSRF, which only matters for cookie-based browser sessions) so we can exercise the REST API freely. **This is replaced by real JWT authentication in Phase 6.**

---

## 4. Trying it out

Start the app:

```bash
cd codearena
./mvnw spring-boot:run
```

Then:

```bash
# Liveness check
curl http://localhost:8080/api/ping

# List the seeded problems
curl http://localhost:8080/api/problems

# Create a problem
curl -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" \
  -d '{"title":"Valid Parentheses","description":"Check bracket validity.","difficulty":"EASY","tags":["stack","string"]}'

# Fetch it (use the id returned above)
curl http://localhost:8080/api/problems/3

# Update it
curl -X PUT http://localhost:8080/api/problems/3 \
  -H "Content-Type: application/json" \
  -d '{"title":"Valid Parentheses II","description":"Updated.","difficulty":"MEDIUM","tags":["stack"]}'

# Delete it
curl -X DELETE http://localhost:8080/api/problems/3 -i
```

> Because storage is in memory, everything resets when the app restarts.

---

## 5. The tests

`ProblemControllerTest` uses `@SpringBootTest` + `@AutoConfigureMockMvc`. `MockMvc` exercises the controllers through the full Spring MVC stack **without starting a real network server**, which is fast and reliable. The tests cover: ping, listing, create→retrieve (asserting 201 + `Location`), 404 on missing get/update, update replacing fields, and delete returning 204 then 404 on a second delete.

Run them:

```bash
./mvnw test
```

---

## 6. Design decisions & trade-offs

- **In-memory store** keeps Phase 2 focused on REST, not databases. Trade-off: no persistence across restarts — fixed in Phase 4.
- **`PUT` does a full replace.** A partial update would be `PATCH`. We chose `PUT` for simplicity; the whole resource is sent each time.
- **No validation yet.** Sending an empty title currently succeeds. Bean Validation (`@NotBlank`, etc.) and a global error handler arrive in Phase 5.
- **Permit-all security** is intentionally insecure and temporary.

---

## 7. Interview questions

**REST & HTTP**

1. What does REST mean, and what makes an API "RESTful"? Which HTTP methods map to CRUD?
2. Which status codes did we use (200, 201, 204, 404) and when is each appropriate? What's the difference between 401, 403, and 404?
3. Why return a `Location` header on `POST`? What is it for?
4. `PUT` vs `PATCH` vs `POST` — what are the semantics of each? Which are idempotent?

**Spring MVC**

5. What is the `DispatcherServlet` and what role does it play?
6. Difference between `@Controller` and `@RestController`? What does `@ResponseBody` do?
7. What do `@PathVariable`, `@RequestBody`, and `@RequestParam` bind to?
8. How does a Java object become JSON in the response (and vice-versa)? What library does it by default?
9. When and why would you return `ResponseEntity<T>` instead of just `T`?

**Design & architecture**

10. Why introduce DTOs instead of returning the domain/entity object directly? What attack does a separate request DTO help prevent?
11. What is the purpose of the service layer? What's the benefit of returning `Optional` from `findById`?
12. What does "keep controllers thin" mean and why does it matter?

**Dependency injection & beans**

13. What is a Spring bean? What is the default scope, and why does that make thread-safety relevant for `ProblemService`?
14. Constructor injection vs field injection — why is constructor injection preferred?
15. Why are `ConcurrentHashMap` and `AtomicLong` used instead of `HashMap` and a `long` counter?
16. What does `@PostConstruct` do and when does it run in the bean lifecycle?

**Testing & security**

17. What is `MockMvc` and how does it differ from a full end-to-end test with a running server?
18. Difference between `@SpringBootTest` and `@WebMvcTest`?
19. Why does adding `spring-boot-starter-security` immediately protect all endpoints, and how did we relax that for Phase 2? What is CSRF and why disable it for a stateless REST API?

**Stretch / follow-ups**

20. The seeded data and created problems share one in-memory store across tests — what test-isolation problems can that cause, and how would you avoid them?
21. How would you add pagination and filtering (e.g. by difficulty or tag) to `GET /api/problems`?
22. What changes when we swap the in-memory store for a JPA repository in Phase 4 — and what should *not* change?
