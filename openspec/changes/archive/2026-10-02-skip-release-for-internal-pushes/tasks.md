## 1. Pipeline trigger

- [x] 1.1 Add `paths-ignore` with `ai/**`, `openspec/**`, `.claude/**` to the `push` trigger in `.github/workflows/pipeline.yml`, leaving `branches`, `workflow_dispatch` and the jobs unchanged; verify the file still parses as YAML (`python3 -c 'import yaml,sys; yaml.safe_load(open(".github/workflows/pipeline.yml"))'`) and `git diff` shows only the trigger lines

## 2. Documentation and memory

- [x] 2.1 Note in `ai/memory/reference_build_and_test.md` and `ai/memory/feedback_pom_version_forward.md` that pushes touching only `ai/`, `openspec/`, `.claude/` do not release and do not mint a version; verify by reading the entries back

## 3. Verification

- [x] 3.1 Run the offline tests (`KeycloakTokenTests,LastExceptionTests,HttpMethodTests,PackagingTests,RestClientExceptionTests`) on JDK 21 and verify all pass with a non-zero test count (no library code changes; this guards the release this push mints)
- [ ] 3.2 After the release push, verify on GitHub Actions that the next push changing only files under `ai/` or `openspec/` starts no PIPELINE run (manual check; covers the spec scenarios that cannot run offline)

## 4. Release

- [x] 4.1 Set `<version>` in `pom.xml` to the version this push mints -- the latest tag (`git fetch --tags; git tag --sort=-creatordate | head -1`, expected `1.0.12`) plus one, i.e. `1.0.13`; verify `git diff pom.xml` shows that version
