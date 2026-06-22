# Phase 7 — Problem & Submission Management

> Goal: let authenticated users submit solutions to problems, manage those submissions with **ownership-based access**, let an admin "judge" them, and rank everyone on a simple **leaderboard**.

Problems already had full CRUD (Phases 2–6). Phase 7 adds the other half of a coding platform: **submissions**. Because the real code-execution sandbox (Phase 8) is intentionally out of scope, judging is *simulated* by an admin endpoint — but everything around it (modelling, relationships, ownership, aggregation) is real.

> **Scope note.** This phase also folds in a lightweight leaderboard (originally the "Phase 9" idea). Full *contests* (time-windowed, registered, auto-scored) were dropped: without a real judge their scoring would be fictional and hard to justify. The real sandbox remains documented future work.

---

## 1. What's new

| Concept | Detail |
|--------|--------|
| `Submission` entity | `@ManyToOne` to both `Problem` and `User` (foreign keys), plus `language`, `sourceCode`, `status`, `createdAt` |
| Lifecycle | new submissions are `PENDING`; an admin moves them to `ACCEPTED`/`REJECTED` (stand-in for the judge) |
| Ownership | a user sees only their own submissions; admins see all |
| Leaderboard | users ranked by **distinct problems accepted**, via an aggregation query |

---

## 2. Modelling relationships with JPA

`Submission` has two `@ManyToOne` associations:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "problem_id", nullable = false)
private Problem problem;

@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

- `@ManyToOne` creates a foreign-key column (`problem_id`, `user_id`). "Many submissions → one problem."
- **`fetch = LAZY`** overrides the JPA default for `@ManyToOne` (which is EAGER). Lazy means the related `Problem`/`User` is loaded only when accessed, avoiding needless joins — important to dodge the classic **N+1** problem when listing many submissions.

### The lazy-loading catch (and why the service is `@Transactional`)

Phase 4 disabled Open-Session-In-View. So once a repository call returns, the persistence context is closed. If we then touch `submission.getProblem().getTitle()` while mapping to a DTO, we'd get a `LazyInitializationException`.

The fix: make `SubmissionService` methods `@Transactional`. The transaction keeps the persistence context open for the whole method, so the lazy associations load successfully while we build the response. Read methods use `@Transactional(readOnly = true)` (a hint that can let the DB/driver optimise, and documents intent).

---

## 3. Authorization beyond roles: ownership

Phase 6 gave us *role*-based rules ("DELETE needs ADMIN"). Submissions need something finer: **you can only read your own**. Roles can't express "owns this row" — that's a runtime, data-dependent check, so it lives in the service:

```java
private void requireOwnerOrAdmin(Submission submission, String username) {
    if (submission.getUser().getUsername().equals(username)) return;       // owner
    if (requireUser(username).getRole() != Role.ADMIN)                     // not admin
        throw new AccessDeniedException("You may only access your own submissions");
}
```

The current user arrives via the controller as `Authentication.getName()` (the username, set from the JWT). Throwing `AccessDeniedException` yields a **403** — handled by `GlobalExceptionHandler` when thrown from the service (URL-level role denials are still handled earlier by `RestAccessDeniedHandler`; both produce a consistent 403 body).

---

## 4. The leaderboard: an aggregation query

`SubmissionRepository.leaderboard()` is explicit JPQL with `GROUP BY` and `COUNT(DISTINCT ...)`:

```java
@Query("""
    select s.user.username as username, count(distinct s.problem.id) as solvedCount
    from Submission s
    where s.status = com.codearena.codearena.model.SubmissionStatus.ACCEPTED
    group by s.user.username
    order by count(distinct s.problem.id) desc, s.user.username asc
    """)
List<LeaderboardRow> leaderboard();
```

- Counting **distinct problems** (not raw accepted submissions) means re-solving a problem doesn't inflate your score.
- It returns an **interface projection** (`LeaderboardRow`) — a lightweight read-only view, not entities. The service then walks the already-ordered rows assigning a 1-based `rank` into the `LeaderboardEntry` DTO.

---

## 5. Endpoints

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| `POST` | `/api/problems/{id}/submissions` | authenticated | submit a solution (201, `PENDING`) |
| `GET` | `/api/problems/{id}/submissions` | authenticated | admin: all; user: own |
| `GET` | `/api/submissions/me` | authenticated | the caller's submissions |
| `GET` | `/api/submissions/{id}` | owner or **ADMIN** | else 403 |
| `PUT` | `/api/submissions/{id}/status` | **ADMIN** | "judge" → ACCEPTED/REJECTED |
| `GET` | `/api/leaderboard` | public | ranked standings |

**Routing subtlety:** `GET /api/problems/**` is public, so the rule for `/api/problems/*/submissions` (authenticated) is placed *before* it in `SecurityConfig` — first match wins, otherwise listing submissions would be public.

---

## 6. Try it

```bash
./mvnw spring-boot:run

# Log in (seeded accounts)
USER=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
ADMIN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# Submit to problem 1
curl -s -X POST localhost:8080/api/problems/1/submissions -H "Authorization: Bearer $USER" \
  -H 'Content-Type: application/json' -d '{"language":"JAVA","sourceCode":"class S {}"}'

# Admin judges submission 1 as accepted
curl -s -X PUT localhost:8080/api/submissions/1/status -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' -d '{"status":"ACCEPTED"}'

# Leaderboard (public)
curl -s localhost:8080/api/leaderboard
```

---

## 7. Tests

- **`SubmissionServiceTest`** (unit, mocked repos): create → PENDING, missing problem → 404, ownership (owner ok / other 403 / admin ok), status update, leaderboard ranking.
- **`SubmissionControllerTest`** (integration, real JWTs): submit (201), no token (401), missing problem (404), owner vs other (200 vs 403), admin judge vs non-admin (200 vs 403), and the public leaderboard reflecting an accepted submission.

```bash
./mvnw test    # 57 tests, all green
```

---

## 8. Interview questions

**JPA relationships**

1. What does `@ManyToOne` map to in the database? What's the default fetch type, and why did we override it to `LAZY`?
2. What is the N+1 select problem? How do lazy fetching, `JOIN FETCH`, or entity graphs address it?
3. Why did the service need to be `@Transactional` for the response mapping to work? How does that relate to Open-Session-In-View?
4. `@Transactional(readOnly = true)` — what does it actually do/signal?

**Authorization**

5. Role-based vs ownership-based (a.k.a. attribute/relationship-based) authorization — why can't roles express "only the owner"?
6. Where should an ownership check live — controller, service, or a security layer (`@PreAuthorize` with SpEL)? Trade-offs?
7. In this app, which 403s are produced by the security filter chain vs by the `@ControllerAdvice`, and why must both exist?
8. How does the controller know who the current user is? Trace it back to the JWT.

**Querying & aggregation**

9. Explain `COUNT(DISTINCT ...)` with `GROUP BY` here. Why distinct problems rather than accepted submissions?
10. What is an interface projection in Spring Data, and why use one instead of returning entities or `Object[]`?
11. The rank is assigned in Java after the DB orders rows. What are the pros/cons vs computing it in SQL (window functions like `RANK()`)?

**Design & REST**

12. Why is the submitter taken from the token and the problem from the URL, rather than from the request body?
13. Why does `GET /api/problems/{id}/submissions` need a rule placed *before* the public `GET /api/problems/**`? What does "first match wins" imply for ordering?
14. The leaderboard recomputes on every request. At scale, how would you cache or materialise it, and what staleness trade-offs appear?

**Scope / extension**

15. This phase fakes judging with an admin endpoint. Sketch how a real code-execution engine (Phase 8) would plug in — where would the call go, sync vs async, and how would the status transition?
16. How would you add pagination to the submissions and leaderboard endpoints?
17. What would "contests" require on top of this (time windows, registration, scoring), and why are they hard without a real judge?
