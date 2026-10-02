## Purpose

Defines how the body of a successful answer reaches the caller of a builder, so that
binary content arrives byte for byte and text content is decoded as before.

## ADDED Requirements

### Requirement: A GET for byte[] returns the body unchanged

A GET call whose requested type is `byte[]` SHALL return the bytes of the answer's
body exactly as the server sent them, without decoding them as text.

This SHALL hold for the plain GET builder and for the keycloak GET builder alike,
and for a call that is repeated because the server refused its token with 401.

A successful answer with an empty body SHALL give an empty array, not `null`.

A non-2xx answer SHALL raise a `RestClientException` carrying its status, and a
transport failure SHALL answer `null` and be reported as the builder's last
exception, the same as for every other type.

#### Scenario: Every byte value survives a plain GET

- **WHEN** a plain GET for `byte[]` is answered with a body holding all 256 byte values
- **THEN** the call returns exactly those bytes in the same order

#### Scenario: Every byte value survives a keycloak GET after a 401

- **WHEN** a keycloak GET for `byte[]` is refused with 401 and repeated with a new token
- **AND** the repeat is answered with a body holding all 256 byte values
- **THEN** the call returns exactly those bytes

#### Scenario: An empty body gives an empty array

- **WHEN** a GET for `byte[]` is answered with 2xx and no body
- **THEN** the call returns an empty array

#### Scenario: A refusal still raises the status

- **WHEN** a GET for `byte[]` is answered with a non-2xx status
- **THEN** a `RestClientException` with that status is raised

### Requirement: Compression on the wire does not change a body

When the server compresses its answer with gzip, the client SHALL decompress it and
hand on exactly the bytes the server compressed.

#### Scenario: A compressed binary answer arrives unchanged

- **WHEN** a GET for `byte[]` is answered with a gzip-compressed body holding all 256 byte values
- **THEN** the call returns exactly those bytes

### Requirement: Other types are decoded from text as before

A GET for any type other than `byte[]` SHALL decode the body as text, using the
charset its content type declares or UTF-8 if it declares none, and then convert it
to the requested type as before.

#### Scenario: A String GET is unchanged

- **WHEN** a GET for `String` is answered with a UTF-8 text body
- **THEN** the call returns that text

#### Scenario: A compressed text answer is decoded as before

- **WHEN** a GET for `String` is answered with a gzip-compressed UTF-8 text body
- **THEN** the call returns that text
