# Project Guidelines

This rule file contains branching, commit, PR, and task-finding conventions for the project. Commands read this file to determine how to name branches, format commits, and search for tasks.

- **Fix branch:** `fix/<ISSUE_NUMBER>`
- **Feature branch:** `feature/<ISSUE_NUMBER>-<short-slug>`
- **Bugfix branch:** `bugfix/<ISSUE_NUMBER>`
- **Quick-fix branch:** `quick-fix/<short-slug>`
- **CI-issue branch:** `ci-issue/<short-slug>`
- **Commit format (fix):** `fix: <brief description> (fixes #<ISSUE_NUMBER>)`
- **Commit format (feat):** `feat: <brief description> (#<ISSUE_NUMBER>)`
- **Commit format (quick-fix):** `chore: <brief description>`
- **Commit format (ci-issue):** `ci: <brief description>`
- **PR creation:** always
- **Merge policy:** before merging a PR, complete all three steps:
  1. **Label** — categorize with the appropriate label (`bug`, `enhancement`, `fix`, `documentation`, `build`, `chore`, `test`, `dependencies`, etc.). Labels drive the release notes.
  2. **Milestone** — assign to the target release milestone (check open milestones: e.g., `4.4.0`, `3.30.17`, `5.0.0`). Use the next patch/minor for the branch the PR targets.
  3. **Assign** — assign the PR to its author (or the contributor who did the work). Do not change existing assignees on linked issues.
- **Find-task source:** GitHub labels
- **Find-task beginner label:** `good first issue`
- **Find-task experienced label:** `help wanted`
- **Scope-too-large redirect:** create a GitHub issue

## Version
b1ca3bf14c7f191083ce24523e4098bbccd01a73
