## Why

Since 1.0.11 a call refused with 401 discards its token, fetches a new one and is
repeated once -- and that works. But on staging the repeat is routine, not the
exception. On 2026-10-02 between 03:09 and 06:10 UTC, the CMS data logger running
against the staging keycloak logged **20** "was refused with 401 and succeeded with
a new token" warnings. The one running against production logged **6**. They were
spread over every endpoint (`opcua/values/bulk`, `opcua/heartbeats/bulk`,
`loggers/cmodules`, `loggers/ctop`, `actions`). Nothing was lost, but every one cost
an extra round trip and a WARN line. As long as a healthy logger warns every ten
minutes, a grant that keycloak really revoked would not stand out.

Two things in `KeycloakContext.update()` make this expected rather than rare:

- **No margin.** The token is reused until `now > refreshTimestamp`, i.e. up to the
  last millisecond of its lifetime. A caller that sends every second will, once per
  lifetime, send a token that expires while the request is in flight or that the
  server's clock already sees as expired.
- **No synchronisation.** One context is shared by every scheduled task of a
  service (the logger has at least four threads on it). `update()` and
  `invalidate()` read and write the token fields unguarded. Two threads can fetch
  two tokens at once, or one thread can discard the token that another has just
  fetched.

## What Changes

- **The context renews its token before it expires.** It fetches a new token once
  less than a margin of the token's lifetime is left: 30 seconds, or half the
  lifetime if that is shorter. A token is no longer sent in its last seconds.
- **Token renewal and invalidation are atomic per context.** Threads that need a
  token at the same time share one renewal instead of each fetching their own. A
  401 discards only the token that was refused. If another thread has already
  replaced it, its new token is kept.
- **The 401 retry stays as it is.** It remains the safety net for tokens that the
  server stops accepting early. Its WARN line becomes a real signal again.
- **The token lifetime is measured first.** The `expires_in` of both realms is
  recorded before the margin is fixed. The spacing of the 401s suggests a few
  minutes on staging and longer on production, but nobody has checked.

No **BREAKING** change. The public API (`get`/`post`/`put`/`del`, `invalidate()`,
the getters) stays the same. `getRefreshTimestamp()` still returns the expiry
reported by keycloak. Callers only see fewer token refusals and fewer warnings.

## Capabilities

### New Capabilities

<!-- None -->

### Modified Capabilities

- `keycloak-authentication`: a token is no longer reused up to its reported
  expiry, only until a margin before it. A new requirement says that concurrent
  calls through one context share one renewal, and that a refusal discards only
  the token that was refused.

## Impact

- **Changed:** `KeycloakContext` (`update`, `invalidate`, `execute`, `authorize`).
  The keycloak builders are untouched.
- **Tests:** new cases in `KeycloakTokenTests`. `KeycloakTestServer` can hand out
  a configurable `expires_in` and can count concurrent grants. No new dependency,
  and no network access is needed.
- **Release:** 1.0.12.
- **Consumers:** `java-cms-data-logger`, `java-overmind-server` and
  `java-elite-server` only bump the version. No caller has to change code.
