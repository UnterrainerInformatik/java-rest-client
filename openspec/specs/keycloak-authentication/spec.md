# keycloak-authentication Specification

## Purpose

Defines how a call made through a keycloak context obtains its access token, when
it reuses one, and when it replaces one, so that a token the server refuses is
never sent again on the strength of the client's own expiry estimate alone.

## Requirements

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

### Requirement: A context offers authenticated calls for every method

A keycloak context SHALL let a caller outside the library build an authenticated
GET, POST, PUT and DELETE call.

Each of them SHALL send the request method it is named for, SHALL carry the
context's access token as a bearer token, and SHALL follow the rules for fetching,
reusing and replacing the token that apply to every keycloak-authenticated call.

#### Scenario: An authenticated PUT

- **WHEN** a caller makes a PUT through a context
- **THEN** the server receives a PUT carrying the context's token as a bearer token
- **AND** the caller receives the server's answer

#### Scenario: An authenticated DELETE

- **WHEN** a caller makes a DELETE through a context
- **THEN** the server receives a DELETE carrying the context's token as a bearer token
- **AND** the caller receives the server's answer

#### Scenario: A refused PUT or DELETE gets a new token

- **WHEN** a PUT or DELETE made through a context is refused with 401
- **AND** keycloak issues a new token that the server accepts
- **THEN** the call is repeated once with the new token, using the same method
- **AND** the caller receives the server's answer

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
