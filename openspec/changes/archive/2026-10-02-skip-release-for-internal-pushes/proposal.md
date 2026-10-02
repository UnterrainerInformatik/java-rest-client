## Why

Every push to `master` bumps the version, builds and publishes a release to Maven
Central -- including pushes that only touch project memory (`ai/`), OpenSpec
artifacts (`openspec/`) or Claude Code tooling (`.claude/`). None of these is
shipped in the jar, so such a push mints a release that is byte-for-byte the
previous one under a new number, and it makes keeping the memory up to date
expensive (each memory fix would be a release).

## What Changes

- The push trigger of the release pipeline ignores pushes whose changed files all
  lie under `ai/`, `openspec/` or `.claude/`. Such a push no longer bumps, builds
  or publishes.
- A push that changes at least one other file (code, `pom.xml`, `README.md`, the
  workflow itself, ...) releases exactly as before; internal files in it go along.
- `README.md` deliberately stays a release-triggering file.
- The manual trigger (`workflow_dispatch`) is unchanged and still releases.
- Pushing this change itself releases once, because the workflow file is not on
  the ignore list.

## Capabilities

### New Capabilities
- `release-pipeline`: which pushes to `master` produce a release on Maven Central and which do not.

### Modified Capabilities
<!-- none -->

## Impact

- `.github/workflows/pipeline.yml` (push trigger only; jobs unchanged).
- Consumers (data logger, overmind server, elite server): not affected, callers do
  not change. Release numbers simply stop advancing on internal-only pushes.
- Not BREAKING: no caller-visible library behaviour changes.
