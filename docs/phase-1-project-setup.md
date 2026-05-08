# Phase 1 — Project Setup & Hello World

> Goal: stand up a runnable Spring Boot application skeleton that we can build, run, and grow phase by phase.

This document explains, from scratch, everything that makes up the initial project: what each file is, why it exists, and the concepts a newcomer needs to understand Spring Boot's starting point. It ends with interview questions covering the same ground.

---

## 1. What is Spring Boot (and why use it)?

**Spring** is a large framework for building Java applications. Historically, configuring Spring meant writing a lot of XML and wiring beans by hand.

**Spring Boot** sits on top of Spring and removes most of that ceremony through three ideas:

1. **Auto-configuration** — Boot looks at what libraries are on your classpath and configures sensible defaults. Add the web starter, and it automatically configures an embedded web server and JSON support.
2. **Starters** — curated dependency bundles (e.g. `spring-boot-starter-web`) so you depend on one artifact instead of hand-picking dozens of compatible versions.
3. **Embedded server** — the app ships with an embedded Tomcat, so `java -jar app.jar` is enough to run a web server. No external server to install and deploy into.

The result: a production-grade app from a single `main` method.

---

## 2. The files created in Phase 1

### `pom.xml` — the Maven build file

Maven is the build tool. `pom.xml` (Project Object Model) declares:

- **Parent** — `spring-boot-starter-parent`. This gives us a curated set of dependency versions (so we rarely specify versions ourselves) and sensible plugin defaults.
- **Java version** — `<java.version>17</java.version>`.
- **Dependencies** — the libraries the project uses. In CodeArena these are the starters we will grow into:
  - `spring-boot-starter-web` — REST controllers + embedded Tomcat + Jackson (JSON). *Used heavily from Phase 2.*
  - `spring-boot-starter-data-jpa` — database access via JPA/Hibernate. *Used in Phase 4.*
  - `spring-boot-starter-security` — authentication/authorization. *Configured in Phase 6.*
  - `spring-boot-starter-validation` — Bean Validation. *Used in Phase 5.*
  - `postgresql` — the PostgreSQL JDBC driver (`runtime` scope). *Phase 4.*
  - `lombok` — compile-time boilerplate generation (getters/setters/builders).
  - `spring-boot-starter-test` + `spring-security-test` — testing (`test` scope).
- **Build plugins** — the `spring-boot-maven-plugin` (packages an executable "fat" jar) and the compiler plugin configured with the Lombok annotation processor.

> Note: having a dependency on the classpath does **not** mean it is active. In Phase 1–2 we explicitly switch off the database and relax security; we only "turn them on" when their phase arrives.

### `mvnw` / `mvnw.cmd` — the Maven Wrapper

These scripts download and run a pinned version of Maven, so contributors don't need Maven pre-installed and everyone uses the same version. Use `./mvnw` (macOS/Linux/Git Bash) or `mvnw.cmd` (Windows cmd/PowerShell) exactly like you'd use `mvn`.

### `CodearenaApplication.java` — the entry point

```java
@SpringBootApplication
public class CodearenaApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodearenaApplication.class, args);
    }
}
```

- `@SpringBootApplication` is a meta-annotation that combines three:
  - `@SpringBootConfiguration` — marks this as a configuration class.
  - `@EnableAutoConfiguration` — turns on Boot's auto-configuration.
  - `@ComponentScan` — scans this package (`com.codearena.codearena`) **and its sub-packages** for components (`@Component`, `@Service`, `@RestController`, `@Configuration`, …). This is why later phases place code in sub-packages like `controller`, `service`, `config`.
- `SpringApplication.run(...)` boots the **application context** (the container that creates and wires all beans) and starts the embedded server.

### `application.properties` — configuration

Spring Boot reads this file from `src/main/resources` at startup. In Phase 1 it:

- sets the application name, and
- temporarily disables the database auto-configuration and sets a default security user, because those subsystems aren't ready to be used yet.

Externalizing configuration here (instead of hard-coding it) means the same build can run in different environments by changing properties, not code.

### `src/test/.../CodearenaApplicationTests.java`

```java
@SpringBootTest
class CodearenaApplicationTests {
    @Test
    void contextLoads() {}
}
```

`@SpringBootTest` starts the whole application context for the test. The (empty) `contextLoads` test passes only if the application can start — i.e. all beans wire up correctly. It's a surprisingly powerful smoke test: misconfiguration usually fails here.

### `.gitignore`, `.gitattributes`, `HELP.md`, `README.md`, `CONTRIBUTING.md`

Project hygiene: ignore build output (`/target`) and IDE files, normalize line endings, and document the project for humans.

---

## 3. The standard Maven project layout

```
src/
  main/
    java/        ← application source code
    resources/   ← config & static files (application.properties, etc.)
  test/
    java/        ← test source code
target/          ← build output (ignored by git)
pom.xml          ← build definition
```

This "convention over configuration" layout means Maven knows where everything is without extra setup.

---

## 4. How to run it

```bash
cd codearena
./mvnw spring-boot:run      # run directly
# or
./mvnw clean package        # build an executable jar into target/
java -jar target/codearena-0.0.1-SNAPSHOT.jar
```

The app starts on `http://localhost:8080`.

---

## 5. Interview questions

**Conceptual**

1. What problem does Spring Boot solve compared to plain Spring?
2. Explain auto-configuration. How does Boot decide what to configure?
3. What is a "starter" dependency? Give an example and what it pulls in.
4. What does `@SpringBootApplication` expand to, and what does each part do?
5. What is the Spring *application context* / IoC container?
6. What is the difference between having a dependency on the classpath and that feature being *active*?

**Practical**

7. Where does Spring Boot load `application.properties` from, and why externalize configuration at all?
8. What is the Maven Wrapper (`mvnw`) and what problem does it solve?
9. Why does component scanning start from the package of the main class? What happens if you put a `@RestController` in a package *above* it?
10. What does the `spring-boot-maven-plugin` do that a normal jar build doesn't?
11. What does the empty `contextLoads()` test actually verify?

**Follow-ups / deeper**

12. What is the difference between `compile`, `runtime`, `provided`, and `test` dependency scopes? (Hint: the PostgreSQL driver is `runtime`.)
13. Why is Lombok marked `optional` and excluded from the repackaged jar?
14. How would you run the app on a different port without recompiling?
