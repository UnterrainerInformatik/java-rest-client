---
name: project_consumers
description: Who depends on rest-client and at which version — a release here is a bump there
metadata:
  type: project
---

`info.unterrainer.commons:rest-client` is used by (checked 2026-09-29):

- `~/source/cms/java/java-cms-data-logger` — 1.0.6. Sends values to the elite server every
  second through `KeycloakContext.post`. Its change `keep-what-a-send-holds` waits for 1.0.7.
- `~/source/private/java/java-overmind-server` — 1.0.6.
- `~/source/cms/java/java-elite-server` — 1.0.2.

**Why:** a defect found in a consumer is fixed here, not worked around there — Gerald
(2026-09-29): "das machen wir dort wo es hingehört." The consumer's change then only bumps.

**How to apply:** a behaviour change states in its proposal which consumers are affected.
After a release, tell Gerald which consumers should bump; that happens in their repos.
