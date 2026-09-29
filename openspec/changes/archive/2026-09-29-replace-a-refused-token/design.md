## Context

The four keycloak builders each override `execute()` with the same three lines:
`kcc.update(client)`, set the `Authorization` header, then `super.execute()`.
`BaseKeycloakBuilder` does the first and last of those but no builder extends it,
because each has to extend its method-specific base (`BaseGetBuilder`,
`BasePostBuilder`, …).

`RestClient.call` throws `RestClientException` with the status whenever the
response is not successful. Transport failures are handed to the `onError`
consumer and reported through `getLastException()`. `headers` is a map, so
setting `Authorization` again replaces the old value. `execute()` resets
`lastException` and can be called twice on the same builder.

## Goals / Non-Goals

**Goals:**
- One place that decides what a 401 does to the token, for all four methods.
- Behaviour that can be tested without the network.

**Non-Goals:**
- Using `refresh_token`. It is fetched and stored but never used today, and a
  refresh on the same session the server just refused is not guaranteed to help.
  The password grant is.
- Retrying on 403 or on any other status.
- Making `update()` thread-safe. It is not today, and this change does not make
  that worse (see Risks).

## Decisions

**1. The logic lives in `KeycloakContext`, and the builders delegate to it.**
The context gets a package-private
`<T> T execute(RestClient client, BaseBuilder<T,?> builder, Supplier<T> call)`.
It runs `update`, sets the header, runs `call`, and on a `RestClientException`
with status 401 runs `invalidate`, `update`, the header and `call` once more.
Each builder's `execute()` becomes one line:
`return kcc.execute(client, this, super::execute)`.
- *Moving it into `BaseKeycloakBuilder`* was rejected. Java has single
  inheritance, and the builders need their method-specific bases. That is the
  reason the class is unused.
- `BaseKeycloakBuilder` is removed. It is package-visible only in its
  constructor, has no subclasses, and keeping it suggests a second way of doing
  this.

**2. `invalidate()` is public and nulls `accessToken`, `refreshToken` and
`refreshTimestamp`.** `update()` already treats a null token as "fetch".
Exposing the method costs nothing and lets a caller with other evidence, such as
a 401 on a hand-built call, use the same mechanism.

**3. A double 401 does not invalidate a second time.**
The token that was just fetched is the freshest one available. If the server
refuses it, a third grant would change nothing, and the next call will fetch
again on its own first 401 anyway. So it is one extra grant per refused call,
never a loop.

**4. Tests run against `com.sun.net.httpserver.HttpServer` on an ephemeral
port.**
One handler answers the token endpoint with a counter-numbered token. The
resource handler refuses tokens from a scripted set with 401. This covers every
scenario in the spec with no new dependency. They live in a new test class.
The existing `KeycloakContextTests` and `BuilderTests` are manual smoke tests
against staging (reachable only via VPN) and are left untouched.

## Risks / Trade-offs

- **A server that refuses for a reason a new token cannot fix** (a user without
  the role, a revoked account) now costs one extra grant per call. → This is
  bounded, and keycloak answers a password grant cheaply. The caller still sees
  the 401 exactly as before.
- **Concurrent calls through one context** can each see a 401 and each
  invalidate, which leads to a few redundant grants. → The outcome is still
  correct: every call ends with a valid token. The existing `update()` has the
  same race on expiry today.
- **A non-idempotent POST that the server partly processed and then answered
  with 401** would be sent twice. → A 401 is decided before the handler runs in
  every server this client talks to. It is still stated in the Javadoc of the
  keycloak builders.

## Migration Plan

Release as 1.0.7 and bump the CMS data logger to it. No caller change is needed.
Rollback means pinning 1.0.6.
