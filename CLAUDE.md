# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project at a glance

- This is a **multi-module Maven library** (`packaging: pom`) for Spring Boot microservices.
- Group/artifact: `io.github.ilovejavac:common-lib`.
- Java toolchain is **Java 25** (`java.version=25` in root `pom.xml`).
- Language mix: Java + Kotlin. Kotlin compilation profile activates automatically for modules that have `src/main/kotlin`.
- Important default: root `pom.xml` sets `<skipTests>true</skipTests>`, so tests are skipped unless explicitly overridden.

## Common development commands

Run all commands from repository root.

### Build / compile

- Fast compile (no tests):
  ```bash
  mvn -DskipTests=true clean compile
  ```
- Build all modules (still skipping tests):
  ```bash
  mvn -DskipTests=true clean install
  ```
- Build only one module with dependencies:
  ```bash
  mvn -pl common-data-jpa -am -DskipTests=true clean compile
  ```

### Tests

Because tests are skipped by default, always pass `-DskipTests=false` when you want tests.

- Run all tests:
  ```bash
  mvn -DskipTests=false test
  ```
- Run tests for one module:
  ```bash
  mvn -pl common-starter -am -DskipTests=false test
  ```
- Run one test class:
  ```bash
  mvn -pl common-starter -am -DskipTests=false -Dtest=GlobalExceptionHandlingTest test
  ```
- Run one test method:
  ```bash
  mvn -pl common-data-jpa -am -DskipTests=false -Dtest=MultiJpaDatasourceIntegrationTest#shouldApplyDatabasePlatformPerDatasource test
  ```

### Lint / static checks

- No dedicated lint plugin (Checkstyle/SpotBugs/PMD/ktlint/detekt) is configured in this repo.
- Use compile + tests as the main quality gate:
  ```bash
  mvn -DskipTests=true compile
  mvn -DskipTests=false test
  ```

### Publishing-related note

- Root build binds `maven-gpg-plugin` at `verify` phase for signing.
- For normal local development, prefer `test` / `compile` / `install` unless you intentionally need signing behavior.

## High-level architecture

## 1) Module topology and dependency direction

The root `pom.xml` aggregates many library modules. Practical dependency backbone:

- `common-util` → low-level utilities (Kotlin helpers, option/outcome, coroutine holders).
- `common-core` → shared domain model, DSL metadata, security context models, cross-cutting abstractions.
- `common-starter` → baseline Spring Boot auto-configuration + default app config.
- Data layers on top of core:
  - `common-data-jpa` (JPA + QueryDSL)
  - `common-data-mongo` (Mongo)
  - `common-data-search` (OpenSearch)
  - `common-data-jpa-tenant` (tenant extensions for JPA)
- Capability modules depend on core/data as needed:
  - security (`common-security`, `common-security-jwt`, `common-security-sa`, `common-security-aes`)
  - messaging (`common-mq` + backend adapters)
  - cloud (`common-cloud`, `common-cloud-consul`, `common-cloud-nacos`)
  - agent (`common-agent`)

## 2) Auto-configuration and config loading model

Two mechanisms are used together:

1. **AutoConfiguration.imports** in module resources (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`) registers module auto-config classes.
2. `common-starter` also uses `spring.factories` to register `CommonLibDefaultsEnvironmentPostProcessor`, which loads selected `application-*.yml/yaml` files as low-priority defaults.

`common-starter/src/main/resources/application-lib.yaml` then imports optional module configs (`application-data.yaml`, `application-security.yaml`, `application-mq.yaml`, etc.).

If you add a new shared config file, keep these in sync:
- `CommonLibDefaultsEnvironmentPostProcessor.SUPPORTED_CONFIG_FILES`
- `application-lib.yaml` `spring.config.import`

## 3) Unified query DSL across JPA / Mongo / OpenSearch

A shared `DslQuery` model in `common-core` drives predicate building across stores:

- JPA path: `BaseRepository` + `BaseRepositoryImpl` + `QueryBuilder` + QueryDSL execution.
- Mongo path: `common-data-mongo` `BaseRepository` with DSL-to-predicate assembly.
- OpenSearch path: `common-data-search` `BaseRepository` with DSL predicate/sort builders.

Important behavior in JPA:

- Delete operations default to **soft delete** with cascade handling (`CascadeSoftDeleteSupport`).
- Physical delete is explicit via `repository.physicalDelete()`.
- Aggregation uses explicit `aggregate(...)`; regular query methods guard against accidental `agg()` usage.

## 4) Security request pipeline

`common-security` wires MVC interceptors in order:

- `InternalInterceptor` first
- `AuthInterceptor` second

Both target `/api/**` with excludes for auth/public endpoints.

`PermissionValidator` handles:

- anonymous endpoint resolution (`@Anonymous`)
- whitelist skipping
- token extraction/parsing into `SecurityContextHolder`
- role/permission enforcement via `@RequireRole` and `@RequirePermission`

## 5) Messaging abstraction + backend adapters

`common-mq` provides the abstraction (`MQ`, `MQTemplate`, `MessageExtend`, reliability config).

Backend modules (`common-mq-rabbit`, `common-mq-kafka`, `common-mq-rocketmq`) each register their own auto-configuration and implementation.

`common-local-task-message` provides local task/outbox-style support used by MQ adapters for reliability/retry integration.

## 6) Agent/harness subsystem (common-agent)

`common-agent` contains a session/protocol/sdk skeleton:

- turn/session state (`HarnessSession`, `ActiveTurn`)
- protocol event adaptation (`SaaResponseEventAdapter` + tests)
- model integration via Spring AI / Alibaba ReactAgent (`SaaAgent`)

This module is still evolving; tests currently focus on protocol/event adaptation behavior rather than full end-to-end orchestration.

## Working conventions for this repo

- Most modules are **libraries**, not standalone runnable apps.
- Prefer targeted module tests (`-pl <module> -am`) over full-repo test runs during iterative development.
- When diagnosing startup/config behavior, inspect both auto-config import files and `application-lib.yaml` import chain.
