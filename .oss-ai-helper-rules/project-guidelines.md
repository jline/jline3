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
- **Merge policy:** when merging a PR, categorize it with a label (e.g., `bug`, `enhancement`, `documentation`) and assign it to the next milestone
- **Find-task source:** GitHub labels
- **Find-task beginner label:** `good first issue`
- **Find-task experienced label:** `help wanted`
- **Scope-too-large redirect:** create a GitHub issue

## Version
1735c1f2a22808a1a3d7837db0eca9714a66f4e8
