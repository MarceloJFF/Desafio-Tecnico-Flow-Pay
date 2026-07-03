# Repository Notes

## Current Layout
- Backend code currently lives in `Back/FlowPay`, not `backend/` as the planning docs describe.
- `Front/` exists but is empty; frontend structure in `Docs/design.md` is planned, not implemented.
- `Docs/` is the main product/architecture source. Prefer `Docs/behavior.md` for business rules and edge cases, `Docs/models.md` for entity/API shapes, and `Docs/phases.md` for implementation priority.

## Architecture Conventions
- Backend should use the standard layered structure: `controller` -> `service` -> `repository` -> `model`.
- Keep request DTOs and response DTOs separated, using Java `record` DTOs in `dto/in` for inbound payloads and `dto/out` for outbound payloads.
- Frontend should follow the standard React structure planned in `Docs/design.md`: `pages`, `components`, `hooks`, and `services`.

## Backend Commands
- Run backend commands from `Back/FlowPay` using the Maven wrapper: `./mvnw test` on POSIX or `.\mvnw.cmd test` on Windows.
- Run a focused test with Maven Surefire syntax: `.\mvnw.cmd -Dtest=ClassName#methodName test`.
- Run the Spring app with `.\mvnw.cmd spring-boot:run`.
- The project targets Java 21 (`pom.xml`). Ensure `JAVA_HOME` points to a valid JDK before running Maven; without it the wrapper exits before tests run.

## Verified Tooling State
- Backend is Spring Boot `4.1.0` with `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, Lombok, and Maven wrapper `3.3.4` downloading Maven `3.9.16`.
- `src/main/resources/application.properties` only sets `spring.application.name=FlowPay`; no datasource, migrations, Docker Compose, lint, formatter, or CI workflow is currently configured.
- No root README or existing agent instructions were present when this file was created.

## Product Constraints To Preserve
- Core invariant: an atendente must never have fewer than 0 or more than 3 active atendimentos, including under concurrent requests.
- Classification should not reject unknown subjects: card-related subjects map to `CARTOES`, loan-related subjects map to `EMPRESTIMOS`, everything else maps to `OUTROS`.
- Waiting queue is logical database state, not a separate queue: `status = AGUARDANDO`, per time, FIFO by `criadoEm`.
- Assignment and finalization must be transactional. Planned locking uses Postgres `SELECT ... FOR UPDATE SKIP LOCKED` so concurrent requests do not over-assign.
- Finalizing an atendimento is not idempotent: nonexistent returns `404`, already finalized returns `409`, and freeing a slot should immediately pull the next queued item for the same time.
- Dashboard updates are expected through SSE at `GET /api/dashboard/stream`, with REST snapshot `GET /api/dashboard/resumo` used initially and after reconnects.
