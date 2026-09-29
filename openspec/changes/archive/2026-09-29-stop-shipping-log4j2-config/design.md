## Context

The same logger list exists twice: `src/main/resources/log4j2.xml` (root INFO) and
`src/test/resources/log4j2-test.xml` (root DEBUG). log4j2 looks for
`log4j2-test.*` before `log4j2.*`, so during tests the main file is already never
used. Its only effect is on consumers, via the jar. See proposal.md for why that is
wrong.

## Goals / Non-Goals

**Goals:**
- The jar carries no logging configuration; tests keep logging as before.
- A regression guard that runs offline in the normal test suite.

**Non-Goals:**
- Changing the log4j/slf4j dependencies or their scopes (parent-pom territory,
  see `ai/open-proposals.md`).
- Trimming `log4j2-test.xml`; it only affects this repo's tests.
- Fixing overmind's multiple SLF4J providers; that belongs in overmind.

## Decisions

- **Delete rather than move.** Moving `log4j2.xml` to `src/test/resources` would
  leave two test configs, one of them dead because `log4j2-test.xml` wins.
  Alternative "exclude it in the jar plugin config" rejected: it keeps a file in
  `src/main` that never ships, which is misleading.
- **Guard with a classpath test, not a jar inspection.** A JUnit test asserts that
  no `log4j2.*` resource (xml/json/yaml/yml/properties) is resolvable from the
  main output directory (`target/classes`), checked via the resource URL's
  location rather than a plain `getResource`, because the test classpath also
  holds `log4j2-test.xml` and dependency jars. What ends up in `target/classes` is
  what the jar plugin packs, so this covers the jar without needing `package` to
  run before the test.
  Alternative: a Maven enforcer rule on the jar — rejected, adds build config to
  cover one file.

## Risks / Trade-offs

- [A consumer without its own log4j2 config silently loses INFO logging from all
  its code, not just ours] → Named per consumer in proposal.md; only overmind is a
  candidate, and it is flagged for its next bump.
- [The guard only checks this repo's `target/classes`] → That is exactly what is
  packed into the jar; nothing else contributes resources.

## Migration Plan

Release as `1.0.11`. Consumers that bump need nothing if they ship their own
configuration (data logger, elite server). Overmind: after bumping, check the
startup log for the SLF4J provider chosen and whether INFO lines still appear.
Rollback: restore the file and release again.
