## Why

A local `mvn verify` fails in `dependency:analyze-only` with "Non-test scoped
test only dependencies found: log4j-api, log4j-core". log4j comes from
`parent-pom` 1.0.4 in compile scope on purpose, because it is the production
logging backend. The parent only lists it under
`ignoredUnusedDeclaredDependencies`. Since commit 7adada5, `KeycloakTokenTests`
uses log4j types for its `WarningCapture` appender. The analyzer now counts
log4j as "used by tests only" instead of "unused", and the parent has no
exemption for that category. The CI path (`-Dmaven.test.skip=true package`) is
unaffected, but a red local build hides real dependency problems.

## What Changes

- The `analyze` execution of `maven-dependency-plugin` in this `pom.xml` adds
  `org.apache.logging.log4j:log4j-api` and `org.apache.logging.log4j:log4j-core`
  to `ignoredNonTestScopedDependencies`. The rest of the parent's configuration
  (`failOnWarning`, the other ignore lists) stays in force.
- log4j stays in compile scope. No dependency is added, removed or re-scoped.

## Capabilities

### New Capabilities

<!-- None: build configuration only. -->

### Modified Capabilities

<!-- None: no runtime behaviour changes, so `.openspec.yaml` sets
     `skip_specs: true`. -->

## Impact

- **Changed:** `pom.xml` (build section only).
- **Artifact:** the published jar and its dependency list are unchanged.
- **Consumers:** `java-cms-data-logger`, `java-overmind-server` and
  `java-elite-server` see no difference and do not have to change. They declare
  their logging backend themselves and do not rely on this library for it. A
  bump is not needed.
- **Release:** a push to `master` still publishes. If this commit is pushed on
  its own, it mints 1.0.10 with identical content. Otherwise it goes out with
  the next functional change.
- **Not in scope:** fixing the same gap in `parent-pom` (the sibling repos hit
  it too) and the `log4j2.xml` shipped in `src/main/resources`. Both are
  recorded in `ai/open-proposals.md`.
