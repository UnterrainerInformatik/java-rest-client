# Log4j Migration Plan: rest-client

**Layer**: 2 — update after `serialization`.

## Before Starting

Prompt the user for the following version numbers before making any changes:

| Variable | Question |
|----------|----------|
| `NEW_PARENT_POM_VERSION` | What is the new `parent-pom` version? |
| `NEW_SERIALIZATION_VERSION` | What is the new `serialization` version? |
| `OWN_NEW_VERSION` | What version should `rest-client` be bumped to? (currently `0.1.5`) |

## Context

Part of a migration from Log4j 1.x to Log4j 2.25.3 across all libraries. Has 3 classes using `@Slf4j` — no code changes needed (SLF4J API is unchanged).

> **IMPORTANT for execution**: This plan should be executed by actually making the file changes described below — create the new `log4j2.xml` / `log4j2-test.xml` files with the content provided, and delete the old `log4j.properties` files. Do not leave the config migration as a manual step.

## Current State

- **Artifact**: `info.unterrainer.commons:rest-client`
- **Parent**: `parent-pom:0.2.1`
- **In-house dependencies**:
  - `serialization:0.2.4` — bump to new version
- **log4j.properties**: YES (main + test)
- **@Slf4j usage**: 3 classes

### Current `log4j.properties` content (main):
```properties
log4j.rootLogger=INFO, A1
log4j.appender.A1=org.apache.log4j.ConsoleAppender
log4j.appender.A1.layout=org.apache.log4j.PatternLayout
log4j.appender.A1.layout.charset=UTF-8
log4j.appender.A1.layout.ConversionPattern=%d [%-5p] [%t] %c [%x] - %m%n
log4j.logger.io.netty=WARN
log4j.logger.org.eclipse.milo=WARN
log4j.logger.org.hibernate=WARN
log4j.logger.com.mchange.v2.log.MLog=WARN
log4j.logger.org.jboss.logging=WARN
log4j.logger.liquibase.servicelocator=WARN
log4j.logger.liquibase.resource=WARN
log4j.logger.com.mchange.v2=WARN
log4j.logger.org.eclipse.jetty=WARN
```

**Note**: Root level is `INFO` and pattern includes `%d` (date) — different from most other libraries.

## Steps

### 1. Update parent version in `pom.xml`

Change the parent version (line 8) to the new parent-pom version.

### 2. Update in-house dependency versions in `pom.xml`

```xml
<dependency>
    <groupId>info.unterrainer.commons</groupId>
    <artifactId>serialization</artifactId>
    <version>NEW_SERIALIZATION_VERSION</version>
</dependency>
```

### 3. Bump own version

Increment `<version>` (line 14, currently `0.1.5`).

### 4. Create `src/main/resources/log4j2.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout charset="UTF-8"
                           pattern="%d [%-5p] [%t] %c [%x] - %m%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Logger name="io.netty" level="WARN"/>
        <Logger name="org.eclipse.milo" level="WARN"/>
        <Logger name="org.hibernate" level="WARN"/>
        <Logger name="com.mchange.v2.log.MLog" level="WARN"/>
        <Logger name="org.jboss.logging" level="WARN"/>
        <Logger name="liquibase.servicelocator" level="WARN"/>
        <Logger name="liquibase.resource" level="WARN"/>
        <Logger name="com.mchange.v2" level="WARN"/>
        <Logger name="org.eclipse.jetty" level="WARN"/>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

### 5. Create `src/test/resources/log4j2-test.xml`

Same content as above (or with root=DEBUG if you want verbose test output).

### 6. Delete old config files

- Delete `src/main/resources/log4j.properties`
- Delete `src/test/resources/log4j.properties`

### 7. Build, test, install

```bash
mvn clean install
```

## Files Changed

| File | Action |
|------|--------|
| `pom.xml` | Update parent version, update serialization version, bump own version |
| `src/main/resources/log4j2.xml` | Create |
| `src/test/resources/log4j2-test.xml` | Create |
| `src/main/resources/log4j.properties` | Delete |
| `src/test/resources/log4j.properties` | Delete |
