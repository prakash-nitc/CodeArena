# Phase 5 — Input Validation & Error Handling

> Goal: stop trusting input and stop leaking ugly errors. Validate request bodies with **Bean Validation**, and translate every failure into a clean, consistent JSON error via a **global exception handler**.

Until now, bad input either slipped through (an empty title was happily stored) or produced an inconsistent response. Phase 5 introduces two complementary ideas:

- **Validation** — "is this request well-formed?" (declarative, on the DTO).
- **Error handling** — "when something goes wrong, return the right status and a consistent body" (centralised, in one advice class).

It also upgrades not-found handling from `Optional`/booleans to **domain exceptions**, and adds a real business rule: titles must be unique (→ `409 Conflict`).

---

## 1. Validation vs. business rules

Two different kinds of "no":

| | Validation | Business rule |
|---|------------|---------------|
| Question | Is the input syntactically valid? | Does it conflict with our domain/state? |
| Example | title is blank, > 200 chars | title already taken |
| Where | annotations on `ProblemRequest` + `@Valid` | logic in `ProblemService` |
| Result | `400 Bad Request` | `409 Conflict` |

Keeping them separate matters: validation is generic and reusable; business rules need the database and domain knowledge.

---

## 2. Bean Validation on the DTO

`ProblemRequest` now carries constraints:

```java
@NotBlank(message = "title must not be blank")
@Size(max = 200, message = "title must be at most 200 characters")
private String title;

@NotBlank @Size(max = 5000)
private String description;

private Difficulty difficulty;                       // optional (service defaults to MEDIUM)

@Size(max = 10)
private List<@Size(max = 40) String> tags;           // list size + per-element size
```

- `@NotBlank` rejects null, empty, and whitespace-only strings (stronger than `@NotNull`/`@NotEmpty`).
- `@Size` bounds length (for strings) and collection size (for the list).
- `List<@Size(max = 40) String>` is a **container-element constraint** — each tag is validated, not just the list.

These are part of **Jakarta Bean Validation** (the spec); **Hibernate Validator** is the implementation, pulled in by `spring-boot-starter-validation`.

### Triggering it

The constraints do nothing on their own. The controller marks the body `@Valid`:

```java
public ResponseEntity<ProblemResponse> createProblem(@Valid @RequestBody ProblemRequest request) { ... }
```

Now Spring validates the deserialized object before the method body runs. On failure it throws `MethodArgumentNotValidException` — which we handle centrally (below) rather than letting it become a default error page.

---

## 3. Domain exceptions

Two unchecked exceptions express domain failures:

- `ProblemNotFoundException(id)` → 404
- `DuplicateProblemTitleException(title)` → 409

They're **unchecked** (`extends RuntimeException`) so the service can throw them without polluting method signatures, and the global handler catches them in one place.

The service now throws instead of returning `Optional`/boolean:

```java
public ProblemResponse getById(Long id) {
    return problemRepository.findById(id).map(this::toResponse)
            .orElseThrow(() -> new ProblemNotFoundException(id));
}

public ProblemResponse create(ProblemRequest request) {
    String title = normalizeText(request.getTitle());
    if (title != null && problemRepository.existsByTitleIgnoreCase(title)) {
        throw new DuplicateProblemTitleException(title);
    }
    ...
}

public void delete(Long id) {
    if (!problemRepository.existsById(id)) throw new ProblemNotFoundException(id);
    problemRepository.deleteById(id);
}
```

Uniqueness uses two **derived queries** on the repository: `existsByTitleIgnoreCase` (for create) and `existsByTitleIgnoreCaseAndIdNot` (for update, so a problem can keep its own title).

The controller becomes wonderfully thin — no Optional handling, no manual status codes for the error cases:

```java
@GetMapping("/{id}")
public ProblemResponse getProblemById(@PathVariable Long id) { return problemService.getById(id); }

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteProblem(@PathVariable Long id) { problemService.delete(id); }
```

---

## 4. The global exception handler

`@RestControllerAdvice` makes `GlobalExceptionHandler` apply to every controller. Each `@ExceptionHandler` maps an exception type to an `ApiError` with the right status. Spring chooses the handler whose exception type most closely matches what was thrown.

| Exception | Status | When |
|-----------|--------|------|
| `ProblemNotFoundException` | 404 | unknown id |
| `DuplicateProblemTitleException` | 409 | title taken |
| `MethodArgumentNotValidException` | 400 | `@Valid` failed (includes `fieldErrors`) |
| `MethodArgumentTypeMismatchException` | 400 | bad path/query type, e.g. `/problems/abc`, `?difficulty=foo` |
| `HttpMessageNotReadableException` | 400 | malformed JSON / invalid enum in body |
| `Exception` | 500 | last-resort fallback |

The consistent body (`ApiError`, with `fieldErrors` omitted when not relevant):

```json
{
  "timestamp": "2026-05-26T14:03:11.482Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/problems",
  "fieldErrors": { "title": "title must not be blank" }
}
```

> **Caveat (in the code comments too):** a catch-all `Exception → 500` is convenient but can mask framework exceptions (e.g. 405 Method Not Allowed becomes 500). In a larger app you'd handle those explicitly or extend `ResponseEntityExceptionHandler`.

---

## 5. Try it

```bash
./mvnw spring-boot:run
```

```bash
# 400 — validation (blank title)
curl -i -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" -d '{"title":"  ","description":"d"}'

# 201 then 409 — duplicate title
curl -s -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" -d '{"title":"Unique X","description":"d"}' >/dev/null
curl -i -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" -d '{"title":"unique x","description":"d"}'

# 400 — invalid enum in body
curl -i -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" -d '{"title":"X","description":"d","difficulty":"SUPERHARD"}'

# 400 — non-numeric id
curl -i http://localhost:8080/api/problems/abc

# 404 — unknown id
curl -i http://localhost:8080/api/problems/99999
```

---

## 6. Tests

- **`ProblemServiceTest`** updated for the new signatures: `getById`/`update`/`delete` now throw, verified with `assertThatThrownBy(...)`, plus a duplicate-title case. The mock repository also simulates `existsByTitleIgnoreCase[AndIdNot]`.
- **`ProblemControllerTest`** adds: 400 with `fieldErrors` for a blank title, 409 for a duplicate, 400 for an invalid enum body, and 400 for a non-numeric id. The existing 404/204 tests still pass — their status is now produced by the global handler.

```bash
./mvnw test    # 29 tests, all green
```

---

## 7. Interview questions

**Bean Validation**

1. What's the difference between Jakarta Bean Validation and Hibernate Validator?
2. `@NotNull` vs `@NotEmpty` vs `@NotBlank` — when does each apply?
3. What does `@Valid` actually do, and what exception is thrown when validation fails on a `@RequestBody`?
4. How do you validate each element of a collection (not just the collection itself)?
5. What's the difference between `@Valid` and `@Validated`? When would you validate method parameters or path variables?

**Error handling**

6. What does `@RestControllerAdvice` do? How is it different from `@ControllerAdvice`?
7. How does Spring decide which `@ExceptionHandler` to invoke when several could match?
8. Why return a consistent error body shape? What fields are useful to clients?
9. Why is a catch-all `@ExceptionHandler(Exception.class)` both useful and risky?
10. What is `ResponseEntityExceptionHandler` and when would you extend it?

**HTTP semantics**

11. When is `400` vs `404` vs `409` vs `422` appropriate? Why did a duplicate title map to `409`?
12. Which HTTP methods are idempotent? Is `DELETE` of an already-deleted resource a success or a 404 — and what are the arguments either way?

**Design**

13. Why model "not found" as an exception rather than returning `Optional` all the way to the controller? What did that do to the controller code?
14. Why are these domain exceptions unchecked rather than checked?
15. How is "validation" different from a "business rule" like title uniqueness, and why keep them in different layers?

**Stretch / follow-ups**

16. The uniqueness check has a race condition (two concurrent creates can both pass `existsBy...`). How would a database unique constraint close that gap, and how would you map the resulting `DataIntegrityViolationException`? Why is a case-insensitive unique constraint trickier?
17. How would you internationalise validation messages (message codes + `messages.properties`)?
18. How would you write a custom constraint annotation (e.g. `@ValidDifficultySet`)?
