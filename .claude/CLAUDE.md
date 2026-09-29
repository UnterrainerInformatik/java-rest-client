# CLAUDE Project — rest-client

## Working rules
- Memory is in `./ai/memory/MEMORY.md` (index) with one `.md` file per entry under `./ai/memory/`. Treat that index as the auto-memory; keep it in sync the same way you would the per-machine `~/.claude/projects/<cwd>/memory/MEMORY.md`. **Read it at the start of every session.**
- **Conversation language is German.** Always reply to the user in German — every response, from the first message of a session on. Everything written into the repo (identifiers, comments, Javadoc, log messages, docs, commit messages, OpenSpec artifacts) stays English.
- **No change outside an OpenSpec change** — code, docs and project configuration alike. Announce first, then `/opsx:propose`, implement via `/opsx:apply`. A diagnosis request means diagnose only. See `ai/memory/feedback_announce_changes_first.md` and `ai/memory/feedback_openspec_only_changes.md`.
- Backlog of drafted-but-not-proposed work lives in `./ai/open-proposals.md`.
- Secrets go to `./ai/secrets/` (git-ignored), never into tracked files — this repository is public.
- **A push to `master` is a release to Maven Central.** CI skips tests. Run the relevant tests first, set the pom version forward, and ask before every push.

## Repository
- Public OSS library `info.unterrainer.commons:rest-client` (GitHub `UnterrainerInformatik/java-rest-client`), a REST client on OkHttp 3 with Keycloak bearer-token support (`KeycloakContext` and the `*KeycloakBuilder`s).
- Java 21, Maven, Lombok, parent `info.unterrainer.commons:parent-pom`. Tests: JUnit 5.
- `openspec/` — specs and changes. `ai/` — memory, open proposals. Not shipped.
- Consumers (data logger, overmind server, elite server) are listed in `ai/memory/project_consumers.md`.

Build/test commands: `ai/memory/reference_build_and_test.md`.
