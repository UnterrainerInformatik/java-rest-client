---
name: feedback_tests_welcome
description: Tests are welcome — write them freely when adding or changing behaviour; they must not need the network
metadata:
  type: feedback
---

Adding or extending tests alongside a change does not need permission.

**Why:** Gerald said so explicitly. Tests are a default part of the work.

**How to apply:** JUnit 5 (see the `java-test-quality` and `tdd` skills). New tests run
against a local `com.sun.net.httpserver.HttpServer` on an ephemeral port — no new test
dependency, no LAN, no internet. `BuilderTests` and `KeycloakContextTests` are deliberate
**manual smoke tests** against staging (`*.lan.elite-zettl.at`, VPN only) — Gerald
2026-09-29. Leave them as they are; don't replace or "fix" them, and keep automated tests
in separate classes. Never strip existing tests to simplify a refactor
without flagging it.
