# release-pipeline Specification

## Purpose

Defines which pushes to `master` produce a release of the library on Maven Central,
so that changes to files that are never shipped do not mint new versions.

## Requirements

### Requirement: Only pushes that touch shipped or public files release

A push to `master` SHALL produce a release (version bump, build and publication to
Maven Central) unless every file it changes lies under `ai/`, `openspec/` or
`.claude/`.

A push whose changed files all lie under those directories SHALL NOT bump the
version, build or publish anything.

A push that changes at least one file outside those directories SHALL release, and
the files under those directories that it also changes SHALL go along with it.

`README.md` and the pipeline definition itself SHALL be treated like any other
file outside those directories.

#### Scenario: A memory-only push does not release

- **WHEN** a push to `master` changes only files under `ai/`
- **THEN** no pipeline run starts and no new version is tagged or published

#### Scenario: An OpenSpec- or tooling-only push does not release

- **WHEN** a push to `master` changes only files under `openspec/` and `.claude/`
- **THEN** no pipeline run starts and no new version is tagged or published

#### Scenario: A mixed push releases

- **WHEN** a push to `master` changes a source file and files under `ai/`
- **THEN** a release is built and published as before

#### Scenario: A README-only push releases

- **WHEN** a push to `master` changes only `README.md`
- **THEN** a release is built and published

### Requirement: A release can still be started by hand

The pipeline SHALL remain startable manually, and a manual start SHALL release
regardless of which files the latest commits changed.

#### Scenario: Manual start after an internal-only push

- **WHEN** the last push changed only files under `ai/`
- **AND** the pipeline is started manually
- **THEN** a release is built and published
