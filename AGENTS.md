# AGENTS.md — Recruiter Bot Engineering Guide

## 1. Project purpose and source of truth

This repository implements **Build-a-Recruiter-Bot**, the Backend Developer assignment in the refined `Recruiterflow_Internship_Assignments.md` brief.

The refined Markdown assignment is the source of truth. Do not implement the QA Automation or Trip Splitter assignments from the older combined PDF. The work has two assignment deliverables:

1. The Recruiter Bot REST backend: ingest the provided fictional candidates and jobs, store them in a relational database, and rank candidate/job matches in both directions.
2. `sql_detective.sql`: the separate hiring-operations SQL challenge, containing one query for each of the five questions (including the bonus window-function question).

The following enterprise capabilities are secondary requirements and must support—not distract from or replace—the assignment: structured logging and request correlation, health checks and metrics, persistent API usage statistics, validation and consistent errors, tests and maintainable module boundaries, and Swagger UI/OpenAPI documentation.

## 2. Fixed technology and operating constraints

- Language: Kotlin.
- Build: Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, Gradle wrapper).
- Framework: the generated Spring Boot version (currently initialized as Spring Boot **4.1.1**).
- JVM target: Java 21.
- Package/group: `dev.keyup.recruiterbot` / `dev.keyup`.
- Application database: a local SQLite file accessed through the Xerial SQLite JDBC driver.
- Persistence: **hand-written SQL only**. Use Spring JDBC APIs (`JdbcTemplate`, `NamedParameterJdbcTemplate`, or `JdbcClient`) with explicit SQL strings and row mappers.
- Migrations: Flyway if the exact generated Flyway/SQLite versions are compatible. Prove compatibility with a clean database before relying on it.
- API documentation: add `org.springdoc:springdoc-openapi-starter-webmvc-ui` **3.x** for Spring Boot 4; verify the newest stable compatible version before pinning it. Springdoc 2.x targets Spring Boot 3 and must not be used for this Boot 4 project.
- Architecture: use Spring Modulith only to describe and verify useful package/module boundaries. Do not create unnecessary microservices, messaging infrastructure, or distributed workflows.
- Local startup must not require Docker, Redis, a separately running database, a cloud account, a paid service, or a third-party authentication service.

Before adding any dependency, inspect the existing `build.gradle.kts`. Do not duplicate the SQLite driver or starters that Initializr has already included. Let the Spring Boot dependency-management plugin control versions wherever possible. Do not independently upgrade transitive Spring/Flyway/Kotlin dependencies without checking compatibility.

### SQLite/Flyway compatibility gate

Initializr's dependency selection and the manually added SQLite driver may already provide part of the required setup. Confirm the actual resolved dependencies and run the application from a clean state. Current Flyway releases have distributed SQLite support as a separate module in some release lines (for example, `org.flywaydb:flyway-database-nc-sqlite`). Do not blindly add that module with an arbitrary version: its Flyway version must match the Flyway core version resolved by the generated Spring Boot build. If compatible migration support cannot be established cleanly, use a small SQL bootstrap approach rather than leaving the project unable to start. Record the chosen setup in the README.

## 3. Mandatory assignment requirements

### Recruiter Bot

- Seed the 15 provided candidates and 6 provided jobs, including skills, experience, availability, traits/culture keywords, quirk/tagline fields as appropriate.
- Store the information in a relational schema that is easy to query and explain.
- Provide these exact required paths (do not require a `/api/v1` prefix):
  - `GET /jobs/{id}/matches` — candidates ranked for the job, with a numeric score and a short reason.
  - `GET /candidates/{id}/matches` — jobs ranked for the candidate, with a numeric score and a short reason.
- The matching algorithm **must** account for both skill overlap and experience fit. Culture/traits, availability, score weights, and deterministic tie-breaking are design decisions that must be documented and tested.
- Include a README with local installation/startup instructions, curl examples, design trade-offs (especially scoring), and a brief disclosure of AI assistance if used.

### SQL Detective Challenge

- Preserve the supplied challenge schema and seed data as given. Do not redesign its four tables.
- Keep the challenge separate from the Recruiter Bot's `candidates` and `jobs` schema. Use `sql_detective.sql` at the repository root as the independent submission.
- Include one clearly commented query for each question. The bonus question must use a window function.
- Identity resolution for the duplicate-applicant question should account for case and surrounding whitespace in email addresses; do not count multiple interview stages for the same applicant/job as multiple applications.

## 4. Explicitly prohibited approaches

- No JPA, Hibernate entity persistence, Spring Data repositories, Exposed, jOOQ, QueryDSL, ORM, query builder, or library that generates SQL for the application schema.
- No interpolating user-controlled values into SQL. Bind all values using prepared-statement parameters. Table/column identifiers must remain hard-coded, not user-supplied.
- No extra database service, cache service, broker, container runtime, external telemetry server, paid API, or account requirement for normal local development.
- No persistence of computed match results unless a clear requirement emerges. Matching is small and cheap enough to recompute from current source data; use the database for source records and usage events.
- No logging request bodies, authorization headers, passwords, or full candidate/job payloads by default.
- No high-cardinality metrics tags such as candidate IDs, job IDs, request IDs, raw URLs, email addresses, or names.
- No public seed/reseed endpoint merely to demonstrate ingestion. Versioned SQL seed data is sufficient and reduces accidental destructive operations.
- No claims that Actuator metrics are durable historical analytics. Persist usage events separately in SQLite.

## 5. Architecture and package ownership

Keep a modular monolith in the single Spring Boot application. The expected package root is `dev.keyup.recruiterbot`.

Suggested shape (names may change if the same responsibilities remain clear):

```text
src/main/kotlin/dev/keyup/recruiterbot/
  RecruiterbotApplication.kt
  candidate/
    Candidate.kt
    CandidateAvailability.kt
    CandidateCatalog.kt                 # public module facade/query contract
    CandidateRepository.kt              # explicit persistence contract if useful
    JdbcCandidateRepository.kt          # hand-written SQL + row mapping
  job/
    Job.kt
    JobCatalog.kt
    JobRepository.kt
    JdbcJobRepository.kt
  matching/
    MatchResult.kt
    MatchScoreBreakdown.kt
    MatchScoreCalculator.kt             # pure deterministic domain logic
    MatchReasonBuilder.kt
    MatchingService.kt
    MatchingController.kt
    MatchResponse.kt                    # API DTOs, not persistence entities
  usage/
    ApiUsageEvent.kt
    ApiUsageRepository.kt
    JdbcApiUsageRepository.kt
    ApiUsageRecorder.kt
    UsageStatisticsService.kt
    UsageStatisticsController.kt
    UsageStatisticsResponse.kt
  web/
    RequestCorrelationFilter.kt
    ApiExceptionHandler.kt
    ApiProblem.kt                       # if a small custom response wrapper is useful
  config/
    SecurityConfiguration.kt
    OpenApiConfiguration.kt             # only if annotations/properties are insufficient
    ObservabilityConfiguration.kt       # optional custom meters

src/main/resources/
  application.yml
  application-local.yml                 # optional local-only configuration
  db/migration/V1__create_recruiter_bot_schema.sql
  db/migration/V2__seed_candidates_and_jobs.sql
  db/migration/V3__create_usage_telemetry.sql

src/test/kotlin/dev/keyup/recruiterbot/
  matching/MatchScoreCalculatorTest.kt
  matching/MatchingServiceTest.kt
  persistence/SqliteRepositoryIntegrationTest.kt
  web/MatchingApiIntegrationTest.kt
  usage/UsageStatisticsIntegrationTest.kt
  architecture/ModuleArchitectureTest.kt

sql_detective.sql
README.md
IMPLEMENTATION_PLAN.md
AGENTS.md
```

This is a guide rather than a demand to create one file per class. Prefer fewer cohesive classes over trivial wrappers. Do not create empty abstractions solely to match this tree.

### Module boundary rules

- `candidate` owns candidate data and reads it through a stable facade/contract.
- `job` owns job data and reads it through a stable facade/contract.
- `matching` depends on the candidate and job modules but not on their JDBC implementation classes. It combines domain data and computes rankings.
- `usage` owns persistence and aggregation of HTTP usage telemetry. Web/request tracing can call its recorder without using its SQL implementation directly.
- `web` owns common HTTP error formatting and correlation behavior, not matching rules or SQL.
- `config` owns framework wiring/security/OpenAPI configuration, not business logic.
- SQL migrations own initial schema and assignment seed ingestion. Do not seed through repeated application-startup inserts in Kotlin.
- Use Spring Modulith's module verification/test support to prove package dependencies are intentional. Keep the module count proportional to this small project.

## 6. Domain and database rules

### Canonicalization

The Markdown's table has line-wrap/OCR artifacts in a handful of hyphenated words. Normalize those obvious line-wrap splits into the intended canonical skill/keyword terms (for example, `pattern-recognition`, `time-management`, `public-speaking`, `systems-design`, `rapid-prototyping`, `stakeholder-management`, `crisis-management`, `theoretical-analysis`, `resourcefulness`, and `calm-under-pressure`). Use lowercase, trimmed, stable tokens and document the normalization once. Do not silently change the meaning of an input skill.

### Schema direction

Use normalized relational tables instead of storing lists as comma-delimited text:

- `candidates`: identity/name, years of experience, availability, quirk, and timestamps as needed.
- `candidate_skills`: one row per candidate/skill; composite uniqueness and a foreign key.
- `candidate_traits`: one row per candidate/trait; composite uniqueness and a foreign key.
- `jobs`: title, minimum experience, tagline, and timestamps as needed.
- `job_required_skills`: one row per job/required skill.
- `job_culture_keywords`: one row per job/culture keyword.
- `api_usage_events`: durable, minimal request telemetry; keep it separate from business records.

Use explicit IDs in seed data so local examples and tests are deterministic. Use SQLite-compatible DDL, `CHECK` constraints for experience/availability where appropriate, indexes for relationship lookups, and foreign keys. Ensure SQLite foreign-key enforcement is enabled for application connections (not only in one migration connection). Do not use database features that are not supported by the SQLite version bundled with the chosen JDBC driver.

Keep all application queries in named Kotlin repository files or versioned migration SQL. Use `RowMapper`/mapping functions to translate rows into Kotlin types. If reading multiple related collections, prefer a small, fixed number of explicit queries over a join that multiplies rows into a candidate-skill-trait Cartesian product. Never use an ORM.

## 7. Matching contract

Implement the score calculator as pure Kotlin logic with no Spring, SQL, HTTP, or logging dependency. The score must be deterministic for the same candidate/job pair.

A reasonable baseline (adjust only with a clear reason) is:

- Skills: 70% — matched required skills divided by total required skills.
- Experience: 20% — candidate years divided by required years, capped at 100%; define the zero-minimum case defensively.
- Culture/traits: 10% — overlap of candidate traits with job culture keywords divided by the number of job culture keywords.
- Availability adjustment: subtract 0 for immediate, 5 for two weeks, and 15 for not looking; clamp the final score to `[0, 100]`.
- Round the final score to two decimal places using a documented rounding rule.

Treat availability as a soft fit penalty, not a hard exclusion, unless the README explains and tests a different policy. Do not make the algorithm silently discard candidates who are not looking. A score reason should be generated from the same `ScoreBreakdown` used to calculate the score; it should mention matched skills, experience versus the minimum, culture overlap, and an availability note where relevant.

Return the same underlying score for a candidate/job pair in either direction. Sort deterministically: score descending, then a documented stable tie-break (for example matched-skill count descending, culture-match count descending, normalized display name ascending, ID ascending). Do not depend on SQLite row order or hash-set iteration order.

## 8. HTTP/API rules

- Keep the two assignment paths exactly `/jobs/{id}/matches` and `/candidates/{id}/matches`.
- Use REST controllers only for HTTP translation: parse identifiers, call services, and return response DTOs. Controllers must not calculate scores or execute SQL.
- Return `200 OK` with a stable JSON envelope for a valid ID, including an empty ranked list if no matches exist.
- Return `404 Not Found` with the common error schema when a requested job/candidate does not exist. Do not confuse “existing record with zero matches” with “missing record.”
- Use proper HTTP status codes and do not expose stack traces or SQL exception text in responses.
- Document the score scale, weighting, reason fields, ordering, and 404 behavior in OpenAPI/README.
- Swagger UI should run locally at Springdoc's default UI path (typically `/swagger-ui/index.html`) and raw OpenAPI JSON at `/v3/api-docs`.
- Because Spring Security is on the classpath, define an explicit security filter chain. Do not allow Boot's generated development password or default auto-configuration behavior to become an undocumented accidental API contract.

## 9. Validation and consistent errors

Use Jakarta Bean Validation where there are incoming values and validate query parameters/identifiers at the boundary. Kotlin annotation use-site targets (such as `@field:NotBlank`) should be used where needed. Invalid identifiers, malformed dates/ranges on usage-statistics requests, invalid JSON, missing resources, unauthenticated requests, and authorization failures should return useful but non-sensitive structured errors.

Use Spring's `ProblemDetail` (or a small stable RFC 9457-style response object) plus a machine-readable error code. Recommended fields are `type`, `title`, `status`, `detail`, `instance`, `code`, `requestId`, and optional `fieldErrors`. Preserve normal framework behavior where it is already correct rather than building a large custom error framework.

## 10. Logging, correlation and metrics

- Use the default SLF4J/Logback stack. Do not add another logging facade.
- Prefer Spring Boot structured JSON console logging if the generated Boot version supports it; otherwise use a clear text pattern for local readability. Make this a configuration decision, not custom logging infrastructure.
- A request-correlation filter should accept a safe, length-limited incoming `X-Request-ID` or generate a UUID; echo it in the response, put it in MDC for the request, and always remove MDC state in `finally`.
- Validate request IDs to prevent log injection. Never trust arbitrary incoming header content as an unrestricted log field.
- Log request completion with method, route template (when known), status, duration, and request ID. Avoid duplicate access logs from multiple layers. Log unexpected exceptions with stack traces once at the right boundary.
- Add Micrometer counters/timers for match requests, calculation duration, and result count if they add useful value. Use low-cardinality tags only (direction/status/result band, not IDs or raw paths).
- Expose a limited Actuator set such as health, info, and metrics. Do not add a Prometheus server or require Grafana merely to see metrics.

## 11. Persistent usage statistics

Actuator is operational telemetry; it is not the durable usage-reporting feature. Implement SQLite-backed request events for application API routes and an admin-only aggregation endpoint, for example `GET /api/v1/admin/usage?from=YYYY-MM-DD&to=YYYY-MM-DD`.

Record only what is needed: UTC completion time, request ID if operationally useful, HTTP method, route template, status code, and duration in milliseconds. Store route templates such as `/jobs/{id}/matches`, not raw URL paths with IDs. Do not store query strings, bodies, names, candidate details, IP addresses, authentication headers, or secrets.

Exclude `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`, and the usage-report endpoint itself from the event counts, unless the README explicitly documents a different policy. Count failed requests where possible. Handle a usage-recording failure without replacing a successful business response with a telemetry exception; emit an operational warning. Document that synchronous SQLite writes are appropriate for the small local workload but are not a high-throughput analytics pipeline.

The usage response should provide a date range, total requests, error count/rate, status breakdown, route breakdown, and useful latency aggregates. Define whether the end date is inclusive and use UTC consistently. Bound the report range to avoid unbounded scans. Protect this endpoint using the admin role.

## 12. Security and local usability

- Keep the app usable with only the JDK/Gradle wrapper and the SQLite JDBC file; no external auth provider.
- Configure stateless HTTP authentication explicitly if using Spring Security. Use clearly documented local-only demo credentials for local development and environment-supplied secrets for non-local profiles. Never use committed credentials as production defaults.
- Permit the OpenAPI/Swagger resources to load, and define how Swagger UI authenticates API calls. Protect the usage-statistics/admin endpoint.
- Permit a minimal health/info view if desired; protect operational metrics and diagnostic details.
- Avoid enabling CORS for all origins without a frontend requirement. There is no frontend in this assignment.
- Disable CSRF only when the application is deliberately stateless and uses non-cookie API authentication; explain the choice in configuration/README.
- Do not claim that this take-home is production-hardened merely because Spring Security is present.

## 13. Testing expectations

Minimum test layers:

1. Pure unit tests for scoring, rounding, score clamping, missing/partial/full skill overlap, experience below/at/above minimum, culture overlap, availability adjustments, reason generation, and tie-breaking.
2. SQLite JDBC integration tests using a temporary file or a correctly isolated in-memory connection. Verify migrations, seed counts, foreign keys, row mapping, both lookup directions, and missing records. No Testcontainers or Docker.
3. HTTP integration tests for both required endpoints, score/reason fields, sorting, invalid/missing identifiers, consistent error response, auth rules, OpenAPI generation, and Swagger access.
4. Usage-stat tests for persistence across application restarts/reopen, aggregation by date/route/status, exclusions, and non-fatal telemetry errors.
5. Spring Modulith architecture test(s) proving there are no accidental package cycles or dependencies on another module's implementation classes.
6. SQL challenge validation: preserve all supplied fixture data, run all five queries against SQLite, and test edge cases such as case-insensitive email identity and multiple interview stages within one application.

Tests must not rely on public internet APIs, hosted systems, current date assumptions, execution order, or a developer's existing SQLite file. Use deterministic test fixtures and temporary database paths. Tests should fail loudly rather than silently skip schema initialization.

## 14. Coding standards

- Prefer immutable Kotlin `data class` values for domain/read DTOs; avoid exposing persistence-mutable collections.
- Use `Long` for SQLite integer IDs and `Int` for year/status counts where ranges are known.
- Make nullability explicit. Avoid `!!`; do not suppress compiler warnings without a documented reason.
- Keep SQL parameterized. Name SQL constants by repository operation where that makes code easier to inspect.
- Separate domain calculation from persistence and HTTP formatting.
- Avoid broad catch-all exception handling. Translate known failures at the correct boundary and let unexpected failures be logged once.
- Use UTC `Instant` or epoch milliseconds for telemetry timestamps. Do not use locale-dependent date formatting in persistence.
- Use explicit, documented normalization for skills and traits; avoid fuzzy matching unless the README explains its impact.
- Do not overbuild pagination, CRUD endpoints, background schedulers, caches, message brokers, or frontend assets without a concrete requirement.

## 15. Required developer workflow

Before declaring a phase complete:

1. Inspect the changed code and migration SQL.
2. Run `./gradlew test` (or `gradlew.bat test` on Windows).
3. Run `./gradlew check` and ensure compilation/architecture tests pass.
4. Start against a new, temporary SQLite file and verify migrations + seed data.
5. Exercise both required curl examples and confirm the score/reason payload.
6. Verify `/v3/api-docs`, Swagger UI, health, metrics, and the usage report according to security policy.
7. Confirm there are no accidental database files, credentials, logs, or build outputs staged in Git.
8. Update README and `IMPLEMENTATION_PLAN.md` if actual decisions differ from this plan.

## 16. Completion definition

The project is complete when the two exact matching endpoints work against seeded SQLite data; matching is deterministic and explainable; all five SQL Detective queries are in the required file and execute against the provided dataset; a clean checkout can build/test/run without external infrastructure; logs, request IDs, health/metrics, persistent usage reports, consistent errors, security rules, and Swagger UI have tests/docs; and the README explains decisions and trade-offs honestly.
