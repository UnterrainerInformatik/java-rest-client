## 1. Failing tests first

- [x] 1.1 Add a `/binary` endpoint to `KeycloakTestServer` that answers with settable bytes, as `application/octet-stream`, gzip-compressed if the test asks for it and the request accepts gzip, and that honours refused tokens like `/resource`. Verify the existing tests still pass
- [x] 1.2 Add `BinaryResponseTests`: all 256 byte values through a plain GET for `byte[]`; verify it fails against the current code
- [x] 1.3 Add the keycloak case: `token-1` refused, the repeat with `token-2` returns all 256 byte values unchanged; verify it fails against the current code
- [x] 1.4 Add the gzip case: a gzip-compressed answer with all 256 byte values arrives unchanged; verify it fails against the current code
- [x] 1.5 Add the edge cases: an empty 2xx body gives an empty array; a 404 raises `RestClientException` with status 404; a String GET (plain and gzip-compressed, with non-ASCII text) returns the text unchanged

## 2. Implementation

- [x] 2.1 Split `RestClient.call` into a shared send-and-check step and the String read; add package-private `getBytes(url, headers)` reading `body().bytes()`. Verify the existing tests still pass
- [x] 2.2 Branch on `byte[].class` in `BaseGetBuilder.provideCall` to use `getBytes`. Verify 1.2, 1.3 and 1.5 pass
- [x] 2.3 Make `GzipInterceptor.unzip` read and rebuild the body as bytes with the original content type. Verify 1.4 and the gzip String case pass

## 3. Docs and release

- [x] 3.1 Document `byte[]` answers in the README and the `get(...)` Javadoc
- [x] 3.2 Run all offline test classes (everything except the LAN smoke tests `KeycloakContextTests`, `BuilderTests`) with JDK 21 and verify they are green; verify the CI build `mvn -Dmaven.test.skip=true -Prelease package` succeeds
- [x] 3.3 Set the pom version to 1.0.14, the version this push will mint (latest tag 1.0.13), and verify `mvn verify` builds it
- [ ] 3.4 Push to `master` and verify the pipeline publishes 1.0.14 to Maven Central
