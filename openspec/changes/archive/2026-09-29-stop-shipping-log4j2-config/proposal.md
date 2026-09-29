## Why

`src/main/resources/log4j2.xml` ends up at the root of the published jar. A library
must not ship a logging configuration: depending on classpath order it shadows or
competes with the consumer's own `log4j2.xml`, and its logger list (netty, milo,
hibernate, liquibase, mchange, jboss, jetty) is application configuration that has
nothing to do with a REST client.

## What Changes

- Delete `src/main/resources/log4j2.xml`. The published jar no longer contains a
  log4j2 configuration file.
- Tests keep their configuration: `src/test/resources/log4j2-test.xml` already
  exists, and log4j2 prefers it over `log4j2.xml`.
- Add an offline test that fails if a log4j2 configuration reappears on the main
  classpath.
- **BREAKING** (only for a consumer that has no logging configuration of its own
  and logs through log4j2): such a consumer currently runs on this jar's config
  (root INFO to the console) and falls back to log4j2's default configuration
  (ERROR only) after the bump. No API changes.

## Capabilities

### New Capabilities
- `library-packaging`: what the published jar contains and what it deliberately
  leaves to the consumer — here, the logging configuration.

### Modified Capabilities

(none)

## Impact

- Code: `src/main/resources/log4j2.xml` removed; one new test class. No Java API change.
- Consumers (checked 2026-09-29):
  - `java-cms-data-logger` — has its own `src/main/resources/log4j2.xml`; unaffected,
    no change needed.
  - `java-elite-server` — has its own `src/main/resources/log4j2.xml`; unaffected,
    no change needed.
  - `java-overmind-server` — has no `log4j2.xml`; it configures logging through
    `log4j.properties` for `slf4j-reload4j`. Its runtime classpath contains three
    SLF4J providers (`log4j-slf4j2-impl`, `slf4j-reload4j`, `slf4j-simple`). If the
    log4j2 provider wins there, overmind currently logs through this jar's
    `log4j2.xml` and will log only ERROR after the bump. To be checked when
    overmind bumps; the fix (one SLF4J provider, its own config) belongs in overmind.
- Release: patch release `1.0.11`.
