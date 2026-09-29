---
name: feedback_openspec_only_changes
description: Every change goes through the OpenSpec workflow — start with opsx:propose, never patch directly
metadata:
  type: feedback
---

Code, docs and project configuration are only changed inside an OpenSpec change
(`/opsx:propose`, then `/opsx:apply`, `/opsx:archive`). Do not edit source directly, not even
for an obvious one-line bugfix.

**Why:** Gerald: "wir machen KEINE Changes außerhalb des Contextes eines opsx:propose. Wir
wollen die openspec-Spec nicht umgehen." The `openspec/` specs are the source of truth for
what the library does; a direct patch leaves the spec silently out of date.

**How to apply:**
- Announce the intent first ([[feedback_announce_changes_first]]), then `/opsx:propose`.
- Implementation runs via `/opsx:apply` — working `tasks.md` by hand does not count.
- A new behaviour the delta spec lacks is a change of its own. Ticking `tasks.md` boxes is fine.
- Memory files under `ai/memory/` and `ai/open-proposals.md` are the only exception.
- Read-only diagnostics are not restricted.
