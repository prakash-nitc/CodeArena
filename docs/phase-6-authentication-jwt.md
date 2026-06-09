# Phase 6 — Authentication & Authorization (JWT)

> Goal: replace the temporary permit-all security with real, stateless **JWT authentication** and **role-based authorization**. Users register/login to get a token; the token gates who can do what.

This is the largest phase. It introduces accounts, password hashing, token issuing/verifying, a security filter, and authorization rules — and finally retires the "permit everything" config that stood in from Phases 2–5.

---

## 1. Two words that get confused

- **Authentication (authn)** — *who are you?* Proving identity (username + password → a token).
- **Authorization (authz)** — *what may you do?* Checking permissions (this token has role `ADMIN`, so it may delete).

Phase 6 does both: login authenticates; the filter chain + role rules authorize.

---

## 2. Why JWT (and what "stateless" means)

A classic web app stores a **session** on the server and gives the browser a session-id cookie. That's *stateful* — the server must remember every logged-in user.

A **JWT** (JSON Web Token) flips this: the token itself carries the identity (the username as the "subject", plus the role), and it's **signed** by the server. On each request the server just verifies the signature — no session store needed. That's **stateless** auth, which scales well and suits APIs.

A JWT has three dot-separated parts: `header.payload.signature`. The payload (claims) is only Base64-encoded, **not encrypted** — so never put secrets in it. The signature (here HMAC-SHA256 with our secret) is what makes it tamper-proof: change the payload and the signature no longer matches.

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsInJvbGUiOiJVU0VSIn0.<sig>
```

---

## 3. The pieces and how a request flows

**Getting a token (register/login):**

```
POST /api/auth/login {username, password}
      │
   AuthController → AuthService
      │  AuthenticationManager.authenticate(...)   ← checks password (BCrypt) via UserDetailsService
      │  JwtService.generateToken(user)            ← signs a token
      ▼
   { "token": "...", "tokenType": "Bearer", "username": "...", "role": "USER" }
```

**Using a token (every other request):**

```
GET/POST/... with "Authorization: Bearer <token>"
      │
   JwtAuthenticationFilter                      ← validates token, loads user, sets SecurityContext
      │
   Authorization rules (SecurityConfig)         ← is this principal allowed here?
      │            ├─ no/invalid token on a protected URL → 401 (RestAuthenticationEntryPoint)
      │            └─ authenticated but wrong role         → 403 (RestAccessDeniedHandler)
      ▼
   Controller (only if allowed)
```

---

## 4. Walkthrough

### 4.1 Accounts: `User`, `Role`, `UserRepository`

`User` is a JPA entity (`users` table) with a unique `username`, a `password` (**BCrypt hash**, never plaintext), and a `Role` (`USER`/`ADMIN`). `UserRepository` adds `findByUsername` (used during login) and `existsByUsername` (used to reject duplicate registration).

### 4.2 Password hashing: `SecurityBeansConfig`

```java
@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
```

BCrypt is a deliberately **slow, salted** hash. Slowness and per-hash salts make brute-force and rainbow-table attacks impractical — the opposite of fast hashes like MD5/SHA-256, which are wrong for passwords. The same bean encodes on register and verifies on login. This config also exposes the `AuthenticationManager` bean.

### 4.3 `JwtService`

Wraps the **jjwt** library. `generateToken(user)` builds a token with the username as subject, a `role` claim, an issued-at and expiry, signed with a `SecretKey` derived from `jwt.secret`. `isValid(token)` and `extractUsername(token)` verify the signature/expiry and read the subject. HS256 requires a secret of at least 32 bytes (see `application.properties`; override via the `JWT_SECRET` env var in production).

### 4.4 `AppUserDetailsService`

Spring Security doesn't know our `User` table. A `UserDetailsService` is the bridge: given a username, it loads the user and returns a `UserDetails` (username, password hash, authorities). The role becomes an authority `ROLE_USER`/`ROLE_ADMIN` — the `ROLE_` prefix is what `hasRole("ADMIN")` expects.

### 4.5 `AuthService` + `AuthController`

- **register**: reject taken username (`409`), BCrypt-hash the password, save a `USER`, return a token.
- **login**: hand the credentials to the `AuthenticationManager`; on mismatch it throws `BadCredentialsException` → `401`; on success, issue a token.

`AuthController` exposes `POST /api/auth/register` (201) and `POST /api/auth/login` (200). These stay public — you can't require a token to get a token.

### 4.6 `JwtAuthenticationFilter`

A `OncePerRequestFilter` that reads the `Authorization: Bearer` header; if the token is valid, it loads the user and puts an `Authentication` into the `SecurityContextHolder`, so downstream authorization sees an authenticated principal. No token? It does nothing and lets the chain continue (a protected URL then triggers the 401 entry point).

> It is **not** a `@Component`: Spring Boot auto-registers `Filter` beans onto *every* request, which would run it twice. Instead `SecurityConfig` instantiates it and adds it to the chain.

### 4.7 `SecurityConfig` — the lockdown

Replaces permit-all with:

- **stateless** sessions, **CSRF disabled** (token API, not cookie/browser sessions);
- the JWT filter placed before `UsernamePasswordAuthenticationFilter`;
- JSON 401/403 via `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler`;
- rules (first match wins):
  - `/api/auth/**`, H2 console — public
  - `GET` ping/problems — public (anyone can browse)
  - `DELETE /api/problems/**` — `ADMIN` only
  - everything else (`POST`/`PUT`) — any authenticated user

### 4.8 Consistent errors everywhere

Authorization failures happen in the *filter chain*, before any controller, so `@RestControllerAdvice` can't see them. The custom entry point (401) and access-denied handler (403) write the same `ApiError` JSON shape, so clients get a uniform error body. Login failures (`BadCredentialsException`) and duplicate usernames *do* happen in the controller chain, so those are handled in `GlobalExceptionHandler` (401 / 409).

### 4.9 Seeded accounts

`DataSeeder` now also creates two **dev-only** accounts (idempotently): `admin/admin123` (ADMIN) and `user/user123` (USER).

---

## 5. Try it

```bash
./mvnw spring-boot:run
```

```bash
# Public read — no token
curl http://localhost:8080/api/problems

# Create without a token -> 401
curl -i -X POST http://localhost:8080/api/problems \
  -H "Content-Type: application/json" -d '{"title":"X","description":"d"}'

# Log in as the seeded user, capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"user","password":"user123"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

# Create with the token -> 201
curl -i -X POST http://localhost:8080/api/problems \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"title":"My Problem","description":"d"}'

# Delete as USER -> 403 (needs ADMIN). Log in as admin/admin123 for 204.
```

---

## 6. Tests

- **`JwtServiceTest`** (unit): sign/verify round-trip, rejects garbage, rejects a token signed with a different secret, rejects an expired token.
- **`AuthControllerTest`**: register (201 + token), duplicate username (409), short username (400), login (200), wrong password (401).
- **`SecurityIntegrationTest`** (real tokens, no shortcuts): public GET, 401 without token, create with a user token, 403 deleting as USER, 204 deleting as ADMIN.
- **`ProblemControllerTest`**: now annotated `@WithMockUser(roles = "ADMIN")` so the existing endpoint tests run authenticated.

```bash
./mvnw test    # 43 tests, all green
```

---

## 7. Interview questions

**Concepts**

1. Authentication vs authorization — define each with an example from this app.
2. What is a JWT? What are its three parts, and which one makes it tamper-proof?
3. Is a JWT encrypted? What must you therefore never store in its payload?
4. Stateless (JWT) vs stateful (server session) auth — trade-offs? How do you revoke a JWT, and why is that harder?
5. Why hash passwords with BCrypt instead of SHA-256? What do "salt" and "work factor" mean?

**Spring Security**

6. Walk through the Spring Security filter chain. Where does a custom JWT filter go and why before `UsernamePasswordAuthenticationFilter`?
7. What is the `SecurityContextHolder`? What does it mean to "set the authentication" in it?
8. What is a `UserDetailsService` and why is it needed? What is the `ROLE_` prefix convention?
9. What does `AuthenticationManager.authenticate(...)` do under the hood (which provider, which encoder)?
10. Why disable CSRF here, and why `SessionCreationPolicy.STATELESS`?
11. Why must the JWT filter not be a `@Component` (or why disable its auto-registration)?

**Authorization & errors**

12. 401 vs 403 — precise difference. Which component produces each in this app, and why can't `@RestControllerAdvice` handle them?
13. How do `requestMatchers(...).permitAll()/hasRole(...)/authenticated()` get evaluated? Why does order matter?
14. How would you switch from URL-based rules to method-level security (`@PreAuthorize`)?

**Design / security hardening**

15. Where would you add a refresh-token flow, and what problem does it solve?
16. The secret is in `application.properties` with an env override. Why is committing a real secret dangerous, and how should secrets be managed in production?
17. How would you add a "users can only edit problems they created" rule (ownership)? Which layer enforces it?
18. What attacks does a short token expiry mitigate? What's the trade-off with user experience?
