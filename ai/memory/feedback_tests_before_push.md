---
name: feedback_tests_before_push
description: CI does not run tests (-Dmaven.test.skip=true); run the relevant tests locally before commit/push
metadata:
  type: feedback
---

The pipeline builds with `-Dmaven.test.skip=true` and publishes straight to Maven Central, so
a push to `master` is a release. Run the tests that make sense for the change locally before
committing ([[reference_build_and_test]]), state which ones and why, and do not push with
failures.

**Why:** Gerald (2026-09-27, carried over from presserl): "tests immer nur die laufen lassen,
die auch Sinn ergeben. Nicht immer alle blind." Here it matters more: nothing between a push
and Maven Central checks anything.

**How to apply:** test classes of the touched code plus the fixture-based suites. Manual smoke tests that
need the VPN (see [[feedback_tests_welcome]]) are skipped with `-Dtest=…` and say so.
Ask before every push — it publishes ([[feedback_pom_version_forward]]).
