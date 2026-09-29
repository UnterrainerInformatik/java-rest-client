## 1. A test server, and the failing tests first

- [x] 1.1 Build a test fixture on `com.sun.net.httpserver.HttpServer` (ephemeral port) that serves a token endpoint handing out numbered tokens and counting grants, and a resource endpoint that answers 401 for a scriptable set of tokens and 200 with a JSON body otherwise. Verify with a smoke test that a GET through a `KeycloakContext` succeeds against it
- [x] 1.2 Write the refusal test: the resource refuses token 1, and a POST is answered 200 after token 2. Verify it fails against the current code, because the caller gets a 401 and only one grant happened
- [x] 1.3 Write the double-refusal test: tokens 1 and 2 are refused. Verify the caller gets a `RestClientException` with status 401, the resource was called exactly twice and there were two grants. It fails today, because there is one call
- [x] 1.4 Write the no-repeat tests for 403 and 500. Verify one resource call each. This passes today and must keep passing
- [x] 1.5 Write the reuse test: two calls with a valid token lead to one grant. It passes today and must keep passing

## 2. The context replaces a refused token

- [x] 2.1 Add a public `invalidate()` to `KeycloakContext` that clears the access token, refresh token and timestamp. Verify with a test that the next call makes one more grant and carries the new token
- [x] 2.2 Add the package-private `execute(client, builder, call)` to `KeycloakContext`: update, header, call, and on 401 invalidate, update, header and call once. Log the cured case at warn with the URL. Verify 1.2 and 1.3 pass and 1.4 and 1.5 still do
- [x] 2.3 Make `Get`, `Post`, `Put` and `DelKeycloakBuilder.execute()` delegate to it, and remove `BaseKeycloakBuilder`. Verify with the refusal test repeated for each of the four methods

## 3. Cleanup and release

- [x] 3.1 Keep the fixture-based tests in their own class, separate from the manual smoke tests (`KeycloakContextTests`, `BuilderTests`, which need the VPN and stay untouched). Verify the new class passes with no network access
- [x] 3.2 Document the 401 behaviour and `invalidate()` in the README and in the Javadoc of the keycloak builders, including the note on non-idempotent POSTs. Verify `mvn javadoc:javadoc` produces no new warnings
- [x] 3.3 Set the version to 1.0.7 and verify that `mvn verify` succeeds
  - Note (2026-09-29): `mvn verify` passes except the manual smoke test `KeycloakContextTests`, whose token URL `/auth/realms/...` answers 404 on staging. It fails identically on the pre-change HEAD and is left untouched by decision.
