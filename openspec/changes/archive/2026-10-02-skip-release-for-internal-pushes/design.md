## Context

`.github/workflows/pipeline.yml` triggers on every push to `master` and on
`workflow_dispatch`. Its jobs call two shared workflows (`bump-semver-workflow`,
`maven-central-workflow`) that derive the version from the last git tag. See
proposal.md for why internal-only pushes should not release.

## Goals / Non-Goals

**Goals:**
- Skip the whole run (bump included) for pushes touching only `ai/`, `openspec/`, `.claude/`.

**Non-Goals:**
- Changing the shared workflows or the jobs in this file.
- Skipping releases for documentation (`README.md`) or any other path.

## Decisions

- **`paths-ignore` on the `push` trigger** rather than a job-level `if:`.
  GitHub then does not start a run at all, so nothing is bumped or tagged and no
  skipped run clutters the Actions list. A job-level condition on commit messages
  (e.g. `[skip release]`) was rejected: it depends on discipline per commit and
  GitHub's own `[skip ci]` would also skip mixed pushes that must release.
- **Directory globs `ai/**`, `openspec/**`, `.claude/**`.** These hold exactly the
  non-shipped, internal files. `README.md` stays out (decided with the maintainer):
  README edits normally accompany code, and keeping the list to "internal only"
  keeps it unambiguous.
- **The workflow file is not ignored.** Ignoring it would let a broken pipeline
  change land silently; the cost is that this change releases once (1.0.13).

## Risks / Trade-offs

- [GitHub evaluates `paths-ignore` against the whole push; if it cannot compute the
  diff (e.g. >1000 commits) it runs anyway] → harmless: it releases, as today.
- [A shipped file moved under an ignored directory would stop triggering releases]
  → nothing under `ai/`, `openspec/`, `.claude/` is packaged; `PackagingTests` and
  the pom's resource config are unaffected.
- [Version numbers no longer advance on internal pushes] → intended; the
  "pom version forward" rule still holds because no tag is minted.

## Migration Plan

Push once (releases 1.0.13). Afterwards verify with the next internal-only push that
no run appears. Rollback: remove the `paths-ignore` block.
