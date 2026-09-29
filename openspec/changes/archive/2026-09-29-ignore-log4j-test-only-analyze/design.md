## Context

`parent-pom` 1.0.4 declares `log4j-api`, `log4j-core` and `log4j-slf4j2-impl` as
compile dependencies. It configures the `analyze` execution (goal
`analyze-only`, plugin 3.8.1) with `failOnWarning=true` and exempts log4j only
from the "unused declared" check. Main code logs through slf4j (`@Slf4j`), so
main code never references log4j types. `KeycloakTokenTests` references
`org.apache.logging.log4j.*` and `org.apache.logging.log4j.core.*` directly.
That puts log4j-api and log4j-core in the "non-test scoped, test only" category,
which fails the build. `log4j-slf4j2-impl` is not referenced by any code and
stays covered by the parent's "unused declared" exemption.

## Goals / Non-Goals

**Goals:**
- A local `mvn verify` passes the dependency analysis again.
- log4j stays a compile dependency.

**Non-Goals:**
- Changing `parent-pom`.
- Rewriting `WarningCapture` without log4j types.
- Touching the shipped `log4j2.xml`.

## Decisions

- **Declare the plugin in this pom's `<build><plugins>` with an `analyze`
  execution that sets only `ignoredNonTestScopedDependencies`.** Maven merges
  executions by id and merges `<configuration>` element by element, so the
  parent's `failOnWarning` and ignore lists are kept. The version and goal come
  from the parent's `pluginManagement`.
  - *Alternative: move log4j to test scope.* Rejected, because log4j is the
    intended production backend, and test scope would override the parent's
    policy.
  - *Alternative: set `failOnWarning=false`.* Rejected, because it would also
    hide real dependency problems.
  - *Alternative: fix it in `parent-pom`.* This is the right long-term place,
    but it needs a parent release and a bump in every repo. Recorded separately.
- **List the two artifacts as `groupId:artifactId`.** This is the same format
  the parent uses for its other ignore lists.

## Risks / Trade-offs

- [A future production-code dependency on log4j types would no longer be
  flagged as test-only] → Such a dependency would already be correctly scoped
  (compile), so no false negative arises. The exemption only hides the
  opposite, intended case.
- [The exemption goes stale if the parent fixes the gap] → Then it is
  redundant but harmless. Remove it when bumping to that parent version.
