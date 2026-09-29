# http-methods Specification

## Purpose

Defines which HTTP method each builder sends, so that a call reaches the server
as the method the caller chose, with or without keycloak authentication.

## Requirements

### Requirement: Each builder sends the method it is named for

A GET builder SHALL send `GET`, a POST builder `POST`, a PUT builder `PUT` and a
DELETE builder `DELETE`.

This SHALL hold for the plain builders and for their keycloak-authenticated
counterparts alike. It SHALL also hold for a call that is repeated because the
server refused its token with 401.

A DELETE call SHALL NOT be sent as any other method.

#### Scenario: A plain builder sends its method

- **WHEN** a call is made with the plain GET, POST, PUT or DELETE builder
- **THEN** the server receives `GET`, `POST`, `PUT` or `DELETE` respectively

#### Scenario: A keycloak builder sends its method

- **WHEN** a call is made with the keycloak GET, POST, PUT or DELETE builder
- **THEN** the server receives `GET`, `POST`, `PUT` or `DELETE` respectively
- **AND** the call carries the context's bearer token

#### Scenario: A DELETE repeated after a 401 is still a DELETE

- **WHEN** a keycloak DELETE call is refused with 401 and repeated with a new token
- **THEN** the server receives `DELETE` both times
