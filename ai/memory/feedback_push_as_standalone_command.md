---
name: feedback_push_as_standalone_command
description: After Gerald approves a push, run exactly `git push origin master` as its own command, never chained
metadata:
  type: feedback
---

Once a push is approved, commit in one command and push in a separate one that is exactly
`git push origin master` — no `&&`, `;`, pipes or extra flags.

**Why:** Gerald (2026-10-02): "das geht nicht, dass ich das dauernd machen muss. das ist extrem
lästig". `~/.claude/settings.json` allows `Bash(git push origin master)`, but only for that exact
command. A push chained behind `git add && git commit` went to the auto-mode classifier, which
denied it as a production deploy, and Gerald had to push by hand.

**How to apply:** Still ask before every push ([[feedback_tests_before_push]],
[[feedback_pom_version_forward]]); once approved, push yourself with the standalone command.
