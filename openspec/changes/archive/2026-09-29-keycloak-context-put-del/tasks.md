## 1. Tests through the public entry points

- [x] 1.1 Change the keycloak rows of `HttpMethodTests` and the `Method` enum in `KeycloakTokenTests` to build their calls with `kcc.get/post/put/del(...)` instead of the package-private constructors. Verify that the test sources fail to compile only because `KeycloakContext.put` and `KeycloakContext.del` do not exist yet

## 2. The factories

- [x] 2.1 Add `put(client, type)` and `del(client, type)` to `KeycloakContext`, next to `get` and `post`, each returning the existing builder bound to this context. Verify with `mvn test -Dtest='HttpMethodTests,KeycloakTokenTests'`: the server receives `PUT` and `DELETE` with a bearer token, and `everyMethodRecoversFromARefusedToken` passes for PUT and DEL

## 3. Docs and cleanup

- [x] 3.1 In the keycloak section of `README.md`, list all four entry points (`kcc.get`, `kcc.post`, `kcc.put`, `kcc.del`). Verify by reading the section
- [x] 3.2 Confirm the entry "KeycloakContext has no put() and del()" is gone from `ai/open-proposals.md`. Verify by reading the file

## 4. Release preparation

- [x] 4.1 Set the pom version to 1.0.9. Verify that `mvn test` on JDK 21 passes apart from the known staging smoke-test failure in `KeycloakContextTests`, and that `mvn -Dmaven.test.skip=true package` (the CI command) builds
