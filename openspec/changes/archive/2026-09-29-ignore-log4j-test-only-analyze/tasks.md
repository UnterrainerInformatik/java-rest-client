## 1. Build configuration

- [x] 1.1 In `pom.xml`, add a `<build><plugins>` entry for `maven-dependency-plugin` with an execution `analyze` whose configuration lists `org.apache.logging.log4j:log4j-api` and `org.apache.logging.log4j:log4j-core` under `ignoredNonTestScopedDependencies`. Do not set a version or goals; both come from the parent.
- [x] 1.2 Check with `mvn help:effective-pom` that the merged `analyze` execution still has `failOnWarning=true` and the parent's `ignoredUnusedDeclaredDependencies`, plus the new list.

## 2. Verification

- [x] 2.1 On JDK 21, run `mvn -DskipTests verify` and confirm that `dependency:analyze-only` passes and the build is green.
- [x] 2.2 Run the offline tests (`KeycloakTokenTests`, `HttpMethodTests`, `LastExceptionTests`) and confirm from the surefire report that tests ran and passed.
- [x] 2.3 Run `mvn -Dmaven.test.skip=true package` (the CI path) and confirm it is still green.

## 3. Backlog and release

- [x] 3.1 Add two entries to `ai/open-proposals.md`: the same gap in `parent-pom`, and the `log4j2.xml` shipped in `src/main/resources`.
- [x] 3.2 Ask Gerald whether this commit is pushed on its own. If it is, set the pom `<version>` to 1.0.10. Otherwise leave it at 1.0.9 and let the next functional change set the version.
