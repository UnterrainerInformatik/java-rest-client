## Context

See proposal.md - Why. The relevant state of `KeycloakContext` today:

- `accessToken`, `refreshToken` and `refreshTimestamp` are plain fields with
  Lombok getters. `update()`, `invalidate()` and `authorize()` read and write them
  without any lock.
- `update()` takes `now` before the token request goes out and sets
  `refreshTimestamp = now + expiresIn * 1000L`. The token counts as valid while
  `now <= refreshTimestamp`.
- `authorize()` calls `update()` and then reads `accessToken` a second time to build
  the header. Another thread can have cleared or replaced it in between.
- `execute()` calls `invalidate()` on every 401, even if another thread has already
  replaced the refused token.
- `KeycloakTestServer` runs on `HttpServer`'s default executor. That executor
  handles one exchange at a time, so the server cannot exercise concurrency today.
  Every token it issues has `expires_in: 300`.

## Goals / Non-Goals

**Goals:**
- Stop sending tokens in the last seconds of their lifetime.
- Ensure that one context performs at most one token fetch at a time and that the
  token in a header is always the one the fetch returned.
- Make both of these testable offline and deterministically, without sleeping for
  the length of a token's lifetime.

**Non-Goals:**
- Using the refresh token (`grant_type=refresh_token`). The context keeps doing a
  password grant for every new token. That is a separate decision.
- Changing the 401 retry itself (one repeat, WARN line, no repeat on other
  statuses).
- Making the margin configurable. It can be added if a consumer ever needs it.

## Decisions

### Renewal deadline stored next to the reported expiry

Next to `refreshTimestamp`, the context stores a private `renewAt = requestedAt +
lifetime - margin`, where `margin = min(30 s, lifetime / 2)`. `update()` fetches a
new token when `now >= renewAt`. `refreshTimestamp` keeps meaning "expiry as
reported by keycloak", so the public `getRefreshTimestamp()` does not change.

`requestedAt` stays the moment *before* the token request. This errs towards
renewing slightly early, which is the safe direction.

*Alternatives:* store only `renewAt` in `refreshTimestamp`. That is simpler, but
it silently changes what a public getter returns. A fixed margin without the
`lifetime / 2` cap would renew on every call for tokens shorter than 30 s, so the
cap stays.

### One private lock per context, held across the token fetch

All access to the token fields goes through `synchronized (lock)` on a private
`final Object lock`. The lock is private rather than `this`, so that a caller who
synchronises on the context cannot interfere. The token request runs *inside* the
lock. A thread that needs a token while another thread is fetching one waits for
it and then finds a valid token. That is the "one fetch" the spec asks for.

*Alternatives:* a single-flight `CompletableFuture` would let waiting threads
avoid holding a monitor during I/O. It adds code with no observable benefit,
because a thread without a token cannot do anything but wait. A `ReentrantLock`
offers nothing that is needed here. `volatile` fields alone would not make
"check, then fetch" atomic.

### The header uses the token that `update()` returned

`update()` returns the token it ensured, and `authorize()` puts *that* value into
the header. It does not read the field a second time. `execute()` remembers that
token as `sent`. On a 401 it calls a package-private `invalidate(sent)`, which
clears the fields only if `accessToken` still equals `sent`. Otherwise another
thread has already renewed the token, and the retry goes through `update()`,
which simply returns the newer token without a fetch.

The public `invalidate()` stays unconditional, as documented.

The public getters (`getAccessToken()`, `getRefreshToken()`,
`getRefreshTimestamp()`) become hand-written and read under the lock. A caller
therefore never sees a half-updated set of fields.

### An injectable clock for the tests

`KeycloakContext` gets a package-private `LongSupplier clock`, which defaults to
`System::currentTimeMillis`. Tests advance the clock instead of sleeping. The
public constructor (`@RequiredArgsConstructor`) does not change.

### Test server: configurable lifetime, real concurrency

`KeycloakTestServer` gets:
- a settable `expires_in`,
- a multi-threaded executor (`Executors.newCachedThreadPool()`),
- an optional delay on the token endpoint, so that concurrent requests are
  guaranteed to overlap,
- a counter of grants that were in flight at the same time (the maximum), so a
  test can assert that there was never more than one.

## Risks / Trade-offs

- [Holding the lock during the token request blocks the other threads for the
  duration of the request, including `retryShort()`, if keycloak is slow or
  unreachable] → They would otherwise each fire their own request at the same
  struggling keycloak. Waiting for one request is the better failure mode. When
  that request fails, the next waiting thread tries again. This is the same number
  of attempts as today, only serialised.
- [A clock skew between client and server larger than the margin would still
  cause 401s] → The 401 retry stays as the safety net. With working NTP the skew
  is below a second, and the logger's analysis on 2026-09-24 confirmed that.
- [`renewAt` depends on `expires_in` being sane. A value of 0 or a missing field
  would make every call fetch a token] → This is no worse than today, where such a
  token expires immediately as well. A test covers that a 2-second token is reused
  within its first second.
- [Concurrency tests can be flaky] → Overlap is forced with latches and a token
  endpoint delay, not left to chance. The assertions count grants, not timings.

## Migration Plan

Release 1.0.12, then bump the version in the consumers. There is nothing to
migrate. Rollback means pinning 1.0.11 again.

## Open Questions

- **Production:** the access-token lifetime is 15 minutes (`expires_in` 900,
  reported by Gerald on 2026-10-02). The margin is therefore 30 s. Over the three
  hours on 2026-10-02 that gives 12 token expiries; 6 of them ended in a 401,
  which fits a token sent in its last moments about every other time.
- **Staging:** the access-token lifetime is 5 minutes (`expires_in` 300,
  reported by Gerald on 2026-10-02). The margin is again 30 s. Over the same three
  hours that gives 36 token expiries; 20 of them ended in a 401 -- the same
  "about every other time" ratio as on production.

Both values are known. Neither changes the design. After rollout, the logger's
401 warnings on both systems should drop from about one per two token
lifetimes to near zero.
