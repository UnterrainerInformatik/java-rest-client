## Context

See proposal.md - Why. The relevant state today:

- `RestClient.call(...)` executes the request, throws a `RestClientException` on a
  non-2xx status and otherwise returns `response.body().string()`, i.e. the body
  decoded with the charset of its content type (UTF-8 if none).
- `BaseGetBuilder.provideCall` passes that String to `BaseBuilder.castTo`, which
  maps it to the requested type. For `byte[]` it falls through to the JSON mapper.
- `GzipInterceptor` always asks for `Accept-Encoding: gzip` (its "already set"
  check compares Strings by reference and is never true). Because it sets the
  header itself, OkHttp does not decompress transparently; the interceptor does,
  with `readUtf8()`, and wraps the resulting String in a new body.
- `KeycloakContext.execute` repeats a refused call by running the builder's
  `execute()` again, so whatever the plain path returns, the keycloak path returns
  too.

## Goals / Non-Goals

**Goals:**
- One code path for the status check, shared by the text and the byte read, so the
  error behaviour cannot drift apart.
- Bytes untouched from the socket to the caller, compressed or not.

**Non-Goals:**
- Streaming large bodies (`InputStream`). The documents in question are small; a
  `byte[]` is what the consumer hashes and stores.
- Binary answers for POST, PUT and DELETE. Nobody needs them yet.
- Fixing the reference comparisons in `GzipInterceptor` beyond what the body
  handling needs.

## Decisions

### Split `call` into "send and check" and "read"

A private `send(...)` executes the call and throws on a non-2xx status, exactly as
`call` does today. `call` (String) reads `body().string()` from its result as
before. A new package-private `getBytes(url, headers)` reads `body().bytes()`,
which gives an empty array for an empty body.

*Alternative:* read bytes everywhere and decode in `castTo`. That would move the
charset handling out of OkHttp for every existing caller, which is more change
than needed.

### Branch on `byte[].class` in `BaseGetBuilder.provideCall`

If the builder's type is `byte[].class`, the call goes through `getBytes` and its
result is returned as is; every other type keeps the String path and `castTo`.
The keycloak builder inherits this, and the 401 retry with it.

*Alternative:* a separate `getBytes()` builder method. It would add public API for
something the type token already expresses.

### Unpack gzip as bytes

`GzipInterceptor.unzip` reads the decompressed body with `readByteArray()` and
creates the new body from those bytes with the original content type. Text is
then decoded where it always was, in `body().string()`. For a body without a
charset or with UTF-8 this gives the same String as before.

## Risks / Trade-offs

- [A whole binary body is held in memory] → Acceptable for documents and keys; a
  streaming variant can be added if a consumer ever needs one.
- [A compressed text answer that declares a non-UTF-8 charset now decodes
  differently] → It decodes correctly now; before, it was garbled.

## Migration Plan

Release 1.0.14, then bump the version in the consumers. Nothing to migrate.
Rollback means pinning 1.0.13 again.
