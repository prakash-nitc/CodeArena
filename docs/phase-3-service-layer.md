# Phase 3 — Service Layer & Business Logic

> Goal: turn the thin Phase 2 service into a proper **business layer**, sitting on top of a **repository abstraction**, and give it real logic — input normalization, filtering/search, and statistics.

Phase 2 already had a `ProblemService`, but it doubled as the data store (it held the `ConcurrentHashMap` itself) and contained almost no business rules. Phase 3 fixes both: it separates *storage* from *logic*, and it adds the kind of decisions a real service makes. There is still no database (Phase 4) and still no validation/error-handling framework (Phase 5).

---

## 1. The architectural change: introduce a repository

In Phase 2 the layers were:

```
Controller → Service (owns the HashMap)
```

In Phase 3 they become:

```
Controller → Service (business logic) → Repository (interface) → InMemoryProblemRepository (storage)
```

Why add the extra layer?

- **Separation of concerns.** The service should reason about *problems* (rules, filtering, stats). It should not care whether they live in a `HashMap`, PostgreSQL, or a file. Storage details now live behind `ProblemRepository`.
- **Swappability / Phase 4 readiness.** The service depends on the `ProblemRepository` *interface*. In Phase 4 we replace `InMemoryProblemRepository` with a Spring Data JPA repository, and the service doesn't change. The interface even mirrors Spring Data's method names (`findAll`, `findById`, `save`, `deleteById`, `existsById`) so the migration is nearly a drop-in.
- **Testability.** Business logic can be unit-tested by wiring the service to a simple in-memory repository — no Spring context, no database, milliseconds per test.

This is the **Dependency Inversion Principle** in action: the high-level policy (service) and the low-level detail (storage) both depend on an abstraction (the interface), not on each other.

---

## 2. The classes

### 2.1 `repository/ProblemRepository.java` — the abstraction

A plain interface describing persistence operations:

```java
public interface ProblemRepository {
    List<Problem> findAll();
    Optional<Problem> findById(Long id);
    Problem save(Problem problem);     // assigns an id when id == null
    boolean deleteById(Long id);
    boolean existsById(Long id);
}
```

Key detail: **`save` does both insert and update**, just like Spring Data. If `id` is `null`, it's a new problem and an id is assigned; otherwise the existing record is overwritten. This "upsert" semantics is what lets the service treat create and update almost identically.

### 2.2 `repository/InMemoryProblemRepository.java` — the implementation

Holds the `ConcurrentHashMap<Long, Problem>` and the `AtomicLong` id generator that used to live in the service. Annotated `@Repository`, so Spring creates it and injects it into the service. `findAll()` returns a **defensive copy** (`new ArrayList<>(store.values())`) so callers can't accidentally mutate the backing collection.

### 2.3 `service/ProblemService.java` — now pure business logic

The service is injected with a `ProblemRepository` (constructor injection) and no longer stores anything. Its new responsibilities:

**Input normalization** — applied on every `create`/`update`:
- `normalizeText` trims surrounding whitespace from title/description.
- `resolveDifficulty` defaults a missing difficulty to `MEDIUM`.
- `normalizeTags` trims each tag, lower-cases it, drops blanks, and removes duplicates while preserving order (using a `LinkedHashSet`). So `["  Graph ", "graph", "BFS", ""]` becomes `["graph", "bfs"]`.

> Normalization keeps the data *consistent* regardless of how messy the client input is — a classic service-layer job. (Hard *validation*, e.g. rejecting a blank title, is deliberately left for Phase 5.)

**Filtering & search** — `findProblems(Difficulty, String)`:
- Both arguments are optional and combine with logical AND.
- `difficulty` filters by exact match; `search` matches a case-insensitive substring against the title or any tag.
- Results are sorted by id for a stable order.
- `findAll()` simply delegates to `findProblems(null, null)`.

**Statistics** — `getStats()` returns a `ProblemStatsResponse` with the total count and a per-difficulty breakdown. It pre-seeds an `EnumMap` with a zero for every `Difficulty`, so the response always contains all buckets (even empty ones) — predictable output for clients.

### 2.4 `dto/ProblemStatsResponse.java`

A small response DTO (`total` + `Map<Difficulty, Long> countByDifficulty`). It's a *derived* view, computed on demand — nothing stores it.

### 2.5 `controller/ProblemController.java` — two additions

- `GET /api/problems` now accepts optional `?difficulty=` and `?search=` query parameters, bound with `@RequestParam(required = false)`. Spring converts `difficulty=EASY` straight into the `Difficulty` enum.
- `GET /api/problems/stats` returns the statistics.

> **Routing note:** `/api/problems/stats` and `/api/problems/{id}` could look ambiguous, but Spring's path matching prefers the more specific *literal* pattern (`/stats`) over the *variable* pattern (`/{id}`), so `stats` is never mistaken for an id.

The controller stays thin — it just binds parameters and delegates.

---

## 3. Where logic lives — and why

A frequent question: should filtering/search live in the repository or the service?

In real Spring Data you *could* express `findByDifficulty(...)` as a derived query on the repository, and in Phase 4 we may push some filtering down to the database for efficiency. For Phase 3 we deliberately keep the **repository minimal and generic** (pure CRUD) and put filtering/search/stats in the **service**, because:

1. it keeps the repository interface tiny and trivially swappable, and
2. it makes the teaching point obvious — *business logic is the service's job*.

The trade-off (filtering in memory over `findAll()`) is fine at this scale and is called out as something Phase 4 can optimize.

---

## 4. Trying it out

```bash
cd codearena
./mvnw spring-boot:run
```

```bash
# Filter by difficulty
curl "http://localhost:8080/api/problems?difficulty=EASY"

# Search by keyword (title or tag, case-insensitive)
curl "http://localhost:8080/api/problems?search=substring"

# Combine both
curl "http://localhost:8080/api/problems?difficulty=MEDIUM&search=string"

# Statistics
curl "http://localhost:8080/api/problems/stats"
# {"total":2,"countByDifficulty":{"EASY":1,"MEDIUM":1,"HARD":0}}

# Tags get normalized on create (lower-cased, de-duplicated)
curl -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" \
  -d '{"title":"  Trie  ","tags":["Tree","tree","  PREFIX "]}'
# stored title -> "Trie", tags -> ["tree","prefix"], difficulty -> "MEDIUM"
```

---

## 5. The tests

- **`ProblemServiceTest`** — *pure unit tests* with no Spring context: `new ProblemService(new InMemoryProblemRepository())`. Because `@PostConstruct` seeding only runs inside the container, each test starts empty, so counts are deterministic. Covers normalization (tags/difficulty/trim), id & timestamp assignment, filtering, search, combined filters, update/delete semantics, and stats.
- **`ProblemControllerTest`** — extended with web-layer tests for `?search=`, combined `?difficulty=&search=`, and `/stats`. To stay order-independent against the shared application context, the search tests use tokens unique to each test.

```bash
./mvnw test    # 22 tests
```

---

## 6. Interview questions

**Layering & design principles**

1. What is the responsibility of a service layer vs a repository vs a controller?
2. What is the Repository pattern and what problem does it solve?
3. Explain the Dependency Inversion Principle. How does depending on `ProblemRepository` (an interface) rather than `InMemoryProblemRepository` illustrate it?
4. Why does making the service depend on an interface make Phase 4's database migration easier?
5. Where should filtering/search logic live — controller, service, or repository — and what are the trade-offs?

**Business logic & data handling**

6. What is "input normalization" and why do it in the service layer? Give examples from `ProblemService`.
7. Why use a `LinkedHashSet` to de-duplicate tags instead of a `HashSet` or a `List`?
8. Why does `getStats` pre-seed the `EnumMap` with zeros for every difficulty? What's the benefit of `EnumMap` over `HashMap` here?
9. The Phase 2 service generated the id itself; now the repository's `save` does. Why is that the better place, and how does it mirror JPA?

**Spring specifics**

10. What does `@Repository` add over `@Component`? (Hint: persistence-exception translation.)
11. How does Spring bind `?difficulty=EASY` to a `Difficulty` enum parameter? What happens for an unknown value, and which phase will handle that nicely?
12. Why doesn't `@PostConstruct` seeding run in the plain `new ProblemService(...)` unit test, and why is that actually convenient?
13. Given `/api/problems/{id}` and `/api/problems/stats`, how does Spring decide which handler a request like `/api/problems/stats` matches?

**Testing**

14. Contrast the unit test (`ProblemServiceTest`) with the integration test (`ProblemControllerTest`): what does each verify, and which is faster/why?
15. The controller tests share one application context. What test-isolation hazard does that create, and how did we work around it?

**Stretch / follow-ups**

16. How would you make `findProblems` efficient if there were a million problems (instead of filtering in memory)?
17. How would you add pagination and sorting to the listing endpoint?
18. If two requests call `create` at the same time, why is the id generation still safe? Which classes guarantee that?
