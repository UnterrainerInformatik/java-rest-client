![GitHub forks](https://img.shields.io/github/forks/UnterrainerInformatik/java-rest-client?style=social) ![GitHub stars](https://img.shields.io/github/stars/UnterrainerInformatik/java-rest-client?style=social) ![GitHub repo size](https://img.shields.io/github/repo-size/UnterrainerInformatik/java-rest-client) [![GitHub issues](https://img.shields.io/github/issues/UnterrainerInformatik/java-rest-client)](https://github.com/UnterrainerInformatik/java-rest-client/issues)

[![license](https://img.shields.io/github/license/unterrainerinformatik/FiniteStateMachine.svg?maxAge=2592000)](http://unlicense.org) [![Travis-build](https://travis-ci.org/UnterrainerInformatik/java-rest-client.svg?branch=master)](https://travis-ci.org/github/UnterrainerInformatik/java-rest-client) [![Maven Central](https://img.shields.io/maven-central/v/info.unterrainer.commons/rest-client)](https://search.maven.org/artifact/org.webjars.npm/rest-client) [![Twitter Follow](https://img.shields.io/twitter/follow/throbax.svg?style=social&label=Follow&maxAge=2592000)](https://twitter.com/throbax)




# rest-client

A REST-client that uses OK-HTTP3.

## Keycloak

A `KeycloakContext` fetches an access token from keycloak with the password grant and
sends it as a bearer token on every call made through its builders
(`kcc.get(client, Type.class)`, `kcc.post(client, Type.class)`). The token is reused until
the expiry keycloak reported for it has passed.

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
