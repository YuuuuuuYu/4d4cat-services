# 4d4cat-services project instructions

## Project

- Java 21 / Spring Boot 3.4.12 Gradle multi-module backend with a React 18 / TypeScript frontend in the `ApplyDays` submodule.
- `api` serves HTTP APIs and authentication, `data` runs collectors/workers/schedulers, `core` owns shared domain and infrastructure code, and `monitoring` exposes observability endpoints.
- PostgreSQL is the persistent store; Redis is used for caches, random media sets, messages, queues, and scheduler locks.

## Architecture and domain boundaries

- Keep backend dependencies one-way: `api -> core` and `data -> core`. `core` must not depend on `api`, `data`, or `monitoring`; `api` and `data` must not depend on each other.
- Preserve the existing feature-based packages. Shared domain or infrastructure belongs in `core`; HTTP-only concerns belong in `api`; collection, batch, and scheduled work belongs in `data`.
- Preserve Command/Query separation in ApplyDays services. Do not move write behavior into `*QueryService` or read-only query behavior into `*CommandService`.
- Association policy is domain-specific: ApplyDays entities use scalar IDs/slugs to reduce coupling, while TechBlog currently uses JPA associations for `Company`, `TechBlogPost`, and tags. Do not apply either pattern globally without an explicit architecture change.

## Implementation policies

- Prefer immutable Java `record` request/response DTOs. Types stored through the Redis `Object` serializer are an exception: they must round-trip with `GenericJackson2JsonRedisSerializer` and `DefaultTyping.NON_FINAL`; use a non-final `Serializable` class where that serializer requires type metadata.
- Use constructor injection. Keep entity mutation behind intent-revealing domain methods rather than adding public setters.
- Use imports instead of fully qualified class names in Java bodies. Framework expressions or unavoidable name collisions are valid exceptions.
- Do not load large tables and filter them in memory. Express filtering and bulk operations in repositories while preserving transaction and persistence-context semantics.

## Persistence and transactions

- `BaseEntity` supplies audit timestamps; `BaseSoftDeleteEntity` additionally supplies the `deleted` flag. Automatic soft delete requires entity-level `@SQLRestriction` and `@SQLDelete`; some entities instead manage the flag explicitly. Inheritance alone does not determine delete behavior.
- Do not use bulk delete methods on soft-deleted entities unless physical deletion is intentional. See `docs/project/SOFT_DELETE_GUIDE.md` for the established behavior and caveats.
- Keep slow external I/O outside database transactions where the current flow does so, especially PortOne calls. Re-check state and idempotency under the transactional lock before committing payment changes.
- Publish notifications caused by state changes after commit. ApplyDays has two deliberate paths: verification events enqueue email work and subscription events use after-commit async listeners. Preserve the path used by that domain flow.

## API compatibility and security

- Treat existing routes, HTTP methods, response envelopes, auth requirements, and cursor formats as compatibility contracts. Do not introduce a global URL-versioning or response-wrapper convention over endpoints that intentionally differ.
- Never commit credentials or log tokens, passwords, billing keys, webhook URLs, or verification-image contents. Keep client-facing failures on the existing `ErrorCode` / `GlobalExceptionHandler` path.
- Preserve ownership and role checks when changing ApplyDays queries. Cache keys for authorization-sensitive results must include the effective authority and all result-shaping inputs.

## Configuration and production constraints

- `config` is a private submodule and the source of truth for `application*.yml`. Gradle copies those files into module resource directories; do not edit copied `api|data|monitoring/src/**/application*.yml` files.
- Preserve the distinction between production, local, and test configuration. Do not add secrets or change production fallbacks, ports, scheduler timing, connection budgets, or exposed Actuator endpoints unless the task requires it.
- Deployment behavior is defined by `.github/workflows/` and the Dockerfiles. Do not deploy or mutate live infrastructure as part of ordinary implementation work.

## Verification

- Initialize submodules when required: `git submodule update --init --recursive`.
- Backend targeted check: `./gradlew :<module>:test`; repository check: `./gradlew test spotlessCheck`; CI-like build: `./gradlew clean build jacocoTestReport`.
- Frontend checks from `ApplyDays`: `npm run check` and `npm run build`.
- Tests that touch Redis should use the existing test configuration/mocks unless the test is explicitly an infrastructure integration test. Use `core` test fixtures for shared domain setup.
- Run the smallest checks that cover the change, then broaden only when shared code, configuration, or cross-module behavior changed.

## Change policy and routing

- Keep application code, dependencies, schema, and production configuration unchanged unless they are part of the requested work.
- Load the matching repository skill for ApplyDays, TechBlog, Pixabay, Message, monitoring, or security work. Skills contain the task-specific workflow and invariants; this file remains the repository-wide policy.
- Use `docs/project/LOCAL_TEST_WORKFLOW.md` for the local startup and manual-trigger procedure.
- Use `docs/troubleshooting/` as incident history, not as automatically current policy. Validate historical fixes against the present code and configuration before applying them.
