# Build-a-Recruiter-Bot

A small Kotlin/Spring Boot REST backend that stores the assignment's fictional candidates and jobs in SQLite and ranks their matches in both directions. The separate hiring-operations challenge is in [`sql_detective.sql`](sql_detective.sql).

## Requirements and local run

- JDK 21
- Gradle wrapper (the wrapper downloads Gradle on first use)
- No database service, Docker, Redis, cloud account, or external API

The default `local` profile uses a SQLite file at `./recruiterbot.db`, creates the schema and seed data through Flyway, and enables SQLite foreign keys on every Hikari connection. Override the file with `RECRUITERBOT_DB_URL`, for example `jdbc:sqlite:./data/recruiterbot.db` (the parent directory must already exist).

```powershell
$env:RECRUITERBOT_DB_URL = 'jdbc:sqlite:./recruiterbot.db'
./gradlew.bat test
./gradlew.bat check
./gradlew.bat bootRun
```

The local demo credentials are `recruiter` / `recruiter-local` and `admin` / `admin-local`. Override them with `RECRUITERBOT_USER`, `RECRUITERBOT_PASSWORD`, `RECRUITERBOT_ADMIN_USER`, and `RECRUITERBOT_ADMIN_PASSWORD`. Outside the default `local` profile, all four values are required; no production credential defaults are committed. This take-home's in-memory accounts and HTTP Basic setup are suitable for a local assignment demo, not a production identity system.

## Matching API

Both match endpoints require HTTP Basic authentication. Every response returns a stable envelope with source metadata and a `matches` array; each entry contains the counterpart's ID and display name, a numeric score from 0 to 100, and a short explanation. A known record returns `200` even if its ranked list is empty. An unknown source ID returns `404` with the common problem response.

```powershell
curl.exe -u recruiter:recruiter-local http://localhost:8080/jobs/1/matches
curl.exe -u recruiter:recruiter-local http://localhost:8080/candidates/1/matches
```

## Candidate and job catalog API

The authenticated catalog endpoints list seeded records and allow recruiters to add records. `GET /candidates` returns `{ "candidates": [...] }`; `GET /jobs` returns `{ "jobs": [...] }`. `POST /candidates` and `POST /jobs` accept JSON, return `201 Created`, and include the new resource URL in `Location`. Names/titles and token values are trimmed; skills, traits, and culture keywords are stored as lowercase, deduplicated tokens. Updates and deletion are not exposed.

```powershell
curl.exe -u recruiter:recruiter-local http://localhost:8080/candidates
curl.exe -u recruiter:recruiter-local -H 'Content-Type: application/json' -d '{"name":"Ada Lovelace","yearsExperience":5,"availability":"IMMEDIATE","skills":["Kotlin","SQL"],"traits":["analytical"],"quirk":"Loves clear examples"}' http://localhost:8080/candidates
curl.exe -u recruiter:recruiter-local http://localhost:8080/jobs
curl.exe -u recruiter:recruiter-local -H 'Content-Type: application/json' -d '{"title":"Backend Engineer","minimumExperience":3,"requiredSkills":["Kotlin","SQL"],"cultureKeywords":["analytical"],"tagline":"Build reliable services"}' http://localhost:8080/jobs
```

The score is the weighted sum of:

- Required-skill coverage: 70% (`matched required skills / required skills`)
- Experience fit: 20% (`candidate years / minimum years`, capped at 100%; a zero minimum is full fit)
- Culture fit: 10% (`overlapping traits / culture keywords`)
- Availability: subtract 0 points for immediate availability, 5 for two weeks, and 15 for not looking

Empty skill or culture requirements are treated as neutral full fit for that component. Availability is a soft penalty and never removes a candidate. Scores are clamped to `[0, 100]` and rounded to two decimal places using `HALF_UP`. Reasons are built from the same score breakdown and mention skill coverage, years versus minimum, culture overlap, and availability. Both directions use this same pair scorer. Rankings sort by score descending, matched-skill count descending, culture overlap descending, normalized display name ascending, then ID ascending.

Skill and trait tokens are stored in lowercase, trimmed relational tables. Obvious line-wrap artifacts in the supplied fixture are normalized to canonical tokens (for example, `pattern-recognition`, `time-management`, `public-speaking`, `systems-design`, and `calm-under-pressure`); no fuzzy skill matching is used.

## Documentation and operational endpoints

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health and info: `/actuator/health`, `/actuator/info` (public, limited output)
- Metrics: `/actuator/metrics` (admin only)
- Persistent usage report: `/api/v1/admin/usage?from=2026-01-01&to=2026-01-31` (admin only)

Use Swagger UI's **Authorize** control with either local Basic Auth account. Usage dates are UTC calendar days with an inclusive `to` date; reports are limited to 31 days. The report includes total/error counts and rate, status and route-template breakdowns, and average/p95/maximum latency. `/actuator/**`, documentation paths, and the usage report itself are excluded. The SQLite table stores request completion time, validated request ID, method, route template, status, and duration; it does not store query strings, request bodies, names, or raw IDs in routes. Writes are synchronous and intentionally simple for this small local workload, not a high-throughput analytics pipeline.

Each request receives an echoed `X-Request-ID` (a safe incoming ID of at most 64 characters or a generated UUID). Completion logs use structured JSON and include method, matched route template, status, duration, and request ID. Actuator health and metrics are operational views; durable usage totals come from the separate SQLite events table.

## Persistence and architecture

The Recruiter Bot schema is normalized across candidates, jobs, skills, traits, and culture keywords. Migrations and fixed-ID fixture data live under `src/main/resources/db/migration`. Application persistence uses explicit parameterized SQL through Spring JDBC; no ORM or SQL-generating repository is used. Candidate, job, matching, usage, web, and config responsibilities are separate packages in one Spring Modulith application.

Spring Boot 4.1.1 manages Flyway core at 12.4.0; `flyway-database-nc-sqlite` uses the matching 12.4.0 release. The Xerial driver is managed through Spring Boot. Springdoc 3.1.1 is the Spring Boot 4-compatible UI starter. The application configures `PRAGMA foreign_keys=ON` as Hikari's connection-init SQL so constraints apply to application connections as well as migration connections.

## Tests and SQL Detective

```powershell
./gradlew.bat test
./gradlew.bat check
```

Tests cover the pure score calculator and tie behavior, same-pair consistency in both directions, clean SQLite migrations and seed counts, foreign keys, HTTP response/error/security behavior, OpenAPI and Swagger access, usage aggregation/range limits, Spring Modulith boundaries, and execution of the SQL challenge fixture and five queries against in-memory SQLite.

`sql_detective.sql` preserves the four challenge tables and supplied fixture. Query 3 trims and lowercases email addresses, then counts distinct posting IDs so interview stages do not inflate application counts. Query 5 uses a department-partitioned `RANK()` and keeps ties.

## Trade-offs and limitations

The candidate/job set is small, so rankings are recomputed on each request rather than stored. The weighted score is a transparent heuristic, not a hiring decision model. Synchronous SQLite telemetry is appropriate for this single-process demo but may create write contention at higher traffic. The application does not include candidate/job update or delete operations, pagination, production identity management, multi-instance coordination, or a telemetry retention job.

AI assistance disclosure: OpenAI Codex was used to inspect the assignment and repository, implement and revise code, write tests and documentation, and run the available build checks. The implementation and design choices should be reviewed and explained by the submitting developer.
