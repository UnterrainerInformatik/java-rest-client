## Why

`KeycloakContext` offers only `get(client, type)` and `post(client, type)`. The
constructors of `PutKeycloakBuilder` and `DelKeycloakBuilder` are package-private,
so code outside the library cannot make an authenticated PUT or DELETE at all.
Since 1.0.8 a keycloak DELETE really sends DELETE and every keycloak builder
recovers from a refused token, so nothing stands in the way of exposing the two.

## What Changes

- Add `KeycloakContext.put(client, type)`, returning a `PutKeycloakBuilder`.
- Add `KeycloakContext.del(client, type)`, returning a `DelKeycloakBuilder`.
- Both carry the context's bearer token and recover from a 401 exactly like
  `get` and `post` do today; no new behaviour in the builders themselves.
- The offline tests make their keycloak calls through the context's four entry
  points instead of the package-private constructors, so the public way in is
  what gets tested.
- README: list the four entry points in the keycloak section.

Nothing existing changes. Not breaking.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `keycloak-authentication`: a context offers authenticated calls for all four
  methods (GET, POST, PUT, DELETE), not only GET and POST.

## Impact

- Code: `KeycloakContext` (two new public methods), tests in
  `KeycloakTokenTests` and `HttpMethodTests`, `README.md`.
- API: additive only. Existing callers compile and behave as before.
- Consumers (data logger, overmind server, elite server): not affected until they
  choose to use the new methods; they then only bump the version. No caller has to
  change.
- Release: pom version 1.0.9.
