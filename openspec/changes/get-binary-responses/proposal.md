## Why

Every response is read as text today: `RestClient` decodes the body into a String,
and the GET builders hand that String on to the type the caller asked for. A binary
answer (a DER certificate, a private key, any `application/octet-stream`) is
corrupted irreversibly on the way, so it cannot be fetched at all.
`java-cms-data-logger` needs exactly that now: it downloads documents from its
server with `GET /fileblobs/download?id=<n>` through a `KeycloakContext` (bearer
token, early renewal, the 401 retry) and checks a sha256 over the bytes it
received.

A second, hidden place has the same flaw. When a server compresses its answer, the
gzip interceptor unpacks it and decodes it as UTF-8 before anyone sees it. A
compressed binary answer would be corrupted there even if the builder kept the
bytes.

## What Changes

- **A GET whose type is `byte[]` returns the body's bytes unchanged.** This holds
  for the plain GET builder and for the keycloak GET builder alike, including a
  call that is repeated after a 401. An empty 2xx body gives an empty array, not
  `null`.
  ```java
  byte[] content = keycloak.<byte[]>get(restClient, byte[].class)
          .addUrl(server).addUrl("fileblobs/download").addParam("id", "4711").execute();
  ```
- **The gzip interceptor unpacks a compressed answer as bytes.** It no longer
  decodes it as UTF-8 and re-encodes it. Text answers are decoded later, as before.
- **Everything else stays as it is.** String, number, boolean and JSON types are
  decoded as today. A non-2xx answer still raises a `RestClientException` with its
  status. A transport failure still answers `null` and is reported by
  `getLastException()`. POST, PUT and DELETE are untouched.

No **BREAKING** change. Asking for `byte[]` used to try to parse the body as JSON
into a byte array, which no caller can have relied on for real binary content. The
only other visible difference: a compressed text answer that declares a charset
other than UTF-8 is now decoded in that charset instead of being garbled.

## Capabilities

### New Capabilities

- `response-bodies`: how a successful answer's body reaches the caller -- as raw
  bytes for `byte[]`, decoded as text otherwise -- and that compression on the wire
  does not change it.

### Modified Capabilities

<!-- None -->

## Impact

- **Changed:** `RestClient` (a package-private byte-returning GET next to the
  String one), `BaseGetBuilder` (a branch for `byte[].class`) and
  `GzipInterceptor` (unpacks to bytes). The public API is unchanged: callers pass
  `byte[].class` to the existing `get(...)` methods.
- **Tests:** new offline cases against a local `HttpServer` (all 256 byte values,
  plain and keycloak, after a 401, gzip-compressed, empty body, non-2xx, String
  unchanged). No new dependency.
- **Release:** 1.0.14 (1.0.13 was already minted by the CI-only push).
- **Consumers:** `java-cms-data-logger` bumps the version and uses `byte[].class`
  for its document download -- that is new code, not a change to existing calls.
  `java-overmind-server` and `java-elite-server` only bump the version; no caller
  has to change code.
