# CodeArena — Complete Study & Interview Guide

> **How to use this doc:** This is your one-stop revision file. Read it top-to-bottom a week before an interview and you'll be able to explain the whole project and every concept behind it. Sections 1–3 are the "what & why". Section 4 explains every concept from scratch with a memory hook. Sections 5–7 are quick-reference cheat sheets. Section 8 is the interview question bank with model answers.

**Per-phase deep dives:** [Phase 1](phase-1-project-setup.md) · [Phase 2](phase-2-rest-api-basics.md) · [Phase 3](phase-3-service-layer.md) · [Phase 4](phase-4-database-integration.md) · [Phase 5](phase-5-validation-error-handling.md) · [Phase 6](phase-6-authentication-jwt.md) · [Phase 7](phase-7-submission-management.md)

---

## Table of contents

1. [The 60-second pitch](#1-the-60-second-pitch)
2. [Tech stack cheat sheet](#2-tech-stack-cheat-sheet)
3. [Architecture & request flow](#3-architecture--request-flow)
4. [Every concept, from scratch](#4-every-concept-from-scratch)
5. [Annotation cheat sheet](#5-annotation-cheat-sheet)
6. [HTTP status codes we use](#6-http-status-codes-we-use)
7. [Gotchas we actually hit](#7-gotchas-we-actually-hit)
8. [Interview question bank](#8-interview-question-bank)

---

## 1. The 60-second pitch

> "CodeArena is a REST backend for a competitive-programming platform, built with **Spring Boot 3 and Java 17**. Users register and log in with **JWT-based authentication**; they can browse coding **problems**, **submit** solutions, and appear on a **leaderboard**. It's organised in clean layers — controllers, services, repositories — persists to **PostgreSQL** (H2 in dev) via **Spring Data JPA**, validates all input with **Bean Validation**, and returns consistent JSON errors through a **global exception handler**. Admins have extra rights (deleting problems, judging submissions). It has ~57 unit and integration tests. I built it in phases, so I can talk about the reasoning behind each layer."

**Domain in one line:** `User` submits `Submission`s (code) to `Problem`s; admins judge them; a `Leaderboard` ranks users by problems solved.

---

## 2. Tech stack cheat sheet

| Layer | Tech | One-liner |
|-------|------|-----------|
| Language | **Java 17** | records, `var`, `switch` expressions, `List.of` |
| Framework | **Spring Boot 3.5** | auto-configured Spring; runnable `main()` with embedded Tomcat |
| Web | **Spring MVC** | annotation-driven REST controllers, JSON via Jackson |
| Persistence | **Spring Data JPA + Hibernate** | ORM: Java objects ⇄ SQL rows, repositories with zero SQL |
| DB | **PostgreSQL** (prod), **H2** (dev) | swapped by a Spring **profile** |
| Security | **Spring Security + JWT (jjwt)** | stateless token auth, role rules, BCrypt hashing |
| Validation | **Jakarta Bean Validation** (Hibernate Validator) | declarative `@NotBlank`/`@Size` |
| Boilerplate | **Lombok** | generates getters/builders at compile time |
| Build | **Maven** (`./mvnw` wrapper) | dependency management + build lifecycle |
| Tests | **JUnit 5, Mockito, MockMvc, AssertJ** | unit + integration |

**Mnemonic for the stack:** *"Boot Web JPA, Postgres H2, Security JWT, Validate, Lombok, Maven, JUnit."*

---

## 3. Architecture & request flow

**Layered architecture** (each layer only talks to the one below):

```
HTTP request
   │
   ▼
┌───────────────┐   Controller  — HTTP ⇄ Java. Thin. Validates (@Valid), delegates.
│  Controller   │   (@RestController)
├───────────────┤
│   Service     │   Service — business logic, rules, transactions. Knows nothing about HTTP.
├───────────────┤   (@Service, @Transactional)
│  Repository   │   Repository — persistence. Spring Data generates the SQL.
├───────────────┤   (interface extends JpaRepository)
│   Database    │   H2 / PostgreSQL
└───────────────┘

DTOs cross the top boundary (never expose entities). Jackson does JSON ⇄ Java.
Cross-cutting: SecurityFilterChain (before controllers), GlobalExceptionHandler (@RestControllerAdvice).
```

**Why layers?** *Separation of concerns* + *testability* + *swappability*. Example payoff: in Phase 3 the service depended on a repository **interface**; in Phase 4 we swapped an in-memory map for a real database and the service barely changed.

**One full request (create a problem):**
```
POST /api/problems  (+ Authorization: Bearer <jwt>)
 → JwtAuthenticationFilter validates token, sets SecurityContext
 → SecurityConfig rule: POST requires authenticated → OK
 → Jackson deserializes JSON → ProblemRequest
 → @Valid runs Bean Validation (else 400 via GlobalExceptionHandler)
 → ProblemController.createProblem() delegates to ProblemService
 → ProblemService normalizes input, checks title uniqueness (else 409), saves via repository
 → repository (Spring Data JPA) issues INSERT
 → service maps entity → ProblemResponse
 → Jackson serializes → 201 Created + Location header
```

---

## 4. Every concept, from scratch

Each concept: **what it is → why it matters → where in CodeArena → 🧠 memory hook.**

### 4.1 Spring Boot & the IoC container
- **What:** Spring is an Inversion-of-Control container: instead of you `new`-ing objects, Spring creates and wires them (**beans**) and injects them where needed (**Dependency Injection**). Spring **Boot** adds *auto-configuration* (sensible defaults based on the classpath), *starters* (curated dependency bundles), and an *embedded server*.
- **Why:** less boilerplate, loose coupling, easy testing (swap a real bean for a mock).
- **Where:** `@SpringBootApplication` on `CodearenaApplication` boots the container and component-scans the package.
- 🧠 *"Don't call us, we'll call you" — you declare beans, Spring wires them.*

### 4.2 Beans, component scanning, DI styles
- **Beans** are objects Spring manages. Stereotypes: `@Component` (generic), `@Service` (business), `@Repository` (data), `@Configuration` (bean definitions), `@RestController` (web). All are scanned from the main class's package downward.
- **Constructor injection** (what we use) makes dependencies explicit, allows `final` fields, and is trivial to unit-test. Prefer it over field injection.
- 🧠 *A bean is a Lego brick; Spring is the kid clicking them together.*

### 4.3 REST & Spring MVC
- **REST:** resources identified by URLs, manipulated with HTTP verbs. `GET` read, `POST` create, `PUT` replace, `DELETE` remove.
- **Spring MVC:** the `DispatcherServlet` (front controller) routes each request to a handler method. `@RestController` = `@Controller` + `@ResponseBody`, so return values become the JSON body (via **Jackson**).
- Bindings: `@PathVariable` (URL segment), `@RequestBody` (JSON body → object), `@RequestParam` (query string).
- `ResponseEntity<T>` = full control over status + headers + body (we use it for 201 + `Location`).
- 🧠 *Verb + Noun = Method + URL.* `POST /api/problems` = "create a problem."

### 4.4 DTOs (Data Transfer Objects)
- **What:** dedicated request/response shapes, separate from entities.
- **Why:** (1) don't leak your DB schema; (2) control exactly what's accepted/returned; (3) **request** DTOs prevent *mass assignment* — a client can't set `id`/`createdAt` because they aren't in `ProblemRequest`.
- **Where:** `ProblemRequest`/`ProblemResponse`, `Auth*`, `Submission*`, `ApiError`, `LeaderboardEntry`.
- 🧠 *Entities are internal; DTOs are the public contract.*

### 4.5 JPA / Hibernate / Spring Data (the ORM stack)
- **JPA** = the *spec* (annotations like `@Entity`). **Hibernate** = the *implementation* that generates SQL. **Spring Data JPA** = the *convenience layer* where you declare a repository **interface** and it writes the implementation.
- **ORM** (Object-Relational Mapping): map objects ⇄ table rows so you `save(problem)` instead of writing `INSERT`.
- Key mappings: `@Entity`/`@Table`, `@Id` + `@GeneratedValue(IDENTITY)`, `@Enumerated(STRING)` (store `"EASY"` not an ordinal), `@ElementCollection` (tags in a side table), `@ManyToOne` (foreign key: many submissions → one problem/user).
- **Derived queries:** method name → SQL (`findByDifficulty`, `existsByTitleIgnoreCase`). **JPQL** `@Query` for aggregation (the leaderboard).
- 🧠 *JPA = the rulebook, Hibernate = the player, Spring Data = the autopilot.*

### 4.6 Fetch types, N+1, and the persistence context
- `@ManyToOne` defaults to **EAGER**; we override to **LAZY** so a related entity loads only when touched.
- **N+1 problem:** loading N submissions then firing 1 query *per* submission for its problem = N+1 queries. Fixed with lazy + `JOIN FETCH`/entity graphs when needed.
- **Persistence context** = Hibernate's first-level cache of "managed" entities within a transaction; it tracks changes (dirty checking) and flushes them as SQL.
- 🧠 *EAGER = "bring everything now"; LAZY = "fetch when I ask".*

### 4.7 Transactions & Open-Session-In-View
- **`@Transactional`** wraps a method in one DB transaction (commit on success, rollback on unchecked exception). It also keeps the **persistence context open** for the whole method.
- We set `spring.jpa.open-in-view=false` (recommended). That means lazy associations can **only** be loaded inside a transaction — which is exactly why `SubmissionService` is `@Transactional` (so mapping `submission.getProblem().getTitle()` works instead of throwing `LazyInitializationException`).
- 🧠 *OSIV off = "do your DB work in the service, not the view."*

### 4.8 Config & profiles
- `application.properties` = H2 defaults; `application-postgres.properties` = PostgreSQL, activated by the **`postgres` profile**. Same build, different environment — no code change.
- `ddl-auto=update` lets Hibernate build the schema in dev; in prod you'd use migrations (Flyway/Liquibase) + `validate`.
- 🧠 *Profiles = one jar, many environments.*

### 4.9 Validation
- **Bean Validation** annotations on DTOs (`@NotBlank`, `@Size`, `@NotNull`) + `@Valid` on the controller param. Failing validation throws `MethodArgumentNotValidException` → mapped to **400** with per-field messages.
- Validation ("is the input well-formed?") ≠ business rules ("is this title taken?" → in the service).
- 🧠 *`@Valid` = the bouncer checking IDs at the door.*

### 4.10 Error handling
- **`@RestControllerAdvice`** (`GlobalExceptionHandler`) centralises exception → HTTP mapping, returning a consistent `ApiError` body. Controllers just throw.
- Mappings: not-found → 404, duplicate → 409, validation/bad-enum/bad-type → 400, bad credentials → 401, access denied → 403, fallback → 500.
- 🧠 *Throw where the problem is; translate to HTTP in one place.*

### 4.11 Authentication vs Authorization
- **Authn = who are you?** (login → token). **Authz = what may you do?** (role/ownership rules).
- 🧠 *Authe**n**tication = ide**n**tity; Autho**r**ization = **r**ights.*

### 4.12 JWT & stateless auth
- A **JWT** is `header.payload.signature`. The payload carries claims (username subject, role); the **signature** (HMAC-SHA256 with our secret) makes it tamper-proof. It's **signed, not encrypted** — never put secrets in it.
- **Stateless:** the token *is* the identity, so no server session store. Each request is verified independently.
- 🧠 *A JWT is a tamper-proof wristband: the club checks the stamp, not a guest list.*

### 4.13 Spring Security filter chain
- Requests pass through a chain of filters *before* controllers. Our `JwtAuthenticationFilter` (a `OncePerRequestFilter`) reads `Authorization: Bearer …`, validates it, loads the user via `UserDetailsService`, and puts an `Authentication` in the **`SecurityContextHolder`**. Then authorization rules decide access.
- **401 vs 403:** no/invalid token on a protected URL → **401** (`RestAuthenticationEntryPoint`); valid token but wrong role → **403** (`RestAccessDeniedHandler`).
- Config: **stateless** sessions, **CSRF disabled** (token API, not cookies), rules like `DELETE → hasRole("ADMIN")`.
- 🧠 *Filters are airport security; the controller is the gate. You clear security before you board.*

### 4.14 Password hashing (BCrypt)
- Passwords are stored as **BCrypt hashes** — slow + salted on purpose, defeating brute force and rainbow tables. `PasswordEncoder.encode` on register, `matches` on login (via `AuthenticationManager`).
- 🧠 *Never store passwords — store slow, salted hashes.*

### 4.15 Role-based vs ownership-based authorization
- **Role-based:** "DELETE needs ADMIN" — expressed in URL rules.
- **Ownership-based:** "you can only read *your* submissions" — data-dependent, so it lives in the service (`SubmissionService.requireOwnerOrAdmin`) and throws `AccessDeniedException` → 403.
- 🧠 *Roles = your rank; ownership = your stuff.*

### 4.16 Aggregation & the leaderboard
- JPQL `GROUP BY username` + `COUNT(DISTINCT problem.id)` where status = ACCEPTED. Counting **distinct problems** stops re-solves from inflating scores. Returned as an **interface projection** (`LeaderboardRow`), then the service assigns 1-based ranks.
- 🧠 *Group, count distinct, order desc, then number the rows.*

### 4.17 Lombok
- Generates boilerplate at compile time: `@Getter/@Setter`, `@Builder`, `@NoArgsConstructor`/`@AllArgsConstructor`, `@Data` (all of the above + equals/hashCode/toString).
- ⚠️ We use `@Getter/@Setter` (not `@Data`) on **entities**, because `@Data`'s equals/hashCode over all fields is an anti-pattern for JPA entities.
- 🧠 *Less boilerplate, same bytecode.*

### 4.18 Testing
- **Unit tests** (`ProblemServiceTest`, `SubmissionServiceTest`): no Spring; **Mockito** mocks the repository; fast; test logic in isolation.
- **Integration tests** (`ProblemControllerTest`, `AuthControllerTest`, `SecurityIntegrationTest`, `SubmissionControllerTest`): `@SpringBootTest` boots the app on H2; **MockMvc** drives real HTTP through the full stack; `@WithMockUser` fakes a principal, or we use real JWTs via login.
- 🧠 *Unit = one brick tested alone; Integration = the wall standing up.*

---

## 5. Annotation cheat sheet

| Annotation | Meaning |
|-----------|---------|
| `@SpringBootApplication` | boot + auto-config + component scan |
| `@RestController` / `@RequestMapping` | REST handler / base path |
| `@GetMapping` `@PostMapping` `@PutMapping` `@DeleteMapping` | verb → method |
| `@PathVariable` / `@RequestBody` / `@RequestParam` | URL part / JSON body / query param |
| `@Service` `@Repository` `@Component` `@Configuration` | bean stereotypes |
| `@Entity` `@Table` `@Id` `@GeneratedValue` | JPA mapping |
| `@Enumerated(EnumType.STRING)` | store enum by name |
| `@ManyToOne` `@ElementCollection` `@JoinColumn` | relationships |
| `@Transactional` | one transaction / open persistence context |
| `@Valid` + `@NotBlank/@Size/@NotNull` | trigger + declare validation |
| `@RestControllerAdvice` / `@ExceptionHandler` | global error handling |
| `@Bean` | factory method producing a bean |
| `@WithMockUser` | inject a fake authenticated user in tests |

---

## 6. HTTP status codes we use

| Code | Meaning | In CodeArena |
|------|---------|--------------|
| 200 OK | success w/ body | GET, PUT, login |
| 201 Created | resource created (+`Location`) | POST problem/submission, register |
| 204 No Content | success, no body | DELETE |
| 400 Bad Request | malformed/invalid input | validation, bad enum, bad path type |
| 401 Unauthorized | not authenticated | missing/invalid token, wrong password |
| 403 Forbidden | authenticated but not allowed | USER deletes / reads others' submission |
| 404 Not Found | unknown id | missing problem/submission |
| 409 Conflict | conflicts with state | duplicate problem title / username |
| 500 Internal Server Error | unexpected | fallback handler |

🧠 *401 = "who are you?", 403 = "not allowed", 404 = "doesn't exist", 409 = "already exists".*

---

## 7. Gotchas we actually hit

- **`SecurityFilterChain` must return `http.build()`**, not `http` (type mismatch) — caught in Phase 2.
- **Security secures everything by default** once the starter is on the classpath — we permitted all in early phases, then locked down in Phase 6.
- **`deleteById` is `void` in Spring Data** (Phase 3→4): we check `existsById` first to keep boolean semantics.
- **`LazyInitializationException`** with OSIV off — solved by making the submission service `@Transactional`.
- **Rule ordering in Spring Security** (first match wins): `/api/problems/*/submissions` (authenticated) must come *before* the public `GET /api/problems/**`.
- **Filter beans auto-register on every request** in Boot — so `JwtAuthenticationFilter` is *not* a `@Component`; the config `new`s it and adds it to the chain.
- **`@Data` on JPA entities** is discouraged (equals/hashCode) — use `@Getter/@Setter`.
- **Enum stored as ORDINAL** is fragile — always `EnumType.STRING`.

---

## 8. Interview question bank

Grouped by topic. Answers are concise — expand with examples from the project.

### Spring Boot & DI
1. **What is Spring Boot vs Spring?** Boot = Spring + auto-configuration + starters + embedded server; removes XML/boilerplate and gives a runnable `main()`.
2. **What is Inversion of Control / Dependency Injection?** The container creates and wires objects (beans) instead of you `new`-ing them; dependencies are injected, giving loose coupling and testability.
3. **What does `@SpringBootApplication` do?** Combines `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` (from its package down).
4. **Constructor vs field injection?** Constructor: explicit deps, `final` fields, easy mocking — preferred. Field: hidden deps, harder to test.
5. **What is a bean scope?** Default **singleton** (one shared instance) — which is why shared state must be thread-safe.

### REST & MVC
6. **What makes an API RESTful?** Resources as URLs, HTTP verbs for actions, stateless, standard status codes.
7. **`@Controller` vs `@RestController`?** `@RestController` adds `@ResponseBody` — returns become the JSON body, not view names.
8. **When use `ResponseEntity`?** When you need to set status/headers explicitly (201 + `Location`, 204).
9. **`PUT` vs `PATCH` vs `POST`?** PUT = full replace (idempotent), PATCH = partial, POST = create/non-idempotent.
10. **How does JSON ⇄ Java happen?** Jackson, auto-configured by Boot, via `@RequestBody`/`@ResponseBody`.

### JPA / Data
11. **JPA vs Hibernate vs Spring Data JPA?** Spec vs implementation vs convenience layer.
12. **How can a repository have no implementation?** Spring Data generates a proxy at runtime from the interface + method names.
13. **What is a derived query?** Query parsed from the method name (`findByDifficulty` → `WHERE difficulty = ?`).
14. **`EnumType.STRING` vs `ORDINAL`?** STRING stores the name (safe); ORDINAL stores an index that breaks if the enum is reordered.
15. **Default fetch type of `@ManyToOne`? Why override to LAZY?** EAGER by default; LAZY avoids loading relations you don't use and helps dodge N+1.
16. **What is the N+1 problem?** 1 query for a list + 1 per row for a relation; fix with join fetch/entity graph.
17. **Why was the submission service `@Transactional`?** With OSIV off, lazy associations only load inside a transaction; the service maps entities to DTOs there.
18. **`ddl-auto` in production?** Use `validate` + migrations (Flyway/Liquibase), not `update`/`create`.

### Validation & errors
19. **What does `@Valid` do, and what's thrown on failure?** Triggers Bean Validation; throws `MethodArgumentNotValidException` → 400.
20. **`@NotNull` vs `@NotEmpty` vs `@NotBlank`?** Not null / not null-or-empty / not null-empty-or-whitespace.
21. **How does global error handling work?** `@RestControllerAdvice` with `@ExceptionHandler`s maps exceptions to a consistent `ApiError` + status.
22. **400 vs 404 vs 409?** Malformed input / doesn't exist / conflicts with current state (duplicate).
23. **Validation vs business rule?** Well-formed input (DTO annotations) vs domain rule needing data (service, e.g. unique title).

### Security
24. **Authentication vs authorization?** Who you are vs what you may do.
25. **What is a JWT? Is it encrypted?** Signed token (`header.payload.signature`); signed **not** encrypted — don't store secrets in it.
26. **Stateless vs session auth — trade-offs?** No server store, scales well, but revocation is hard (mitigate with short expiry + refresh tokens).
27. **Why BCrypt over SHA-256 for passwords?** Deliberately slow + salted; fast hashes are brute-forceable.
28. **Walk the filter chain.** Filters run before controllers; the JWT filter authenticates and sets the `SecurityContext`; authorization rules then allow/deny.
29. **Why is the JWT filter not a `@Component`?** Boot would auto-register it as a global servlet filter (running twice); instead the config adds it to the chain.
30. **401 vs 403?** Unauthenticated vs authenticated-but-forbidden; produced by the entry point vs access-denied handler.
31. **Why disable CSRF and use stateless sessions?** CSRF targets cookie/browser sessions; a Bearer-token API doesn't use them.
32. **Role-based vs ownership-based authz?** Rank (roles, URL rules) vs "your data" (runtime check in the service).

### Testing
33. **Unit vs integration test here?** Mockito service tests (no Spring) vs `@SpringBootTest` + MockMvc through the full stack on H2.
34. **What is `@WithMockUser`?** Injects a fake authenticated principal so secured endpoints can be tested without a real token.
35. **`@SpringBootTest` vs `@WebMvcTest`?** Full context vs just the web layer (controllers) with collaborators mocked.

### Design & the project
36. **Why the layered architecture?** Separation of concerns, testability, swappability (demonstrated by the in-memory → JPA swap).
37. **Why DTOs instead of exposing entities?** Stable contract, control over fields, mass-assignment protection.
38. **Walk me through creating a problem end-to-end.** (Use the flow in §3.)
39. **How would you add the real judge (Phase 8)?** A `Judge` service called after save (likely **async**), running code in a sandbox (Docker), then transitioning the submission status; leaderboard already keys off `ACCEPTED`.
40. **What would you improve with more time?** Pagination, refresh tokens, DB-level unique constraint on title (closes the check-then-insert race), caching the leaderboard, Flyway migrations, OpenAPI/Swagger docs, rate limiting.

---

*Good luck. If you can explain §3 (the request flow) and answer §8 questions 11, 17, 25, 28, and 36 comfortably, you know this project cold.*
