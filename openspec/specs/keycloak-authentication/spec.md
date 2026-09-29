# keycloak-authentication Specification

## Purpose

Defines how a call made through a keycloak context obtains its access token, when
it reuses one, and when it replaces one, so that a token the server refuses is
never sent again on the strength of the client's own expiry estimate alone.

## Requirements

### Requirement: A token is reused until it expires or is refused

A keycloak-authenticated call SHALL send the context's current access token.

The context SHALL fetch a new token when it has none, or when the token's expiry
as reported by keycloak has passed. Otherwise it SHALL reuse the token it holds.

#### Scenario: The first call fetches a token

- **WHEN** a call is made through a context that has never fetched a token
- **THEN** a token is fetched from keycloak before the call
- **AND** the call carries it as a bearer token

#### Scenario: A valid token is reused

- **WHEN** two calls are made through the same context while its token has not expired
- **THEN** keycloak is asked for a token once, not twice

### Requirement: A call refused with 401 is repeated once with a new token

When the server answers a keycloak-authenticated call with **401**, the context
SHALL discard the token it sent, fetch a new one and repeat the same call once
with it.

This SHALL happen regardless of whether the context still considered the refused
token valid.

If the repeated call is refused with 401 as well, the refusal SHALL reach the
caller as it does today, carrying status 401. The call SHALL NOT be repeated a
third time.

Any other outcome -- a different status, a transport failure, keycloak itself
being unreachable -- SHALL NOT cause a repeat.

A 401 that a new token cured SHALL be logged at warning level, naming the URL, so
that a server which keeps invalidating sessions stays visible.

#### Scenario: The server stops accepting a token that has not expired

- **WHEN** the server refuses a call with 401 while the context considers its token valid
- **AND** keycloak issues a new token that the server accepts
- **THEN** the call returns the server's answer to the caller
- **AND** keycloak was asked for a token once more than before
- **AND** a warning names the URL

#### Scenario: The new token is refused as well

- **WHEN** the server refuses a call with 401
- **AND** it refuses the repeated call with the new token with 401 as well
- **THEN** the caller receives a failure carrying status 401
- **AND** the server was called exactly twice

#### Scenario: Other refusals are not repeated

- **WHEN** the server answers a call with a status other than 401
- **THEN** the server was called once, and the caller receives that status

#### Scenario: Every method recovers the same way

- **WHEN** a GET, POST, PUT or DELETE made through a context is refused with 401 and the new token is accepted
- **THEN** each of them returns the server's answer to the caller

### Requirement: A caller can make the context forget its token

A context SHALL offer a way to discard its current token. The next call through it
SHALL fetch a new one.

#### Scenario: A discarded token is not sent again

- **WHEN** a caller tells the context to discard its token
- **AND** a call is then made through it
- **THEN** that call fetches a new token first and carries the new one
