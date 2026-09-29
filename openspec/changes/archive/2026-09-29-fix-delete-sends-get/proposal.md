## Why

`RestClient.getCall` sets the request method only for POST and PUT. Every other
call keeps OkHttp's default, which is GET. `delPlain` passes `"DEL"`, so
`DelBuilder` and `DelKeycloakBuilder` send a **GET** and no DELETE endpoint can
be reached through this client. Found on 2026-09-29 while implementing
`replace-a-refused-token`. Its fixture test missed the bug because the resource
endpoint there ignores the method.

## What Changes

- **BREAKING (bug fix): `DelBuilder` and `DelKeycloakBuilder` send `DELETE`.**
  Before this change they sent `GET`. A server that answered those calls as GETs
  now receives the DELETE the caller asked for.
- Every builder sends the method it is named for. GET, POST and PUT stay as
  they are. GET is now set explicitly instead of being left to OkHttp's default.
- A fixture test on the local test server records which method arrived and
  checks it for every builder, plain and keycloak, so a builder that sends the
  wrong method fails the build.

## Capabilities

### New Capabilities

- `http-methods`: which HTTP method each builder puts on the wire.

### Modified Capabilities

<!-- None: `keycloak-authentication` governs the token, not the method. Its
     requirements are unchanged. -->

## Impact

- **Changed:** `RestClient` (`delPlain`, `getCall`). The builders are unchanged.
- **Tests:** `KeycloakTestServer` records the method of every resource call. A
  new test class covers all eight builders. No new dependency.
- **Consumers:** checked on 2026-09-29. None of `java-cms-data-logger`,
  `java-overmind-server` or `java-elite-server` uses `DelBuilder`,
  `DelKeycloakBuilder`, `del(` or `delPlain`. No consumer is affected, and none
  has to change. A bump is optional.
- **Release:** 1.0.8.
