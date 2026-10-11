# Recruiter Bot — Detailed Implementation Plan

> **Status:** Planning baseline. Revise checkboxes and decisions as the implementation progresses.
>
> **Primary source:** `Recruiterflow_Internship_Assignments.md` (the refined Backend Developer assignment). This plan deliberately ignores the unrelated QA Automation and Trip Splitter sections in the earlier combined PDF.

## 0. Goals, priorities, and scope boundaries

### 0.1 Primary objective: assignment compliance

Build a small, explainable Kotlin REST backend that loads the supplied 15 fictional candidates and 6 jobs into SQLite and ranks matches in both directions. The scoring algorithm must account for skill overlap and experience fit. All SQL used for the Recruiter Bot's persistence must be authored manually; ORM and query-builder persistence is explicitly disallowed.

The second primary deliverable is the independent SQL Detective Challenge in `sql_detective.sql`. Preserve its four-table schema and seed data, and include five commented queries: four required questions plus the bonus window-function query.

### 0.2 Secondary objectives: enterprise-style quality

Add these capabilities without introducing separate infrastructure:

1. Structured application logging and request correlation.
2. Liveness/readiness-style health reporting and operational metrics through Actuator.
3. Persistent usage reporting backed by SQLite.
4. Request validation and consistent error payloads.
5. Layered tests and Spring Modulith package-boundary verification.
6. OpenAPI specification and interactive Swagger UI through `springdoc-openapi`.
7. Explicit, comprehensible Spring Security configuration that remains easy to run locally.
8. SQL migrations, indexes, constraints, and reproducible seed data.

### 0.3 Non-goals

Do not build a frontend, distributed services, Redis caching, a message broker, Docker orchestration, Prometheus/Grafana deployment, cloud logging, externally hosted analytics, an identity-provider integration, or an AI/LLM-based semantic matching system. They are unnecessary to the assignment and conflict with the preference for a low-friction local setup.

### 0.4 Acceptance criteria at a glance

- [ ] A new checkout builds with the Gradle wrapper and Java 21.
- [ ] A fresh SQLite database is created and populated reproducibly.
- [ ] `GET /jobs/{id}/matches` returns a stable ranked candidate list with numeric scores and reasons.
- [ ] `GET /candidates/{id}/matches` returns ranked jobs with scores and reasons.
- [ ] The two directions produce the same score for the same candidate/job pair.
- [ ] Missing parent IDs return a consistent `404`; malformed inputs return appropriate client errors.
- [ ] The algorithm accounts for required skills and minimum experience; optional criteria are explained.
- [ ] The five SQL Detective queries execute against the exact supplied fixture; the bonus uses a window function.
- [ ] Logs include correlation IDs and no secrets/payloads; Actuator health/metrics are available according to access policy.
- [ ] API usage events persist in SQLite and aggregate to sensible reports.
- [ ] Swagger UI works locally, including its authentication behavior.
- [ ] Tests, README, and assignment trade-off notes are complete.

---

## 1. Confirm the generated project and dependency compatibility

### 1.1 Inspect Initializr output before editing

Review `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` if present, `application.yml`, and the generated application class. The project was initialized with Kotlin, Gradle Kotlin DSL, Java 21, group `dev.keyup`, artifact `recruiterbot`, and dependencies selected through Initializr for SQLite, Modulith, JDBC, Validation, Actuator, Flyway, DevTools, Configuration Processor, and Security. The SQLite JDBC driver was also added manually, so first check whether the driver is already declared and remove any duplicate dependency.

Keep Spring Boot's dependency management in charge of dependency versions. Avoid hardcoding a separate version for every Spring-related module.

### 1.2 Add Swagger UI

Add the Spring MVC springdoc UI starter in the Gradle Kotlin DSL. Because the project uses Spring Boot 4.1.1, use a compatible **springdoc-openapi 3.x** release—not the 2.x line intended for Boot 3. The current checked release is springdoc 3.1.0, whose release notes upgrade to Spring Boot 4.1.0; Boot 4.1.1 is the same minor line, but still verify the newest stable compatible 3.x release and run a startup/API-docs smoke test before pinning it.

Expected dependency family:

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
```

Do not add a separate Swagger UI JavaScript install or Node.js frontend. The starter serves the UI from the Spring Boot application.

### 1.3 Verify SQLite and Flyway together early

SQLite is an embedded/file-based database in this architecture, not a server. The app should start with a JDBC URL such as `jdbc:sqlite:./data/recruiterbot.db` (after the local data directory is created, or use a path whose parent already exists). Confirm the exact path syntax and the driver's foreign-key configuration.

Flyway SQLite support is version-sensitive. Current Flyway releases may use a separate module named `org.flywaydb:flyway-database-nc-sqlite`. Before adding it:

1. Inspect the actual Flyway core version resolved by the generated Boot dependency management.
2. Find a SQLite module with a matching compatible release; do not combine unrelated Flyway versions.
3. Run a clean start against a brand-new file and verify that migrations are applied.
4. If that dependency combination is not compatible, use a minimal SQL bootstrap strategy for the current project rather than blocking the app on migration tooling. Explain the trade-off in the README.

Do not create a custom SQLite connection pool or custom datasource until a concrete driver/configuration limitation requires it. If the default datasource is used, ensure `PRAGMA foreign_keys=ON` equivalent behavior is configured for every connection, not merely executed once on a migration connection.

### 1.4 Add only missing test support

Check whether the generated project has Spring Boot Test and JUnit support. Add `spring-security-test` for MockMvc/security integration tests if it is not already present. Add Spring Modulith test support only if the generated Modulith dependency set does not already supply it. Prefer temporary local SQLite files for integration tests; do not introduce Testcontainers/Docker.

**Deliverable:** a dependency graph that compiles before business implementation begins.

**Gate:** `./gradlew test` succeeds with the generated project, an empty SQLite file can be initialized, and `/actuator/health` responds under the initial security configuration.

---

## 2. Proposed architecture and object inventory

Use a **modular monolith**. The assignment is intentionally small; the point of the architecture is clarity, not maximum class count. Package boundaries are conceptual modules in one JVM process.

### 2.1 Application entry point

**Object:** `RecruiterbotApplication`

**Package:** `dev.keyup.recruiterbot`

**Responsibility:** Boot the application and declare the package root from which Spring Modulith can inspect modules. It should contain no business logic or manual database seeding.

**Acceptance:** `contextLoads` succeeds against an isolated test database; module verification discovers the intended modules.

### 2.2 Candidate module

**Domain object:** `Candidate`

Suggested fields:

- `id: Long`
- `name: String`
- `skills: Set<String>` (canonicalized lowercase tokens)
- `experienceYears: Int`
- `availability: CandidateAvailability`
- `traits: Set<String>`
- `quirk: String`

**Enum:** `CandidateAvailability`

Values should be stable internal tokens such as `IMMEDIATE`, `TWO_WEEKS`, and `NOT_LOOKING`. Map the assignment labels to these canonical values during seeding. Do not let the spelling/case of source labels leak into scoring comparisons.

**Read facade:** `CandidateCatalog` (or a similarly named public contract)

Methods should cover the actual use cases, for example `findById(id)` and `findAllForMatching()`. Return fully populated candidates, including their skill and trait sets. Missing IDs should be distinguishable from empty collections.

**Persistence contract/implementation:** `CandidateRepository` / `JdbcCandidateRepository`

Responsibilities:

- Execute explicitly written SQL through Spring JDBC.
- Map scalar candidate columns through a `RowMapper` or focused mapping function.
- Load skills and traits using explicit second queries or another clear non-Cartesian approach.
- Use bound parameters for IDs.
- Throw/return a clear “not found” representation only at the persistence/service boundary; HTTP semantics belong to the API layer.

Do not annotate `Candidate` as a JPA entity and do not add Spring Data repositories.

### 2.3 Job module

**Domain object:** `Job`

Suggested fields:

- `id: Long`
- `title: String`
- `requiredSkills: Set<String>`
- `minExperienceYears: Int`
- `cultureKeywords: Set<String>`
- `tagline: String`

**Read facade:** `JobCatalog`

Use-case methods such as `findById(id)` and `findAllForMatching()` return populated jobs. The service should not need direct access to `JdbcJobRepository`.

**Persistence contract/implementation:** `JobRepository` / `JdbcJobRepository`

Responsibilities mirror the candidate side: hand-written SQL, bind parameters, map base columns, load required skills and culture keywords, and distinguish a missing job from a job with no associated keywords.

### 2.4 Matching module

This module owns scoring, explanations, stable sorting, and the required REST endpoints. It depends on public candidate/job read APIs and must not know how either module's SQL is implemented.

**Value object:** `MatchScoreBreakdown`

Suggested fields:

- `matchedSkills: Set<String>`
- `missingSkills: Set<String>`
- `skillCoveragePercent: BigDecimal` or a deterministic numeric type
- `candidateExperienceYears: Int`
- `minimumExperienceYears: Int`
- `experienceFitPercent: BigDecimal`
- `matchedCultureKeywords: Set<String>`
- `cultureFitPercent: BigDecimal`
- `availabilityPenalty: BigDecimal`
- `finalScore: BigDecimal`

Use one consistent rounding/scale policy. Keep the score breakdown rich enough to power both the API response and the reason text. Avoid calculating the score a second time while formatting the response.

**Value object:** `CandidateMatch` / `JobMatch` or one generic internal `MatchResult<T>`

Represent the matched side, score breakdown, and generated reason. It is acceptable to use direction-specific response DTOs externally even if the calculation result is shared internally.

**Pure service:** `MatchScoreCalculator`

Responsibilities:

1. Intersect candidate skills with job required skills using canonical tokens.
2. Calculate skill coverage from distinct required skills.
3. Calculate experience fit relative to the minimum required years, with explicit handling for zero minimum experience.
4. Calculate culture/trait overlap.
5. Apply availability policy.
6. Clamp to `[0, 100]` and round consistently.
7. Return one breakdown object.

This class must be pure Kotlin and easy to test without Spring, database access, or HTTP setup. It should not log every pair it calculates.

**Pure formatter:** `MatchReasonBuilder`

Generate short, truthful explanations from the breakdown. Example shape: “Matched 3/3 required skills; 8 years’ experience meets the 3-year minimum; 1/2 culture keywords matched; available immediately.” If a skill is missing, name it or summarize the count. Do not claim a skill match that the score calculator did not find. Keep reasons short enough for API consumers to scan.

**Application service:** `MatchingService`

Methods:

- `findCandidatesForJob(jobId)` — confirm the job exists; load candidate profiles; calculate score/breakdown/reason for each; sort deterministically; return all ranked candidates.
- `findJobsForCandidate(candidateId)` — confirm the candidate exists; load jobs; calculate/sort the same pairs identically; return all ranked jobs.

A single shared pair-scoring function should be used by both methods so reverse matching cannot drift. The current data set is only 15 × 6 records, so calculate on demand and do not cache or persist match lists.

**HTTP controller:** `MatchingController`

Required mappings, without an extra version prefix:

- `GET /jobs/{id}/matches`
- `GET /candidates/{id}/matches`

Controller responsibilities: validate positive IDs, call `MatchingService`, map the result to DTOs, and return HTTP responses. No SQL or scoring logic belongs here.

**Response DTOs:**

`JobMatchesResponse` can contain `jobId`, `jobTitle`, and `matches: List<CandidateMatchResponse>`. Each candidate match can contain `candidateId`, `candidateName`, `score`, `reason`, `scoreBreakdown`, `matchedSkills`, `missingSkills`, `experienceYears`, `minimumExperienceYears`, `matchedCultureKeywords`, and normalized availability.

`CandidateMatchesResponse` can contain `candidateId`, `candidateName`, and `matches: List<JobMatchResponse>`. Each job match can contain `jobId`, `jobTitle`, `score`, `reason`, and the same explanation fields relevant to the job.

The response does not need to expose private implementation details or raw database rows. Keep JSON field names stable and document the `0–100` score scale.

### 2.5 Usage/telemetry module

Usage telemetry should be independent of the matching score and candidate/job source data.

**Value object:** `ApiUsageEvent`

Suggested fields:

- `occurredAtEpochMillis: Long` (UTC instant represented as epoch milliseconds)
- `requestId: String?`
- `method: String`
- `routeTemplate: String` (e.g. `/jobs/{id}/matches`)
- `statusCode: Int`
- `durationMillis: Long`

Do not persist body, query string, authentication values, IP address, name, email, or candidate/job IDs. The route template avoids cardinality and data leakage from concrete URLs.

**Recorder:** `ApiUsageRecorder`

A narrow entry point called at request completion. It should not throw out through the response-processing path if telemetry persistence fails. Failures are logged at warning/error level with a correlation ID, but the primary API result is preserved when practical.

**Repository:** `ApiUsageRepository` / `JdbcApiUsageRepository`

Responsibilities: insert events through parameterized SQL and aggregate by time range, route, and status. Do not read the entire event table into memory just to group it.

**Service:** `UsageStatisticsService`

Responsibilities: validate/normalize date range; use UTC consistently; return totals and aggregates; define end-date inclusivity; cap the allowed reporting window; avoid counting its own report endpoint and diagnostic endpoints.

**Controller:** `UsageStatisticsController`

Recommended secondary endpoint: `GET /api/v1/admin/usage?from=YYYY-MM-DD&to=YYYY-MM-DD`. It is not part of the required assignment API and therefore may use a versioned admin path. Restrict it to `ADMIN`.

**Response:** `UsageStatisticsResponse`

Suggested fields: `from`, `to`, `totalRequests`, `errorRequests`, `errorRate`, `statusBreakdown`, `routeBreakdown`, and latency aggregates (`mean`, `max`, and/or percentile if intentionally implemented). Keep the first implementation understandable and avoid promise of production-grade analytics.

### 2.6 Shared web objects

**`RequestCorrelationFilter`**

- Read `X-Request-ID` only if it matches a restrictive character pattern and length bound; otherwise generate a UUID.
- Add it to MDC under one agreed key and echo it in a response header.
- Capture start time and, after request completion, use the matched route template when available.
- Remove MDC values in a `finally` block to prevent thread reuse from leaking one request's ID into another.
- Exclude Swagger assets, OpenAPI JSON, Actuator, and the usage summary endpoint from persisted usage events.
- Record completed HTTP outcomes, including errors where possible. A request rejected before MVC route resolution may use a fixed label such as `UNMATCHED_OR_BLOCKED`, never an unsanitized URL.

Choose a single mechanism for request completion and avoid double counting. If a servlet filter cannot reliably see the final exception/status with the desired behavior, use a carefully ordered filter plus error handling; test the actual behavior rather than assuming it.

**`ApiExceptionHandler`**

Map known exceptions into one stable, non-sensitive response format. Support validation/binding failures, bad date ranges, malformed JSON, entity-not-found, authentication/authorization, and a generic internal error. Never return stack traces or raw SQL driver messages.

**`ApiProblem`** (optional)

Use Spring's `ProblemDetail` if it covers the response needs; otherwise a small project DTO may add `code`, `requestId`, and field-level details. Do not implement both formats inconsistently across controllers.

### 2.7 Configuration objects

**`SecurityConfiguration`**

Create an explicit `SecurityFilterChain`. A reasonable local demo policy is:

- Allow Swagger UI assets and `/v3/api-docs/**` so the docs render.
- Allow `/actuator/health` and minimal `/actuator/info` if desired.
- Require a normal local user for matching endpoints.
- Require an admin role for `/api/v1/admin/**` and sensitive operational metrics.
- Use stateless HTTP authentication and configure unauthorized/forbidden responses consistently.
- Provide documented, local-only credentials for development; use environment variables for non-local profile secrets and do not commit production credentials.

Swagger UI needs to describe and perform authentication for secured requests. Add an HTTP Basic security scheme to OpenAPI and annotate protected operations (or apply it globally if all business APIs are protected). Do not allow requests to fail mysteriously because the security starter was selected but its behavior was never configured.

**`OpenApiConfiguration`** (optional)

Supply an `OpenAPI` bean with title, description, version, and security scheme if annotations/properties do not provide a sufficient description. Document error responses, score scale, sort order, 404 behavior, and examples. Avoid hand-maintaining a separate static OpenAPI JSON file.

**`ObservabilityConfiguration`** (optional)

Define custom Micrometer meters only where useful. Keep tags low-cardinality. Avoid a new metrics backend dependency unless a later requirement calls for it.

---

## 3. Database design in detail

### 3.1 Application tables

All application DDL must be SQLite-compatible and hand-written. Treat schema details as a design choice; the suggested tables meet the assignment while keeping multi-valued fields queryable.

#### `candidates`

| Column | Type / rule | Purpose |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY` | Stable candidate identifier used by the URL |
| `name` | `TEXT NOT NULL` | Display name from the seed data |
| `experience_years` | `INTEGER NOT NULL CHECK (experience_years >= 0)` | Experience-fit input |
| `availability` | `TEXT NOT NULL CHECK (...)` | `IMMEDIATE`, `TWO_WEEKS`, or `NOT_LOOKING` |
| `quirk` | `TEXT NOT NULL` | Retain the assignment's fictional personality detail |
| `created_at` | UTC text or SQLite UTC timestamp | Optional audit field |
| `updated_at` | UTC text or SQLite UTC timestamp | Optional audit field |

Do not add optimistic-lock versions or soft-delete flags without a feature that uses them.

#### `candidate_skills`

- `candidate_id INTEGER NOT NULL` references `candidates(id)` with deletion cascade.
- `skill TEXT NOT NULL` contains one canonical token.
- Composite primary key or unique constraint: `(candidate_id, skill)`.
- Add an index on `skill` if matching/query inspection uses it in reverse lookup.

#### `candidate_traits`

- `candidate_id INTEGER NOT NULL` references `candidates(id)` with deletion cascade.
- `trait TEXT NOT NULL` contains one canonical trait.
- Composite primary key/unique constraint: `(candidate_id, trait)`.

#### `jobs`

| Column | Type / rule | Purpose |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY` | Stable job identifier |
| `title` | `TEXT NOT NULL` | Display title |
| `min_experience_years` | `INTEGER NOT NULL CHECK (min_experience_years >= 0)` | Experience-fit input |
| `tagline` | `TEXT NOT NULL` | Retain job tagline from the assignment |
| `created_at` | Optional timestamp | Audit metadata |
| `updated_at` | Optional timestamp | Audit metadata |

#### `job_required_skills`

- `job_id INTEGER NOT NULL` references `jobs(id)` with deletion cascade.
- `skill TEXT NOT NULL` contains one canonical required-skill token.
- Composite uniqueness `(job_id, skill)`.
- Index on `skill` if it helps explain/optimize overlap lookups.

#### `job_culture_keywords`

- `job_id INTEGER NOT NULL` references `jobs(id)` with deletion cascade.
- `keyword TEXT NOT NULL` contains one canonical culture keyword.
- Composite uniqueness `(job_id, keyword)`.

#### `api_usage_events`

| Column | Suggested form | Purpose |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY` | Event identity |
| `occurred_at_epoch_ms` | `INTEGER NOT NULL` | Comparable UTC time for bounded queries |
| `request_id` | `TEXT` | Optional operational correlation to logs |
| `method` | `TEXT NOT NULL` | HTTP method |
| `route_template` | `TEXT NOT NULL` | Low-cardinality route label |
| `status_code` | `INTEGER NOT NULL` | HTTP outcome |
| `duration_ms` | `INTEGER NOT NULL CHECK (duration_ms >= 0)` | Response latency |

Add an index on `occurred_at_epoch_ms` and an index such as `(route_template, occurred_at_epoch_ms)` if query plans justify it. No separate DB is needed.

### 3.2 Why normalize list fields?

Skills and keywords are set-valued concepts used by a scoring algorithm. A many-to-many style table prevents inconsistent comma separators and makes duplicate canonical tokens impossible. It also gives the schema an understandable relational shape and aligns with the raw-SQL assessment. The data volume is tiny; correctness/readability matter more than micro-optimizing query count.

### 3.3 Migrations and seed workflow

Suggested migration order:

1. `V1__create_recruiter_bot_schema.sql`: candidate/job base tables, relation tables, indexes, foreign keys, checks.
2. `V2__seed_candidates_and_jobs.sql`: stable, explicit candidate/job IDs and their association rows.
3. `V3__create_usage_telemetry.sql`: `api_usage_events` and its indexes.

Migrations should be immutable after they have run in a shared environment. Correct an unpublished local migration if necessary; for any already-used migration, add a forward migration instead of editing history. Seed data should be deterministic and run once through the migration mechanism rather than on every application start.

The assignment allows an endpoint or a seed script for ingestion. This plan uses SQL seed migrations as the seed script, which meets the ingestion requirement without introducing a destructive public `POST /seed` endpoint. Document this choice clearly.

### 3.4 Separate SQL Detective dataset

`sql_detective.sql` is the independent challenge deliverable. Keep its original `recruiters`, `job_postings`, `applicants`, and `interviews` schema and supplied inserts exactly as specified; do not fold these tables into the candidate/job model or rename them.

For a convenient self-contained deliverable, put a clearly separated setup section (original DDL and seed data) before the five commented queries, provided this remains consistent with the assignment's “single .sql file” submission. Do not modify the source fixture to make a query easier. This challenge file can be run separately from the application database; the core app should not require those tables to run.

---

## 4. Seed data mapping and canonical values

### 4.1 Candidate fixtures

Load the 15 names from the refined brief: Sherlock H., Hermione G., Tony S., Leslie K., Ron S., Rick S., Elle W., MacGyver, Sheldon C., Katniss E., Michael S., Olivia P., Ted L., Miranda P., and Dwight S.

Each candidate needs all supplied dimensions:

- Name.
- Set of skills.
- Non-negative experience years.
- Availability.
- Traits.
- Quirk.

Do not discard quirks merely because the first scoring version does not use them. They are part of the source domain data and can be exposed as part of candidate metadata later, but do not let arbitrary prose influence the numeric score.

### 4.2 Job fixtures

Load all 6 jobs from the assignment:

- Backend Detective.
- Rapid Prototyping Engineer.
- Developer Relations Lead.
- Engineering Manager, Chaos Team.
- Incident Commander.
- Sales Engineer.

Each has the provided required-skill set, minimum experience, culture keywords, and tagline. Taglines should be preserved as source data, not included in score calculation.

### 4.3 Canonical terms

The source table has text splits caused by line wrapping. Before inserting associations, standardize broken tokens into their intended words (for example, `pattern-recog nition` to `pattern-recognition`). Use a consistent normalization procedure for hyphens, spaces, and case, with curated seed mappings rather than overly aggressive punctuation stripping. Ensure related forms that are meant to match are identical on both sides—for example `systems-design`, `rapid-prototyping`, and `public-speaking`.

A normalization test should assert that key source skills resolve to canonical strings and that skills from both sides compare as exact tokens.

---

## 5. Matching algorithm: proposed, explainable baseline

The assignment requires skill overlap and experience fit; all other components are optional. Use the following baseline unless implementation/testing exposes a strong reason to change it. Record the final formula in README and Swagger descriptions.

### 5.1 Component calculations

Let:

- `R` = set of required skills for a job.
- `C` = set of skills for a candidate.
- `M = C ∩ R` = matched required skills.
- `E` = candidate's experience years.
- `Emin` = job's minimum experience years.
- `T` = candidate traits.
- `K` = job culture keywords.

**Skill coverage:**

- If `R` is not empty: `skillPercent = 100 × |M| / |R|`.
- If `R` is empty: define a defensive convention (prefer 100 only if there truly are no requirements; alternatively treat data as invalid). Because each seeded job has requirements, this mainly protects future data changes.

**Experience fit:**

- If `Emin > 0`: `experiencePercent = min(100, 100 × E / Emin)`.
- If `Emin = 0`: define `experiencePercent = 100`.

This lets below-minimum candidates receive partial fit rather than being invisibly removed; the score remains a ranking, not a hard eligibility filter. Explain that choice in the README.

**Culture/trait overlap:**

- If `K` is not empty: `culturePercent = 100 × |T ∩ K| / |K|`.
- If `K` is empty: choose a documented neutral convention (for example 100 so an absent optional criterion does not penalize candidates, or 0 and omit its weight through normalized weights). Seeded jobs have culture keywords, so tests should still cover the empty case.

**Weighted subtotal:**

`weighted = 0.70 × skillPercent + 0.20 × experiencePercent + 0.10 × culturePercent`.

**Availability adjustment:**

- Immediate: `0` points penalty.
- Two weeks: `5` points penalty.
- Not looking: `15` points penalty.

`finalScore = clamp(weighted − availabilityPenalty, 0, 100)`, rounded to two decimal places. Use decimal arithmetic where practical and a stated rounding mode such as `HALF_UP`; tests should pin the actual values.

### 5.2 Why these weights?

Skill coverage receives the largest weight because a match should satisfy the job's explicit requirements. Experience fit remains materially important, as explicitly required. Culture fit is a smaller tie-shaper rather than the main determinant. Availability is a modest subtractive penalty that does not outweigh the two required signals in ordinary cases. The specific numbers are design choices, not claims of objective hiring science; they should be defended as transparent, deterministic heuristics.

### 5.3 Reasons and breakdown

Generate the reason from the same breakdown object as the score. Use a predictable explanation order:

1. Skills matched (and optionally missing).
2. Candidate years versus job minimum.
3. Culture keyword overlap.
4. Availability adjustment if nonzero.

Example only: `Matched 2/3 required skills; 4 years vs. 3 required; matched 1/2 culture keywords; available in two weeks (−5 points).`

The displayed score and narrative must not contradict each other. Avoid adjectives such as “excellent” unless defined by explicit, documented thresholds. The assignment asks for a short reason, not opaque AI prose.

### 5.4 Stable sorting

Use a total, deterministic order for matching results:

1. Final score descending.
2. Matched required skill count descending.
3. Matched culture keyword count descending.
4. Candidate/job normalized display name ascending.
5. Stable numeric ID ascending.

If an equivalent alternative is chosen, document it and use the same policy in both directions. Do not rely on a `Set` iteration order, unspecified SQL row ordering, or insertion order.

### 5.5 Algorithm test cases

- All required skills matched versus no skills matched.
- Partial overlap with explicit missing skills.
- Experience below, at, and above the minimum.
- Zero experience minimum / zero required skills defensive behavior.
- No culture overlap, partial culture overlap, all culture keywords matched.
- All three availability values.
- Score clamp and two-decimal rounding boundaries.
- Tied scores result in stable sorting on repeated runs.
- Reverse endpoint returns the exact same pair score and breakdown.
- Reason text references only real matches and accurate year thresholds.

---

## 6. API contract and behavior

### 6.1 `GET /jobs/{id}/matches`

**Purpose:** return candidates ordered from highest fit to lowest for a known job.

**Processing sequence:**

1. Validate that `id` is a positive integer-compatible identifier.
2. Resolve the job; return `404` if it does not exist.
3. Load all seeded candidates and their skills/traits efficiently.
4. Call the shared score calculator for every candidate/job pair.
5. Build reason and response breakdown.
6. Apply the documented stable sorting rule.
7. Return `200 OK` with job metadata and ranked candidates.

**Example curl (authentication depends on the configured local policy):**

```bash
curl -u "$RECRUITERBOT_USER:$RECRUITERBOT_PASSWORD" \
  http://localhost:8080/jobs/1/matches
```

**Acceptance checks:** JSON contains the job ID/title; each item has candidate ID/name, numeric score from 0 to 100, short reason, and auditable breakdown; scores descend; missing job ID produces common `404` error JSON.

### 6.2 `GET /candidates/{id}/matches`

**Purpose:** return jobs ordered from highest fit to lowest for a known candidate.

**Processing sequence:**

1. Validate the ID.
2. Resolve the candidate; return `404` if absent.
3. Load all jobs with required skills and culture keywords.
4. Use the same shared scorer and reason builder as the forward endpoint.
5. Sort by the documented stable rule and return the candidate header plus ranked jobs.

**Example curl:**

```bash
curl -u "$RECRUITERBOT_USER:$RECRUITERBOT_PASSWORD" \
  http://localhost:8080/candidates/1/matches
```

**Acceptance checks:** same pair scores as the forward endpoint; missing candidate returns `404`; a candidate is not removed solely because availability is “not looking” under the baseline soft-penalty policy.

### 6.3 Secondary usage endpoint

Recommended endpoint: `GET /api/v1/admin/usage?from=2026-01-01&to=2026-12-31`.

- Requires administrator role.
- Rejects invalid date values and `from > to` with a field-aware 400 response.
- Enforces an upper bound on requested time range.
- Defines whether `to` is inclusive; implement consistently by translating it to a safe end-exclusive UTC instant.
- Returns zero counts rather than an error when a valid range has no events.
- Excludes the usage endpoint itself from event logging/report counts to avoid recursion and self-inflation.

Do not expose an unprotected endpoint that reveals usage metrics or allows arbitrary SQL filters.

### 6.4 Error contract

Use `ProblemDetail` or a compact RFC 9457-style DTO. Suggested example:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Job 999 was not found.",
  "instance": "/jobs/999/matches",
  "code": "JOB_NOT_FOUND",
  "requestId": "example-correlation-id"
}
```

Document that IDs do not exist without disclosing database/schema internals. Error categories should include at least `CANDIDATE_NOT_FOUND`, `JOB_NOT_FOUND`, `INVALID_REQUEST`, `VALIDATION_FAILED`, `UNAUTHENTICATED`, `FORBIDDEN`, and `INTERNAL_ERROR`. The `requestId` should match the response header/log MDC where one exists.

---

## 7. Logging and request correlation plan

### 7.1 Framework configuration

Use the logging stack already provided by Spring Boot: SLF4J API and Logback implementation. Prefer built-in structured JSON console logging if supported by the selected Boot configuration. Do not add a second logging backend or a cloud collector. Configure package log levels in YAML only when useful; keep SQL bind values and framework debug logs off by default.

### 7.2 Correlation filter lifecycle

For every incoming HTTP request:

1. Validate an incoming `X-Request-ID` using a tight format/length rule; generate a UUID otherwise.
2. Write the ID to MDC and response header.
3. Start a monotonic timer (`System.nanoTime`) for elapsed duration.
4. Continue the filter chain.
5. In the completion path, capture status code and route template if the framework resolved one.
6. Emit one concise completion log with method, route, status, duration, request ID.
7. Record usage telemetry for eligible API calls.
8. Always clear MDC in `finally`.

Do not log request/response bodies. Do not assume that `requestURI` is a safe route template; IDs in a literal URL increase metric cardinality and can expose data.

### 7.3 Logging verification

- Valid incoming request ID is echoed and can be found in logs.
- Missing/invalid request ID produces a generated ID.
- A sequential request on the same thread does not inherit the previous MDC value.
- 404/401/500 responses carry a correlation ID.
- Authorization headers and secrets are absent from logs.
- One HTTP request produces one completion log, not duplicate logs from controller and filter.

---

## 8. Health checks and operational metrics

### 8.1 Actuator endpoints

Expose only the set needed for this local project, such as `health`, `info`, and `metrics`. Keep `env`, `beans`, `configprops`, `heapdump`, `threaddump`, and similar diagnostic endpoints unexposed by default.

- Health should report application process health and whether the database can be reached. If Spring's JDBC health contributor recognizes SQLite, use it; otherwise add a tiny health indicator that performs a constant, read-only database check.
- Info should include a human-readable application name/version/build details where available, not secrets.
- Metrics should expose framework metrics and the custom metrics described below.

Configure health detail visibility deliberately. Public health responses should not disclose connection details or absolute filesystem paths. If `/actuator/metrics/**` is exposed on the main application port, restrict it to administrator access.

### 8.2 Custom meters

Useful low-cardinality meters:

- Counter `recruiterbot.match.requests`, tagged only with `direction=job_to_candidates|candidate_to_jobs`.
- Timer `recruiterbot.match.calculation`, using the same direction tag.
- Counter for API result statuses, if this is not redundant with standard HTTP server metrics.
- A counter for telemetry write failures, if it helps diagnose a degraded feature.

Avoid per-candidate/per-job meter tags. The durable usage endpoint gets identifier-level business data only if explicitly required later; the initial usage design intentionally does not.

### 8.3 Local verification

- `/actuator/health` returns healthy after migrations complete.
- `/actuator/info` returns minimal non-sensitive information.
- `/actuator/metrics` lists the expected meters when authorized.
- If SQLite is unavailable or migration fails, startup/health behavior is clear and the error is logged with actionable context but no secrets.

---

## 9. Persistent API usage statistics

### 9.1 Why use a SQLite table as well as Actuator?

Micrometer/Actuator provides current-process operational measurements. Those measurements are not a durable request ledger by default and may reset on restart. `api_usage_events` is the persistent, local-only data source for usage reports. No external telemetry stack is needed.

### 9.2 Event collection

Record application API requests after completion, including status and duration. Store a normalized route template, not the raw URL. Exclude documentation assets and Actuator endpoints. Define the policy for rejected/security-filter requests explicitly; when no MVC route is known, use one fixed normalized bucket and do not save a raw URI.

Use synchronous inserts for the first version because the sample is small and this avoids a background queue or extra infrastructure. Wrap the recorder so that failure to write analytics doesn't replace a successful matching response. State the throughput trade-off: this is suitable for the assignment/demo, not a high-volume production traffic collector.

### 9.3 Aggregation semantics

Choose and document these semantics before implementing queries:

- Reporting timezone: UTC.
- Time fields stored as epoch milliseconds.
- `from` inclusive; `to` inclusive at calendar-date level, translated internally to the start of the following UTC day as an exclusive upper bound.
- Error response definition: HTTP status `>= 400`.
- Empty period: zero counts and empty breakdown collections.
- Latency: clearly state units in JSON (`...Millis`).
- Route grouping: use stable route templates.

The repository should aggregate in SQL using `COUNT`, `SUM(CASE WHEN...)`, `GROUP BY`, and constrained time predicates. Calculate a simple percentage in Kotlin with a guard against division by zero. Only implement percentiles if they can be described/tested honestly; mean/max latency are sufficient for the first pass.

### 9.4 Retention

The assignment dataset and request volume are tiny. Do not add a scheduled retention process as a first phase. Document how the event table can be cleared locally or add a manual SQL note. If a configurable bounded retention job is later justified, keep it inside the process and avoid introducing an external scheduler. Any deletion must not affect candidate/job data.

---

## 10. Spring Security plan

Security is a secondary quality objective, but Spring Security is already present in the selected dependencies; leaving its default behavior implicit is not acceptable.

### 10.1 Initial policy

- Explicit filter chain.
- Stateless HTTP authentication suitable for a local REST demo.
- `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, and `/v3/api-docs.yaml` allowed so documentation loads.
- Minimal `/actuator/health` and optionally `/actuator/info` allowed; sensitive actuator endpoints restricted.
- Matching endpoints require a local authenticated user (or use documented read-only access if a conscious decision is made to make them public for the take-home; document and test that choice).
- Usage-statistics endpoint requires an admin role.
- Unauthorized/forbidden responses use the same general error style where practical.

### 10.2 Credential profile

Keep local startup straightforward. A `local` profile may provide clearly marked demo-only credentials; never describe them as safe outside localhost. Non-local configuration must accept credentials/secrets from environment variables and must not silently fall back to committed values. Do not add an external identity provider or user-registration database.

For ease of use, a documented HTTP Basic setup works well with Swagger UI's “Authorize” action. Store only an encoded password in the in-memory user details service when feasible. Do not log credentials.

### 10.3 Security tests

- No credentials receives 401 from protected matching endpoint.
- A valid normal user can call matching endpoints.
- A normal user receives 403 for admin usage stats.
- Admin can access usage stats.
- OpenAPI JSON and Swagger static resources load without credentials.
- Swagger's generated security scheme clearly informs the caller how to authorize.
- Secret values do not appear in logs, errors, Actuator info, or committed example configuration.

---

## 11. OpenAPI and Swagger UI

### 11.1 API documentation fields

Set API metadata to:

- Title: `Recruiter Bot API`.
- Description: candidate/job matching service built for the RecruiterFlow backend assignment; scores are deterministic heuristic scores from 0 to 100, not a guarantee of hiring suitability.
- Version: `1.0.0` or another project version tied to the README.

Use OpenAPI annotations where they improve clarity, without cluttering every line of business code.

### 11.2 Document operations

For both matching operations, specify:

- Path identifier type and positivity expectation.
- 200 response schema and illustrative response with score/reason.
- 401/403 according to configured security.
- 404 for missing candidate/job.
- Score range, component meanings, weight formula, availability penalty, and tie-break order.

For usage stats, document date format, inclusive range policy, maximum range, required admin role, zero-event behavior, and the units of latency values.

### 11.3 Verify runtime paths

- Swagger UI: typically `http://localhost:8080/swagger-ui/index.html`.
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`.

The exact UI redirect can vary by Springdoc configuration. Test the actual URL against the chosen springdoc 3.x release and write that path into README. Ensure Spring Security permits UI resources/docs but does not accidentally make the admin endpoint public.

---

## 12. SQL Detective Challenge implementation plan

This is its own deliverable, not a feature of the match-scoring API.

### 12.1 File structure

At repository root:

```text
sql_detective.sql
```

Recommended organization inside the file:

1. A banner comment explaining this is Part 2 of the assignment.
2. Original `CREATE TABLE` statements and fixture `INSERT`s, if including setup makes the file reproducible as a self-contained SQLite script.
3. `-- Question 1`, query.
4. `-- Question 2`, query.
5. `-- Question 3`, query.
6. `-- Question 4`, query.
7. `-- Question 5 (Bonus; window function)`, query.

Keep the schema and seed data unchanged from the refined brief. Do not rely on hidden prior database state.

### 12.2 Query 1 — Open job postings and recruiter

Join `job_postings` to `recruiters` through `recruiter_id`; filter status to the exact open value; project posting title/ID and recruiter name (and useful posting metadata). Use an explicit `ORDER BY` for repeatable output.

**Validation:** only postings currently marked `open` appear, with the correct owning recruiter.

### 12.3 Query 2 — Final-stage counts by posting

Start from every `job_postings` row and `LEFT JOIN` final-stage interviews so postings with zero finals still appear. Count distinct applicants who reached `Final`, avoiding accidental duplicate counts from multiple interview rows. Group by posting ID/title and use deterministic ordering.

**Validation:** zero-final jobs are present with count 0; a candidate with earlier Screen/Technical records is not counted as a finalist unless a `Final` record exists.

### 12.4 Query 3 — Effective duplicate people across postings

The dataset has no standalone application-to-posting relation; interviews link applicants to postings and therefore act as the available evidence that a person participated in a posting's process. Normalize email with `LOWER(TRIM(email))`. Group duplicate applicant rows by that normalized identity, and count `DISTINCT job_posting_id` rather than interview rows. Use a defensible fallback identity only if email is null/blank, for example normalized name, and clearly document it. Return only identities with more than one distinct posting.

**Validation:** case-only differences such as `ananya.rao@mail.com` vs `Ananya.Rao@mail.com` collapse to one person; multiple stages for the same applicant/posting count only once; output includes enough context to explain the grouping.

### 12.5 Query 4 — Recruiter Final conversion rate

Join recruiters through owned postings to interviews, count only `Final` stage interviews, and calculate passed divided by total Final-stage interviews. Use `1.0 * numerator / denominator` or an explicit cast to avoid integer division. Apply `HAVING COUNT(final_interview.id) >= 3` as the threshold. Define a treatment for `NULL` outcomes: it belongs in the denominator because it is a Final interview, but is not counted as passed.

**Validation:** recruiters below the threshold are omitted; displayed rate is fractional/percentage as labelled; pending outcomes do not inflate the passed numerator.

### 12.6 Query 5 — Bonus: top successful-placement recruiter per department

Count successful placements as `Final` interviews with `outcome = 'passed'`, grouped by department and recruiter. Use `RANK()` or `DENSE_RANK()` over each department ordered by placement count descending. Filter to rank 1 in an outer query/CTE. Returning multiple recruiters for a tied first place is preferable to arbitrarily hiding a tie, and should be documented.

**Validation:** the SQL uses a true window function; ranking restarts per department; tied leaders are treated consistently.

### 12.7 Run strategy without adding mandatory tools

The application itself needs no SQLite server; the Xerial JDBC driver opens the file locally. The `.sql` file can be run using an installed SQLite CLI if available. To avoid making that CLI a mandatory project dependency, also validate the challenge during development with a small JUnit integration test using SQLite JDBC and an isolated temporary database, or provide the CLI instruction as an optional alternative. The repository submission remains the `.sql` file regardless of how it is verified.

---

## 13. Test matrix and test data strategy

### 13.1 Unit tests: matching

Use plain Kotlin/JUnit tests with small explicit `Candidate` and `Job` values, independent from Spring and the filesystem.

| Area | Example assertions |
|---|---|
| Full skill match | Skill coverage is 100% |
| No overlap | Skill coverage is 0%; missing skills are correct |
| Partial match | Numerator/denominator and score are correct |
| Experience | Below-minimum fit is proportional; at/above minimum caps at 100% |
| Culture | Trait/keyword intersection is canonical and deterministic |
| Availability | Each enum applies the documented penalty |
| Clamp/round | Result is bounded and rounds using the selected rule |
| Reason | Text corresponds to the actual score breakdown |
| Order | Equal scores are sorted using the same stable tie-break each run |
| Reverse consistency | Same pair produces identical score whichever endpoint initiated it |

### 13.2 Repository integration tests

Use a temporary SQLite path per test suite/test. Point Spring/Flyway/JDBC to the temporary DB and clean it after the test. Do not reuse the developer's `recruiterbot.db`.

Verify:

- All migrations execute from a clean file.
- 15 candidates and 6 jobs are seeded (with the expected relationship rows).
- Skills/traits/keywords map to normalized sets without duplication.
- Foreign keys reject orphan relation rows when constraints are enabled.
- Unknown ID is distinguishable from a valid record.
- Repository queries use bound parameters and return deterministic content.
- Usage events survive closing and reopening a database connection/application context.

### 13.3 HTTP integration tests

Use Spring Boot Test and a test HTTP client/MockMvc appropriate to the generated Boot 4 starter setup. Include security test support if required.

- Required job endpoint returns 200, job metadata, candidate list, scores, reasons, and a descending deterministic order.
- Required candidate endpoint returns 200 and consistent pair scores.
- Missing IDs return 404 and the shared problem response with request ID.
- Invalid IDs/bad date requests return structured 400 errors.
- Security tests cover no credentials, normal user, and admin behavior.
- `/v3/api-docs` returns a valid document containing both required operations and security scheme.
- Swagger UI route returns the UI shell/resources when allowed.
- Health endpoint returns the defined health response.

### 13.4 Usage tests

- Eligible API request writes one event with route template, status, time, and nonnegative duration.
- Docs/Actuator/statistics routes are excluded.
- Multiple events aggregate correctly by date, route, and status.
- Empty period returns all-zero totals.
- An invalid/inverted/oversized range is rejected.
- Telemetry write failure is logged and does not turn a successful matching response into a 500.
- IDs/query strings/bodies do not appear in stored route values.

### 13.5 Architecture verification

Use Spring Modulith verification (and/or its test integration) to ensure package dependencies flow in the intended direction. `matching` may depend on candidate/job public APIs; candidate and job must not depend on matching. Usage/web infrastructure must not own business scoring logic. Avoid cycles and implementation-package access.

### 13.6 SQL challenge tests

Execute the provided DDL/seed + five SELECT queries against a temporary SQLite database via JDBC or a CLI when available. Assert the structural requirements and meaningful query results. A lightweight test can inspect the SQL file or execute each known query against the fixture; do not make tests depend on an external database install.

---

## 14. Configuration plan

### 14.1 `application.yml`

Centralize neutral defaults and documented configuration:

- `spring.application.name: recruiterbot`.
- SQLite JDBC URL, driver class, and data source options.
- Flyway enablement/location if chosen.
- Logging structured format and levels.
- Actuator endpoint exposure and health-detail visibility.
- Springdoc metadata/path only if necessary.
- Security policy settings that are not secrets.
- Usage-statistics range cap/exclusions if configured.

Do not commit passwords or API tokens. There are no external API tokens for this project.

### 14.2 `application-local.yml` (optional)

Use this for local-only convenience settings, including clearly marked demo credentials, friendlier logging if JSON is hard to read during development, and a local DB path. Avoid making an insecure local default active in a production profile.

### 14.3 Database path and Git hygiene

Create the local data directory safely or document that the user should create it once. Add the SQLite database file, WAL/SHM files, local logs, `.gradle/`, build outputs, and IDE workspace files to `.gitignore` where appropriate. Commit schema and seeds, never the developer's runtime database.

### 14.4 Sample configuration

The README should show environment variables for non-local secrets and a documented simple local command. Configuration should fail with an actionable startup message if required non-local secret values are absent; it must not dump those values into logs.

---

## 15. README and submission package

The README should be concise enough to navigate but detailed enough to explain the design. Suggested sections:

1. Project summary and assignment scope.
2. Technology choices and why SQLite/JDBC were chosen.
3. Requirements and local run commands (Java 21, Gradle wrapper; no DB server).
4. Configuration, SQLite file location, migrations and initial seed behavior.
5. Endpoints, curl examples, authentication setup, Swagger URL.
6. Matching formula and one or two trade-offs, including soft availability penalty and stable tie-break.
7. Operational features: request IDs, logs, health/metrics, usage-report endpoint.
8. Test command and what is covered.
9. SQL Detective instructions and query assumptions (especially normalized email identity and interview-based evidence for postings).
10. Incomplete items/future improvements, if any.
11. AI-tool usage disclosure as requested by the assignment.

Do not claim production readiness. State clearly what is demonstrated versus what would need work at real-world scale (multi-instance operation, write contention, data retention, full identity management, etc.).

---

## 16. Phased execution plan

### Phase 1 — Build foundation and dependency proof

- [ ] Inspect the generated Gradle build and eliminate duplicate SQLite dependencies.
- [ ] Add compatible springdoc-openapi 3.x.
- [ ] Check test dependencies and add security/Modulith test support only if missing.
- [ ] Verify SQLite JDBC opens a file and foreign keys can be enabled per connection.
- [ ] Verify Flyway with that exact driver/version combination, or choose and document bootstrap SQL.
- [ ] Confirm a clean app starts and Gradle tests pass.

**Deliverable:** bootable starter with a documented dependency set.

### Phase 2 — Model and migration design

- [ ] Define candidate/job/availability domain types.
- [ ] Write V1 schema with checks, keys, foreign keys, and indexes.
- [ ] Create the canonical skills/traits/culture mappings from the Markdown fixture.
- [ ] Write V2 seed migration with deterministic IDs and all provided fields.
- [ ] Add V3 usage event table/indexes when the telemetry shape is finalized.
- [ ] Verify all candidate/job counts and association counts.

**Deliverable:** clean SQLite DB with source data.

### Phase 3 — Raw SQL access layer

- [ ] Implement `JdbcCandidateRepository` queries and row mapping.
- [ ] Implement `JdbcJobRepository` queries and row mapping.
- [ ] Expose narrow read facades from candidate/job modules.
- [ ] Write repository integration tests using temporary SQLite files.
- [ ] Confirm no ORM/query builder has entered the dependency graph or source.

**Deliverable:** tested access to full profiles for scoring.

### Phase 4 — Matching algorithm and API

- [ ] Implement pure `MatchScoreCalculator` and breakdown model.
- [ ] Implement reason builder from score breakdown.
- [ ] Implement stable sort comparator.
- [ ] Implement `MatchingService` with forward and reverse operations sharing the scorer.
- [ ] Implement exact required paths in `MatchingController` and response DTOs.
- [ ] Add common 404/400 error response handling.
- [ ] Add unit and HTTP tests for the required contract.

**Deliverable:** both required match endpoints return real ranked output.

### Phase 5 — Security and validation

- [ ] Create explicit security filter chain and local credential policy.
- [ ] Configure auth/forbidden response behavior.
- [ ] Add identifier and usage-query validation.
- [ ] Decide and test access policy for matching endpoints, docs, health, metrics, and admin stats.
- [ ] Add tests proving normal users cannot access admin stats.

**Deliverable:** predictable, documented local security behavior.

### Phase 6 — Logging and request IDs

- [ ] Choose structured console logging format supported by Boot 4.1.1.
- [ ] Implement safe request ID handling and MDC lifecycle.
- [ ] Add one completion log per request with route template/status/duration.
- [ ] Add exclusions for diagnostic/documentation paths.
- [ ] Test log correlation and ensure secrets/payloads are not logged.

**Deliverable:** actionable local logs with request correlation.

### Phase 7 — Actuator and metrics

- [ ] Configure minimal endpoint exposure.
- [ ] Restrict sensitive actuator routes.
- [ ] Verify DB health checks or implement a small health indicator if needed.
- [ ] Add low-cardinality match counters/timers.
- [ ] Test endpoint accessibility and meter registration.

**Deliverable:** operational inspection without a separate observability stack.

### Phase 8 — Persistent usage statistics

- [ ] Implement request event model and JDBC repository.
- [ ] Record normalized eligible API routes at completion.
- [ ] Ensure telemetry failures do not mask business responses.
- [ ] Implement range validation, UTC boundary handling, and aggregate queries.
- [ ] Add admin-only usage endpoint and security rules.
- [ ] Add persistence/aggregation/exclusion tests.

**Deliverable:** durable local API usage report backed by SQLite.

### Phase 9 — Swagger/OpenAPI

- [ ] Configure title/description/version.
- [ ] Annotate both matching endpoints and error responses.
- [ ] Add HTTP Basic security scheme compatible with the security policy.
- [ ] Document score formula and reason semantics.
- [ ] Verify docs JSON and UI paths on Spring Boot 4.1.1 + springdoc 3.x.

**Deliverable:** interactive local API demonstration.

### Phase 10 — SQL Detective file

- [ ] Add the untouched four-table schema and dataset setup to the root SQL file if making it self-contained.
- [ ] Write one commented query per question.
- [ ] Implement case-insensitive duplicate identity resolution and distinct job counting.
- [ ] Ensure Query 2 returns postings with zero Final interviews.
- [ ] Ensure Query 4 handles integer division and null outcomes correctly.
- [ ] Ensure Query 5 uses a per-department window function and handles ties.
- [ ] Verify all queries execute against a fresh SQLite fixture.

**Deliverable:** `sql_detective.sql` ready for assignment submission.

### Phase 11 — Final quality, docs, and manual smoke test

- [ ] Run the full Gradle test/check tasks.
- [ ] Verify module boundaries.
- [ ] Run from a new database file, not an existing local DB.
- [ ] Exercise both curl examples and confirm same-pair scores match in both directions.
- [ ] Verify unknown IDs and invalid usage dates return structured errors.
- [ ] Verify `/actuator/health`, authorized metrics, OpenAPI, Swagger UI, and admin usage reporting.
- [ ] Confirm SQL Detective queries return expected results.
- [ ] Update README, AGENTS.md, and this plan to match actual implementation.
- [ ] Check `.gitignore`, repository status, and ensure local database/logs/secrets are not staged.

**Deliverable:** reproducible repository and a clear explanation of engineering decisions.

---

## 17. Example local verification commands

These are target commands; adjust only if project scripts or operating system require a small change.

```bash
# Compile and run tests
gradle --version
./gradlew clean test
./gradlew check

# Start the application (after dependencies/config/migrations are complete)
./gradlew bootRun

# Readiness/health (subject to the documented security policy)
curl -i http://localhost:8080/actuator/health

# Assignment endpoints (environment variables hold local credentials)
curl -u "$RECRUITERBOT_USER:$RECRUITERBOT_PASSWORD" \
  http://localhost:8080/jobs/1/matches

curl -u "$RECRUITERBOT_USER:$RECRUITERBOT_PASSWORD" \
  http://localhost:8080/candidates/1/matches

# Swagger/OpenAPI
curl -i http://localhost:8080/v3/api-docs
# Open in a browser: http://localhost:8080/swagger-ui/index.html

# Usage summary (admin credentials)
curl -u "$RECRUITERBOT_ADMIN_USER:$RECRUITERBOT_ADMIN_PASSWORD" \
  'http://localhost:8080/api/v1/admin/usage?from=2026-01-01&to=2026-12-31'
```

No external REST API, account, remote database, Redis instance, or container is needed for these checks.

---

## 18. Risks and decisions to keep visible

| Risk / decision | Mitigation |
|---|---|
| Spring Boot 4 / springdoc compatibility | Use springdoc 3.x; verify on a real startup and OpenAPI request. |
| SQLite/Flyway module/version mismatch | Inspect resolved dependencies; test migration on an empty DB; use matched versions or a documented SQL bootstrap. |
| SQLite foreign keys not enabled by default | Configure them for every connection and test an orphan insert. |
| Seed data contains line-wrap artifacts | Normalize known split terms explicitly and test canonical tokens. |
| Heuristic scoring seems arbitrary | Publish formula and rationale; keep score calculator pure and test every component. |
| Not-looking candidate could still rank highly | Make availability an explicit documented penalty, not an accidental hard filter. |
| Reverse endpoints can diverge | Share one pair-scoring function and test score equality. |
| SQLite synchronous event insert adds write load | Keep it simple for this scale; fail telemetry softly and document the throughput limitation. |
| Telemetry may expose sensitive data | Store route templates and minimal fields only; never store body/query string/PII. |
| Security starter can block Swagger or confuse curl usage | Use an explicit security chain, document local credentials, and test docs/auth paths. |
| Metrics cardinality becomes unbounded | Restrict custom tags to low-cardinality dimensions. |
| SQL challenge duplicate detection is ambiguous | Explain that the interviews table is the only applicant-to-job relation, normalize emails, and count distinct postings. |
| Too much enterprise machinery can obscure core work | Implement in phases; core matching and SQL challenge must pass before secondary additions. |

---

## 19. Upstream references for compatibility checks

Verify versions again at implementation time; do not treat version examples here as immutable pinning instructions.

- Spring Boot 4.1.1 Kotlin support: https://docs.spring.io/spring-boot/reference/features/kotlin.html
- Springdoc OpenAPI project / Boot 4 guidance: https://github.com/springdoc/springdoc-openapi
- Springdoc releases: https://github.com/springdoc/springdoc-openapi/releases
- Flyway SQLite driver reference: https://documentation.red-gate.com/flyway/reference/database-driver-reference/sqlite
- Xerial SQLite JDBC driver: https://github.com/xerial/sqlite-jdbc
- Spring Modulith releases and compatibility notes: https://github.com/spring-projects/spring-modulith/releases

The generated Gradle dependency graph is the ultimate evidence for what this specific project resolves. When a version conflict appears, resolve it before writing application code rather than papering over it with arbitrary version overrides.
