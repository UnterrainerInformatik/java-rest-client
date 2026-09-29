---
name: project_consumers
description: Who depends on rest-client and at which version — a release here is a bump there
metadata:
  type: project
---

`info.unterrainer.commons:rest-client` is used by (checked 2026-09-29):

- `~/source/cms/java/java-cms-data-logger` — 1.0.6. Sends values to the elite server every
  second through `KeycloakContext.post`. Its change `keep-what-a-send-holds` waits for 1.0.7.
- `~/source/private/java/java-overmind-server` — 1.0.6. **Check on its bump to ≥ 1.0.11:**
  from 1.0.11 on the jar ships no `log4j2.xml` (change `stop-shipping-log4j2-config`).
  Overmind has no `log4j2.xml` of its own (only `log4j.properties` for `slf4j-reload4j`) and
  three SLF4J providers on its runtime classpath (`log4j-slf4j2-impl`, `slf4j-reload4j`,
  `slf4j-simple`). If the log4j2 provider wins, it logged through our config until now and
  logs only ERROR after the bump. Look at the startup log for the chosen provider and whether
  INFO lines still appear; the fix (one provider, its own config) belongs in overmind.
- `~/source/cms/java/java-elite-server` — 1.0.2.

**Why:** a defect found in a consumer is fixed here, not worked around there — Gerald
(2026-09-29): "das machen wir dort wo es hingehört." The consumer's change then only bumps.

**How to apply:** a behaviour change states in its proposal which consumers are affected.
After a release, tell Gerald which consumers should bump; that happens in their repos.
