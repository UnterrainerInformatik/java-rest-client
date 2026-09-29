---
name: reference_build_and_test
description: Build/test commands for rest-client — JDK 21, what CI runs, which tests need the LAN
metadata:
  type: reference
---

- **JDK 21.** Lombok from the parent pom (`parent-pom` 1.0.4) breaks on newer JDKs. Prefix
  Maven with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` unless `archlinux-java` shows 21 as
  default.
- **Tests:** `mvn test -Dtest=<Class>`. Verified 2026-09-29: `LastExceptionTests` passes
  offline on JDK 21. `BuilderTests` and `KeycloakContextTests` are manual
  smoke tests against staging and need the VPN ([[feedback_tests_welcome]]).
- **Reproduce CI:** `mvn -Dmaven.test.skip=true -Prelease-to-sonatype package` is what the
  pipeline does, minus signing/deploy (not yet run locally -- verify before relying on it). In the sibling repos a plain `mvn clean install` fails
  on `dependency:analyze` (log4j reported as test-only). Here it did too since
  `KeycloakTokenTests` uses log4j types; fixed 2026-09-29 via `ignoredNonTestScopedDependencies`
  in `pom.xml` (change `ignore-log4j-test-only-analyze`). `mvn -DskipTests verify` on JDK 21 is green.
  Note: `analyze-only` binds to `verify`, so the CI `package` path never runs it.
- **Surefire is not pinned.** In `java-cms-data-logger` the runner's older Maven picked
  surefire 2.12.4 and ran zero JUnit 5 tests with `BUILD SUCCESS`. CI skips tests here, so
  it doesn't bite yet, but a green run is only real if the report shows a test count.
