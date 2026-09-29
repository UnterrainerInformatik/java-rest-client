## Purpose

Defines what the published rest-client jar contains, so that it never decides
things that belong to the application using it, such as how logging is configured.

## ADDED Requirements

### Requirement: The published jar ships no logging configuration

The published jar SHALL NOT contain a log4j2 configuration file (`log4j2.xml`,
`log4j2.json`, `log4j2.yaml`, `log4j2.yml`, `log4j2.properties`, or any of them
with a `-test` suffix) nor any other logging-framework configuration file.

How the library's log output is filtered and where it goes SHALL be decided
solely by the consumer's own logging configuration.

#### Scenario: The jar contains no logging configuration

- **WHEN** the release jar is built
- **THEN** it contains no `log4j2*.xml`, `log4j2*.json`, `log4j2*.yaml`,
  `log4j2*.yml` or `log4j2*.properties` entry

#### Scenario: The consumer's configuration applies

- **WHEN** a consumer that ships its own `log4j2.xml` uses the library
- **THEN** its configuration is the one log4j2 loads, regardless of classpath order

#### Scenario: The library's own tests still log

- **WHEN** the library's test suite runs
- **THEN** logging is configured by the test-only configuration, which is not part
  of the published jar
