## 1. Regression guard (test first)

- [x] 1.1 Add a JUnit 5 test that fails if any `log4j2.xml`, `log4j2.json`,
      `log4j2.yaml`, `log4j2.yml` or `log4j2.properties` (with or without `-test`
      suffix) resolves from the main output directory (`target/classes`); verify
      it fails while `src/main/resources/log4j2.xml` still exists
      (`mvn -Dtest=<TestClass> test` red)

## 2. Remove the shipped configuration

- [x] 2.1 Delete `src/main/resources/log4j2.xml` (and the then empty
      `src/main/resources` directory if nothing else is in it); verify the test from
      1.1 passes
- [x] 2.2 Run the offline test suite (see `ai/memory/reference_build_and_test.md`)
      and verify it is green and still prints log output configured by
      `log4j2-test.xml`
- [x] 2.3 Run `mvn -DskipTests package` and verify with
      `unzip -l target/rest-client-*.jar | grep -i log4j2` that the jar contains no
      logging configuration

## 3. Consumers and release

- [x] 3.1 Remove the "Stop shipping `log4j2.xml` in the jar" entry from
      `ai/open-proposals.md` if still present, and record the overmind
      SLF4J-provider risk in `ai/memory/project_consumers.md`; verify both files read
      correctly
- [x] 3.2 Set `<version>` in `pom.xml` to `1.0.11` (confirm with
      `git fetch --tags; git tag --sort=-creatordate | head -1` that `1.0.10` is the
      latest release); verify `mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
      prints `1.0.11`
