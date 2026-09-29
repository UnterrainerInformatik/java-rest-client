## 1. Failing tests first

- [x] 1.1 Make `KeycloakTestServer` record the request method of every resource call, exposed as `receivedMethods()` next to `receivedTokens()`. Verify that the existing `KeycloakTokenTests` still pass (`mvn test -Dtest=KeycloakTokenTests`)
- [x] 1.2 Add `HttpMethodTests`, parameterised over the eight builders (plain and keycloak × GET, POST, PUT, DELETE), asserting the method the server received. Verify it fails today only for the two DELETE builders, which the server sees as `GET`
- [x] 1.3 Add a test for a keycloak DELETE refused with 401 (token 1 refused): the server receives `DELETE` twice. Verify it fails today, because the server receives `GET` twice

## 2. Send the real method

- [x] 2.1 Make `RestClient.delPlain` pass `"DELETE"`, and make `getCall` set the method explicitly: `get()`, `delete()`, `method(method, body)` for POST and PUT, `IllegalArgumentException` for anything else. Verify that `HttpMethodTests` pass and `KeycloakTokenTests` and `LastExceptionTests` still pass

## 3. Cleanup and release

- [x] 3.1 Remove the entry "A DELETE call sends GET" from `ai/open-proposals.md` if it is still there, and change the `put()`/`del()` entry so that it no longer waits on this fix. Verify by reading the file
- [x] 3.2 Set the pom version to 1.0.8. Verify that `mvn verify` on JDK 21 passes, apart from the known staging smoke-test failure in `KeycloakContextTests`, and that the surefire report shows a non-zero test count for `HttpMethodTests`
