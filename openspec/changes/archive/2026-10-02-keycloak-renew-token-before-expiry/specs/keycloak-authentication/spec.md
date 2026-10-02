## MODIFIED Requirements

### Requirement: A token is reused until it expires or is refused

A keycloak-authenticated call SHALL send the context's current access token.

The context SHALL fetch a new token when it has none, or when less than a renewal
margin of the token's lifetime is left. The lifetime is the one reported by
keycloak, counted from the moment the token was requested. The renewal margin is
30 seconds, or half the reported lifetime if that is shorter. Otherwise the
context SHALL reuse the token it holds.

A token SHALL NOT be sent once its renewal margin has been reached, even though
keycloak would still consider it valid.

#### Scenario: The first call fetches a token

- **WHEN** a call is made through a context that has never fetched a token
- **THEN** a token is fetched from keycloak before the call
- **AND** the call carries it as a bearer token

#### Scenario: A valid token is reused

- **WHEN** two calls are made through the same context while more than the renewal margin of its token's lifetime is left
- **THEN** keycloak is asked for a token once, not twice

#### Scenario: A token close to its expiry is renewed before it is sent

- **WHEN** a call is made through a context whose token has less than the renewal margin left
- **THEN** a new token is fetched from keycloak before the call
- **AND** the call carries the new token, not the old one
- **AND** the server is called once, without a refusal

#### Scenario: A short-lived token is renewed at half its lifetime

- **WHEN** keycloak reports a lifetime of 2 seconds for a token
- **AND** a call is made through the context more than 1 second after that token was requested
- **THEN** a new token is fetched before the call

## ADDED Requirements

### Requirement: Concurrent calls through one context share its token

A context SHALL be safe to use from several threads at once.

When several calls need a new token at the same time, the context SHALL fetch it
once, and all of them SHALL carry that token.

When a call is refused with 401, the context SHALL discard the token only if it
still holds the token that call sent. If another call has already replaced it, the
context SHALL keep the newer token, and the refused call SHALL be repeated with it
without another fetch.

Every call SHALL carry a complete token that keycloak issued, never a token that
has been discarded or one that is missing.

#### Scenario: Simultaneous first calls fetch one token

- **WHEN** several threads make a call through a context that has no token, at the same time
- **THEN** keycloak is asked for a token once
- **AND** every call carries that token

#### Scenario: Simultaneous refusals fetch one new token

- **WHEN** several threads make a call at the same time and all of them are refused with 401 for the same token
- **AND** keycloak issues a new token that the server accepts
- **THEN** keycloak is asked for one new token, not one per thread
- **AND** every call is repeated once with the new token and returns the server's answer

#### Scenario: A late refusal does not discard a newer token

- **WHEN** a call is refused with 401 after the context has already replaced the refused token with a newer one
- **THEN** the call is repeated with the newer token
- **AND** keycloak is not asked for another token
