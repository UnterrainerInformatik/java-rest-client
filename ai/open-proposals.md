# Open proposals

Things that have been worked out but not decided or not carried out. Each entry
says what it is, what it costs, and what it unblocks, so it can be answered with
a yes or a no rather than re-researched.

Delete an entry the moment it becomes an OpenSpec change -- OpenSpec is then the
record. If rejected, move the reasoning into the README so it is not proposed
again.

## parent-pom: exempt log4j from the "non-test scoped, test only" check

`parent-pom` 1.0.4 declares log4j-api/-core/-slf4j2-impl in compile scope and
exempts them only via `ignoredUnusedDeclaredDependencies`. As soon as a test
references log4j types (e.g. a capturing appender), `dependency:analyze-only`
fails with "Non-test scoped test only dependencies found". This repo works
around it locally (`ignoredNonTestScopedDependencies` in `pom.xml`, change
`ignore-log4j-test-only-analyze`); the sibling repos hit the same failure.

- **What:** add log4j-api and log4j-core to `ignoredNonTestScopedDependencies`
  in the parent's `analyze` execution.
- **Cost:** a parent-pom release plus a parent bump in every repo that inherits it.
- **Unblocks:** a green local `mvn verify` everywhere without per-repo overrides;
  the override in this `pom.xml` can then be removed.

## KeycloakContext: renew the token before it expires, not after the server refuses it

Since 1.0.11 a call that is refused with 401 discards the token, fetches a new
one and is made once more -- and that works. But on staging (dev1, both CMS data
loggers, 2026-10-02 03:09-06:10 UTC) the retry is not the exception, it is the
routine: the logger against the staging keycloak logged **20** "was refused
with 401 and succeeded with a new token" in three hours, the one against
production **6**, spread over every endpoint (`opcua/values/bulk`,
`opcua/heartbeats/bulk`, `loggers/cmodules`, `loggers/ctop`, `actions`). Nothing
was lost; every one cost an extra round trip and a WARN line.

Two things in `KeycloakContext.update()` make that expected rather than rare:

- **No margin.** `refreshTimestamp = now + expiresIn * 1000L`, with `now` taken
  *before* the token request went out, and the token is reused until
  `now > refreshTimestamp`. A caller that sends every second will, once per
  token lifetime, send a token that expires while the request is in flight or
  that the server's clock already sees as expired.
- **No synchronisation.** One context is shared by every scheduled task of a
  service (the logger has at least four threads on it). `update()` and
  `invalidate()` read and write `accessToken` / `refreshTimestamp` unguarded, so
  two threads can fetch two tokens at once, or one can invalidate the token the
  other has just put into its header.

- **What:** renew when less than a margin is left (e.g. `min(30 s,
  expiresIn / 2)`), and make token renewal and invalidation atomic per context.
  The 401 retry stays as the safety net for tokens the server revokes early.
- **First, measure:** the access-token lifetime of both realms (`expires_in` of
  a token response) -- the spacing of the 401s suggests a few minutes on
  staging and longer on production, but that was not checked.
- **Cost:** a few hours plus a test that uses a short `expires_in`; a rest-client
  release and a version bump in `java-cms-data-logger` (and every other user).
- **Unblocks:** the 401 WARN becomes a real signal again -- today it fires every
  ten minutes on a healthy logger, so a revoked grant would not stand out.
