## Context

`PutKeycloakBuilder` and `DelKeycloakBuilder` already exist, send the right method
(since `fix-delete-sends-get`) and run through `KeycloakContext.execute`, which
does the 401 recovery (since `replace-a-refused-token`). They are only unreachable:
their constructors are package-private and `KeycloakContext` has no factory for
them. See proposal.md for why this matters.

## Goals / Non-Goals

**Goals:**
- Reach the two existing builders from outside the package, the same way as
  `get` and `post`.

**Non-Goals:**
- Changing the builders, the token handling or the retry rules.
- Making the builder constructors public.

## Decisions

**Factories on the context, constructors stay package-private.** `put` and `del`
mirror `get` and `post` one to one (`new PutKeycloakBuilder<>(client, type, this)`).
A caller gets one way in for all four methods, and the builder can never be
created without its context. Making the constructors public was the alternative;
it would give two ways to do the same thing and widen the API for no gain.

**The name is `del`, not `delete`.** It matches `RestClient.del(...)` and the
`DelBuilder`/`DelKeycloakBuilder` naming used throughout the library.

**Tests go through the public entry points.** The keycloak rows in
`HttpMethodTests` and the `Method` enum in `KeycloakTokenTests` currently call the
package-private constructors. Switching them to `kcc.get/post/put/del` makes the
existing method and 401 tests cover the new factories without adding parallel
tests that assert the same thing. Being in the same package, the tests cannot
prove accessibility from outside; that follows from the methods being public on a
public class, which the compiler checks for consumers.

**No double-send caveat for PUT and DELETE.** `PostKeycloakBuilder` warns that a
401-refused POST is sent twice. PUT and DELETE are idempotent by HTTP semantics,
so repeating one after a 401 needs no such warning; their Javadoc already
describes the retry.

## Risks / Trade-offs

- [Local `mvn verify` fails in `dependency:analyze-only` on test-only log4j, a
  known and separate backlog item] → verify with `mvn test` and the CI command
  (`mvn -Dmaven.test.skip=true package`) instead; the release path is unaffected.
