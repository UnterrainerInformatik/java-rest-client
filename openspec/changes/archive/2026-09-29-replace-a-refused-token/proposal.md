## Why

`KeycloakContext.update()` fetches a new token only when its *own* notion of the
expiry has passed (`now > refreshTimestamp`). A 401 from the server is not an
input to that decision. A token that the server stops accepting early -- after a
keycloak restart, a cleared session or a revoked grant -- is therefore sent and
refused for the rest of its local lifetime. Every call made through a keycloak
builder fails in that window, and the caller can do nothing about it: the context
offers no way to drop the token.

The CMS data logger ran into exactly this on 2026-09-24. It was refused with 401
for 193 consecutive send cycles and held all its values back until a restart. The
clocks on all three machines agreed to within a second, so the cause was the
token, not time. Every service that uses this client has the same hole, which is
why it is fixed here and not in each caller.

## What Changes

- **A keycloak builder that is answered with 401 drops the token, fetches a new
  one and repeats the call once.** A second 401 is thrown to the caller as today:
  it is a real authorisation failure.
- **`KeycloakContext` can be told to forget its token.** A public `invalidate()`
  lets a caller that learns about a refusal some other way make the next call
  fetch a new token.
- **The four keycloak builders (`Get`, `Post`, `Put`, `Del`) share this
  behaviour.** They currently each duplicate the same `update` + `Authorization`
  header lines, and `BaseKeycloakBuilder` sits unused beside them.
- **Automated tests for the context that do not need a live keycloak.** The
  existing `KeycloakContextTests` is a manual smoke test against staging (VPN)
  and stays as it is.

No **BREAKING** change: callers that never get a 401 see no difference, and
callers that do get one see it only if the new token is refused as well.

## Capabilities

### New Capabilities

- `keycloak-authentication`: how a keycloak-authenticated call obtains, reuses
  and replaces its token, including what a refusal by the server does to it.

### Modified Capabilities

<!-- None: there are no specs in this repository yet. -->

## Impact

- **Changed:** `KeycloakContext`; `GetKeycloakBuilder`, `PostKeycloakBuilder`,
  `PutKeycloakBuilder`, `DelKeycloakBuilder` (and `BaseKeycloakBuilder`, which
  is either used by all four or removed).
- **Tests:** a local `com.sun.net.httpserver.HttpServer` that plays both keycloak
  and the resource server, so the refusal can be scripted. No new dependency.
- **Release:** 1.0.7. The CMS data logger then moves from 1.0.6 to 1.0.7. The
  logger keeps its own separate change, which makes values held by a send in
  progress survive a restart.
