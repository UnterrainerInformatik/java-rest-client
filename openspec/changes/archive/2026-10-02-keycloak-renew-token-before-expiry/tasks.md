## 1. Measure first

- [x] 1.1 Request one token from each of the staging and production realms with the CMS data logger's credentials (from `ai/secrets/`, never committed) and note `expires_in`. Record both values under Open Questions in `design.md`. This needs the LAN/VPN. If the credentials are not available locally, ask Gerald for the two numbers instead
  - Note (2026-10-02): production is 15 min (900 s), reported by Gerald and recorded in `design.md`. Staging is 5 min (300 s), likewise reported by Gerald

## 2. Test server and the failing tests first

- [x] 2.1 Extend `KeycloakTestServer` with a settable `expires_in`, a multi-threaded executor, an optional delay on the token endpoint and a counter for the maximum number of grants in flight at once. Verify that all existing `KeycloakTokenTests` still pass unchanged
- [x] 2.2 Add a package-private, injectable clock (`LongSupplier`, default `System::currentTimeMillis`) to `KeycloakContext` and verify that the existing tests still pass
- [x] 2.3 Write the margin test: with `expires_in` 300, advance the clock to 271 s after the first grant and make a call. Verify that it fails against the current code (the old token is sent and there is one grant). After 2.1-2.2, the target is two grants, the new token in the header and a single resource call
- [x] 2.4 Write the short-lifetime tests: with `expires_in` 2, a call at +0.9 s reuses the token and a call at +1.1 s fetches a new one. Verify that the second one fails against the current code
- [x] 2.5 Write the concurrent-first-call test: 8 threads, released together by a latch, call through a fresh context while the token endpoint is delayed. Verify one grant, a maximum of one grant in flight, and `token-1` in all 8 headers. Verify that it fails against the current code
- [x] 2.6 Write the concurrent-refusal test: the server refuses `token-1`, and 8 threads first obtain `token-1` and are then released together. Verify exactly two grants in total, all 8 calls returning `ok`, and every retry carrying `token-2`. Verify that it fails against the current code
- [x] 2.7 Write the late-refusal test: after the context has moved from `token-1` to `token-2`, a call that sent `token-1` is refused. Verify that `token-2` is kept, there is no third grant, and the retry carries `token-2`

## 3. Implementation

- [x] 3.1 Store `renewAt = requestedAt + lifetime - min(30 s, lifetime / 2)` next to `refreshTimestamp`, and renew in `update()` when `now >= renewAt`. Verify that 2.3 and 2.4 pass and that `getRefreshTimestamp()` still returns the reported expiry (assert it in 2.3)
- [x] 3.2 Guard all token state with a private lock that is held across the token request. Have `update()` return the ensured token and `authorize()` put that value into the header. Replace the Lombok getters with hand-written ones that read under the lock. Verify that 2.5 passes
- [x] 3.3 Add the package-private `invalidate(sentToken)` that clears the state only if it still holds `sentToken`, and use it in `execute()`. Keep the public `invalidate()` unconditional. Verify that 2.6 and 2.7 pass and that the whole `KeycloakTokenTests` class is green

## 4. Docs and release

- [x] 4.1 Document the early renewal and the thread safety in the `KeycloakContext` Javadoc and the README. Verify that `mvn javadoc:javadoc` reports no new warnings
- [x] 4.2 Run all offline test classes, i.e. everything except the LAN smoke tests (`KeycloakContextTests`, `BuilderTests`), several times in a row (e.g. 5×) to catch flaky concurrency tests. Verify that every run is green
- [x] 4.3 Set the pom version to 1.0.12, the version this push will mint, and verify that `mvn verify` succeeds (apart from the known manual LAN smoke tests)
