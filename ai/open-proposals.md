# Open proposals

Things that have been worked out but not decided or not carried out. Each entry
says what it is, what it costs, and what it unblocks, so it can be answered with
a yes or a no rather than re-researched.

Delete an entry the moment it becomes an OpenSpec change -- OpenSpec is then the
record. If rejected, move the reasoning into the README so it is not proposed
again.

## parent-pom: exempt log4j from the "non-test scoped, test only" check

`parent-pom` 1.0.4 declares log4j-api/-core/-slf4j2-impl in compile scope and
exempts them only via `ignoredUnusedDeclaredDependencies`. As soon as a test
references log4j types (e.g. a capturing appender), `dependency:analyze-only`
fails with "Non-test scoped test only dependencies found". This repo works
around it locally (`ignoredNonTestScopedDependencies` in `pom.xml`, change
`ignore-log4j-test-only-analyze`); the sibling repos hit the same failure.

- **What:** add log4j-api and log4j-core to `ignoredNonTestScopedDependencies`
  in the parent's `analyze` execution.
- **Cost:** a parent-pom release plus a parent bump in every repo that inherits it.
- **Unblocks:** a green local `mvn verify` everywhere without per-repo overrides;
  the override in this `pom.xml` can then be removed.
