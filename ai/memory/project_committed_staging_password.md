---
name: project_committed_staging_password
description: The keycloak password in KeycloakContextTests is for LAN staging behind VPN — Gerald says it is irrelevant; don't raise rotation again
metadata:
  type: project
---

`KeycloakContextTests.java` has held a keycloak password in plain text since 2022. It
belongs to the staging instance `*.lan.elite-zettl.at`, which is only reachable through
the VPN.

**Why:** Gerald (2026-09-29): "da kommt keiner rein ohne VPN. Pwd ist in dem Fall
irrelevant." Raising rotation as urgent was a false alarm.

**How to apply:** don't flag it as a security issue again. The test stays: it is a manual smoke test
against staging ([[feedback_tests_welcome]]). New secrets still go to `ai/secrets/`, never into tracked files.
