## Context

All four builder families end up in `RestClient.call(method, …)` →
`getCall(…)`. `getCall` creates a `Request.Builder` and calls
`request.method(method, requestBody)` only when the method is POST or PUT.
`Request.Builder` defaults to GET, so the `"DEL"` that `delPlain` passes is
silently dropped. The keycloak builders go through the same path
(`KeycloakContext.execute` → `super::execute`), so fixing `getCall` fixes all
of them.

The test server `KeycloakTestServer` already plays the resource server on
loopback. It records tokens but not methods. `KeycloakTokenTests` has a
`Method` enum that builds each keycloak builder.

## Goals / Non-Goals

**Goals:**
- `getCall` sets the method explicitly for all four methods.
- A test fails if any builder sends the wrong method.

**Non-Goals:**
- `KeycloakContext.put()` / `del()` and public builder constructors. That is the
  second entry in `ai/open-proposals.md`, a separate change.
- A request body for DELETE. `BaseDelBuilder` has none today, and it gets none
  now.

## Decisions

**`delPlain` passes `"DELETE"`, and `getCall` switches on the method.**
The internal token becomes the real HTTP method name, so what is logged and
what is sent agree. The switch is:
- GET → `request.get()`
- DELETE → `request.delete()`
- POST, PUT → `request.method(method, requestBody)` as today
- anything else → `IllegalArgumentException`

The alternative was to keep `"DEL"` and map it inside `getCall`. That keeps a
private abbreviation alive for no benefit. Failing loudly on an unknown method
beats falling back to GET, and GET-by-default is exactly the bug.

**`request.delete()` over `request.method("DELETE", null)`.**
It is OkHttp's own shortcut. It sends an empty body with
`Content-Length: 0`, which every server this client talks to accepts. No body
at all would work too. The choice is not visible in the spec.

**The test server records the method, and one new test class checks all
eight builders.**
`KeycloakTestServer` gets a `receivedMethods()` list next to
`receivedTokens()`. A plain builder can call the same `/resource` endpoint: it
carries no token, and the server answers 200. The new class `HttpMethodTests`
is parameterised over the eight builders (plain and keycloak × four methods)
and asserts the method the server saw. One more test covers a keycloak DELETE
refused with 401: both calls arrive as `DELETE`. It lives in
`HttpMethodTests`, which reuses the `Method` enum idea without depending on
`KeycloakTokenTests`.

Extending `KeycloakTokenTests` instead was rejected. That class is about the
token, and the method is a separate capability with its own spec.

## Risks / Trade-offs

- [A caller relied on DEL sending GET] → Checked 2026-09-29: none of the three
  known consumers uses DEL. Other users of the public artifact would get the
  method they asked for, which the proposal marks as BREAKING.
- [A server rejects a DELETE that carries `Content-Length: 0`] → Unlikely. If
  it happens, switch to `method("DELETE", null)`, which is a one-line change
  with no spec impact.

## Migration Plan

Release as 1.0.8 by pushing to `master`, after the tests pass locally.
Consumers do not have to bump. To roll back, re-release the previous
behaviour; nothing is persisted.
