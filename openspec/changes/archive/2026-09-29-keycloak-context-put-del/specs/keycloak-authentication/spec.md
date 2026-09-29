## ADDED Requirements

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
