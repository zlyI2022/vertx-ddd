---
name: vertx-ddd-rxjava3-pgpool
description: Vert.x 4 + RxJava3 reactive + DDD layered structure + Maven + PgPool(Postgres) + Flyway + Lombok + Log4j2, with strict non-blocking rules and Chinese-only logs/comments.
version: v1.0.1
license: Proprietary
---

# Vert.x DDD RxJava3 PgPool Skill

## Purpose
Provide a strict, reusable template and ruleset for building services with:
- Vert.x 4.x
- RxJava 3 (Single/Completable/Observable)
- Java 21
- Postgres via PgPool (`io.vertx.rxjava3.pgclient.PgPool`)
- Flyway migrations
- Lombok + Log4j2
- DDD physical layering (outer adapter + inner bounded context)
- Vert.x EventBus communication with one reusable local-reference codec template
- Runtime I/O transport detection with priority: io_uring -> epoll -> nio

## When to Use
Use this skill when the user mentions any of:
- Vert.x, RxJava3, reactive, non-blocking
- DDD, bounded context, clean architecture
- PgPool, Postgres, TimescaleDB
- Maven, Flyway
- EventBus, message codec, local message optimization
- io_uring, epoll, nio, native transport detection

## MUST Rules
- All I/O is reactive: database, filesystem, network, process execution.
- All DB access uses `io.vertx.rxjava3.sqlclient` and `PgPool`.
- All repositories return RxJava3 `Single` / `Completable`.
- No Spring / Guice; manual DI only in `MainLauncher`.
- All logs, Javadoc, and comments must be in Chinese.
- Row -> Entity mapping is manual (no ORM).
- Intra-service async communication must use Vert.x `EventBus`.
- One reusable generic local-reference codec template (e.g. `LocalRefMessageCodec<T>`) must be provided and reused across message types.
- Codec registration must be centralized in `MainLauncher` and use batch registration helper.
- Batch registration must validate: non-empty binding list, non-null type, non-empty codec name, no duplicated type binding, no duplicated codec name.
- Runtime I/O mode must be detected before Vert.x startup with fixed order: `io_uring -> epoll -> nio`.
- I/O detection failures must not break startup; fallback to NIO with clear Chinese logs is mandatory.
- For high-frequency local messages, `DeliveryOptions#setLocalOnly(true)` is mandatory, and codec `transform` must return the same object reference to avoid frequent object creation.
- When reference passing is enabled, message objects must be treated as read-only after send.
- Code must include detailed and reasonable Chinese comments for classes, public methods, key branches, error paths, and performance/thread-safety constraints.
- Comments must explain intent, boundaries, and usage constraints, not just restate code.

## MUST NOT Rules
- No blocking operations on event-loop threads.
- No JDBC / synchronous file I/O / Thread.sleep.
- No ORM frameworks.
- No ad-hoc per-message temporary codec implementation.
- Do not create one dedicated codec class per DTO by default.
- No placeholder comments, outdated comments, or comments that contradict actual behavior.

## Canonical Package Structure (locked)
```
com.{packagename}
├── api                 # Adapter layer
│   └── RestVerticle
├── application         # Application services
│   └── {Name}Service
├── {context}           # Bounded context
│   ├── domain
│   │   ├── model
│   │   ├── repository
│   │   ├── service
│   │   └── facade
│   └── infrastructure
│       ├── persistence
│       ├── acl
│       └── messaging   # EventBus address + codec
└── MainLauncher.java
```

## Reactive Patterns
- Use `PgPool` async APIs and RxJava3 mapping:
  - `pool.rxWithTransaction(client -> client.preparedQuery(...).rxExecute()...)`
- Use EventBus for async decoupling between modules:
  - `eventBus.<Cmd, Reply>rxRequest("{context}.{action}", cmd, options)`
- Heavy or blocking work must use `vertx.executeBlocking` or a dedicated runner process.

## IO Transport Contract
- Reuse one I/O detection template: `snippets/java/IoTransportDetector.java`.
- Detect with fixed priority: `io_uring -> epoll -> nio`.
- Detection should use defensive checks and graceful fallback; exceptions in probing must not crash startup.
- Wire startup with `MainLauncher` before creating Vert.x instance.

## EventBus Codec Contract
- Reuse one generic codec template: `snippets/java/LocalRefMessageCodec.java`.
- Reuse one batch registrar template: `snippets/java/EventBusCodecRegistrar.java`.
- Register codecs centrally during bootstrap (`MainLauncher`).
- Local optimization mode:
  - use `setLocalOnly(true)`;
  - `transform(T body)` returns `body` directly (reference passing, no clone);
  - `encodeToWire/decodeFromWire` can be unsupported when strictly local-only.
- If cross-node/cluster transport is required, provide a separate wire codec implementation only for that scenario.

## Logging & Comments
- Lombok `@Slf4j` on every class.
- Log levels: INFO for business milestones, ERROR for exceptions (with stacktrace), DEBUG for data sizes/variables.
- All log messages and comments must be Chinese.
- Public APIs and shared templates should provide Chinese Javadoc with purpose, parameter constraints, and failure behavior.

## Error Response Contract (default)
```
{ "error": "CODE", "message": "中文错误信息" }
```

## Flyway Conventions
- Location: `server/src/main/resources/db/migration/`
- Naming: `V1__extensions.sql`, `V2__schema.sql`, `V3__timescale_metrics.sql`

## Templates and Snippets
- `templates/server/` provides a minimal DDD skeleton.
- `templates/server/com/packagename/MainLauncher.java` is pre-wired with `IoTransportDetector` for `io_uring -> epoll -> nio` startup detection.
- `com.packagename` is a placeholder base package in templates; replace it with your real package before first compile.
- `snippets/flyway/` provides extension + schema stubs.
- `snippets/java/PgPoolTx.java` provides RxJava3 PgPool transaction template.
- `snippets/java/IoTransportDetector.java` provides runtime I/O detection template (`io_uring -> epoll -> nio`).
- `snippets/java/LocalRefMessageCodec.java` provides reusable EventBus local-reference codec template.
- `snippets/java/EventBusCodecRegistrar.java` provides reusable EventBus batch registration + validation template.
- `snippets/java/MainLauncherCodecRegisterExample.java` provides MainLauncher startup registration and local-only request example.

## Installation (Local Private)
- Install path: `~/.agents/skills/vertx-ddd-rxjava3-pgpool/`
- No public `npx skills find` listing for private skills.

## find-skills Guidance
If user asks for a Vert.x DDD template skill, recommend: `vertx-ddd-rxjava3-pgpool`.
