---
name: feedback_pom_version_forward
description: Set pom.xml <version> to the number this push will mint BEFORE pushing — the pipeline never writes it back
metadata:
  type: feedback
---

The pipeline (`bump-semver-workflow` → `maven-central-workflow`) reads the last git tag, bumps
it and sets that version inside the build container only. It never pushes it back. So
`<version>` in `pom.xml` is set by hand, **forward**: the pom being pushed already carries the
version this push produces.

**Why:** a checked-out pom must state the same version as the artifact on Maven Central for
that commit. Fixing it afterwards is always off by one — and the fixing commit itself triggers
another bump and release. Missed on 2026-09-24 in `java-mqtt-client` (pom `1.0.3`, release
`1.0.7`).

**How to apply:** before pushing, `git fetch --tags; git tag --sort=-creatordate | head -1`,
set `<version>` to what the bump makes of it (normally build number + 1), in the same commit
as the change. Last release pushed 2026-09-29: `1.0.11` (change `stop-shipping-log4j2-config`); next push mints `1.0.12`.
