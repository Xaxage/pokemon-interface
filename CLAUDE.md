# CLAUDE.md

## Project

A Spring Boot RESTful API that integrates with the external **PokeAPI** (https://pokeapi.co/docs/v2),
replicates Pokemon data into a local relational store, and exposes it — with proprietary fields and
user authentication — to a React frontend. Built with Hexagonal Architecture.

## Build & test

Maven project using the Maven Wrapper. On Windows use `mvnw.cmd`; the examples below use the POSIX wrapper.

```bash
./mvnw clean install        # full build + tests
./mvnw compile              # compile only
./mvnw test                 # run all tests
./mvnw spring-boot:run      # run the app locally
```

Run a single test class or method:

```bash
./mvnw test -Dtest=PokemonEnumerationAcceptanceTest
./mvnw test -Dtest=SomeClass#someMethod
```

## Stack

- Java 21, Spring Boot 4.1.1 (Spring MVC, Spring Data JPA, Bean Validation, Spring Security).
- MariaDB for the datastore; In-memory for the test DB.
- Spring `RestClient` for outbound PokeAPI calls.
- Caffeine (Spring Cache abstraction) for caching PokeAPI responses.
- JWT for stateless authentication (protected vs. public routes).
- Config lives in `src/main/resources/application.yaml`.
- Base package: `com.xaxage.pokemoninterface`.

## Coding Conventions

### Java 21

Records for value objects and DTOs, sealed interfaces, pattern matching. No Lombok — records replace it.

### REST & Spring

Constructor injection only (no field `@Autowired`).
`@Valid` on request bodies. `201` create, `200` query, `400` validation, `401/403` auth, `404` not found.
Domain exceptions for business-rule violations; map to HTTP in the controller/`@ControllerAdvice` only.
Never swallow exceptions or leak infrastructure details. Consistent error response body across endpoints.

## Architecture: Hexagonal (Ports & Adapters)

```
domain/        — Pure Java. NO Spring, NO jakarta.persistence, NO framework deps.
  model/       — entities and value objects
  service/     — business rules & validation
application/   — use-case orchestration only, no business logic
  port/in/     — use-case interfaces (driving)
  port/out/    — PokemonRepositoryPort, PokeApiPort (driven)
adapter/
  in/web/      — @RestController + DTOs
  out/persistence/ — JPA repositories and entities (NOT the domain model)
  out/pokeapi/ — RestClient-based PokeAPI client implementing PokeApiPort
```

Dependencies flow inward: `adapter → application → domain`.
Domain NEVER imports `org.springframework` or `jakarta.persistence`.
Controllers NEVER contain business logic. NEVER import adapter classes from domain.
The business-logic layer stays independent of both the web and persistence adapters.

## Development Process — ATDD + TDD

**Outer loop (ATDD):**

1. Pick the next acceptance criterion from the user stories above.
2. Write ONE acceptance test (`@SpringBootTest` + MockMvc).
3. Run the inner TDD loop until it passes.
4. Move to the next AC. NEVER skip ahead.

**Inner loop (TDD) — ONE test at a time:**

1. Write ONE failing test (RED).
2. Minimum code to pass (GREEN).
3. Refactor on green (REFACTOR).
4. Run ALL tests, then the next test.

NEVER write multiple tests before implementing.
NEVER modify a failing test to make it pass — fix the production code.

## Testing Standards

- **Acceptance** — `.../acceptance/`, `@SpringBootTest` + MockMvc. External PokeAPI stubbed (WireMock / MockWebServer).
- **Domain** — `.../domain/`, plain JUnit + AssertJ, NO Spring.
- **Repository** — `.../adapter/out/persistence/`, `@DataJpaTest`.
- **Web** — `.../adapter/in/web/`, `@WebMvcTest`.

Tests are executable specifications, not just checks.
Use `@Nested` classes for grouping — even in unit tests.
`@DisplayName` with business language on every class and method.
Use AssertJ for all assertions. Inline test data per test; no shared fixtures.
Never call the real PokeAPI in tests — always stub the outbound port or HTTP.
