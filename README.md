![GitHub forks](https://img.shields.io/github/forks/UnterrainerInformatik/java-rest-client?style=social) ![GitHub stars](https://img.shields.io/github/stars/UnterrainerInformatik/java-rest-client?style=social) ![GitHub repo size](https://img.shields.io/github/repo-size/UnterrainerInformatik/java-rest-client) [![GitHub issues](https://img.shields.io/github/issues/UnterrainerInformatik/java-rest-client)](https://github.com/UnterrainerInformatik/java-rest-client/issues)

[![license](https://img.shields.io/github/license/unterrainerinformatik/FiniteStateMachine.svg?maxAge=2592000)](http://unlicense.org) [![Travis-build](https://travis-ci.org/UnterrainerInformatik/java-rest-client.svg?branch=master)](https://travis-ci.org/github/UnterrainerInformatik/java-rest-client) [![Maven Central](https://img.shields.io/maven-central/v/info.unterrainer.commons/rest-client)](https://search.maven.org/artifact/org.webjars.npm/rest-client) [![Twitter Follow](https://img.shields.io/twitter/follow/throbax.svg?style=social&label=Follow&maxAge=2592000)](https://twitter.com/throbax)




# rest-client

A REST-client that uses OK-HTTP3.

## Binary answers

A GET for `byte[]` returns the body exactly as the server sent it, without decoding it as
text, so certificates, keys or any `application/octet-stream` arrive byte for byte. An
empty body gives an empty array. This works the same through a `KeycloakContext`,
including a call repeated after a 401:

```java
byte[] content = kcc.<byte[]>get(restClient, byte[].class)
        .addUrl(server).addUrl("fileblobs/download").addParam("id", "4711").execute();
```

Every other type is decoded from the body as text, as before. A non-2xx answer raises a
`RestClientException` with its status; a transport failure answers `null` and is reported
by `getLastException()`. A gzip-compressed answer is unpacked without changing its bytes.

## Keycloak

A `KeycloakContext` fetches an access token from keycloak with the password grant and
sends it as a bearer token on every call made through its builders
(`kcc.get(client, Type.class)`, `kcc.post(client, Type.class)`, `kcc.put(client, Type.class)`,
`kcc.del(client, Type.class)`). The token is reused until shortly before the expiry
keycloak reported for it: once less than 30 seconds of its lifetime are left (or less than
half of it, for tokens that live shorter than a minute), the next call fetches a new one
first. A token is therefore never sent in its last seconds. `kcc.getRefreshTimestamp()`
still returns the expiry keycloak reported.

### Sharing a context between threads

A `KeycloakContext` is safe to use from several threads at once. Threads that need a new
token at the same time wait for a single fetch and all send the token it returned. When a
call is refused with 401, the context discards the token only if it still holds the one
that call sent; if another thread has already replaced it, the newer token is kept and the
call is repeated with it.

### When the server refuses the token

A server can stop accepting a token before its expiry, for example after a keycloak
restart or a revoked session. A call through a keycloak builder that is answered with
**401** therefore discards the token, fetches a new one and repeats the call **once**:

- If the new token is accepted, the caller gets the answer as usual. A warning naming the
  URL is logged.
- If it is refused with 401 as well, the caller receives a `RestClientException` with
  `getStatusCode() == 401`. The call is not repeated a third time.
- Any other status (403, 500, …) or a transport failure is not repeated.

A call refused with 401 is sent twice. For a non-idempotent POST this is safe only if the
server rejects the token before it processes the request.

### Discarding the token yourself

`kcc.invalidate()` drops the current token, so the next call fetches a new one. Use it
when you learn about a refusal some other way, for example on a call you built by hand
with `kcc.getAccessToken()`.
