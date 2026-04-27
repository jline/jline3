# CLAUDE.md — JLine3 Project Guide

## What is JLine?

JLine is a Java library for building interactive command-line applications. It provides terminal handling, line editing with history and completion, configurable key bindings, Unicode and mouse support, and higher-level features like syntax highlighting, prompts, and a shell framework. Think of it as Java's equivalent of GNU Readline, but significantly more capable.

## Build Commands

JLine uses the `mvx` wrapper (not `mvnw`). All commands are run from the project root.

```bash
./mvx rebuild                          # Full build
./mvx mvn install -DskipTests          # Build without tests
./mvx mvn install -DskipTests -pl terminal  # Build single module
./mvx mvn test -pl terminal            # Test single module
./mvx mvn test -pl terminal -Dtest=GraphemeClusterModeTest  # Single test class
./mvx mvn verify                       # Full build + tests
```

## Code Formatting

Spotless (Palantir Java Format) runs automatically during build. If it fails:

```bash
./mvx mvn spotless:apply -pl terminal  # Fix formatting in a module
./mvx mvn spotless:apply               # Fix formatting everywhere
```

## Project Structure

Key modules: `terminal`, `reader`, `native`, `style`, `builtins`, `console`, `console-ui`, `prompt`, `curses`, `remote-ssh`, `remote-telnet`, `groovy`, `demo`.

Terminal providers: `terminal-jni` (JNI), `terminal-ffm` (Java 22+ Foreign Function & Memory).

## Java Version

- **Build requires**: Java 22
- **Runtime target**: Java 11+

## Conventions

### Commit Messages

Format: `<type>: <description>` — optionally referencing issues with `(#1234)` or `(fixes #1234)`.

Types: `feat`, `fix`, `docs`, `chore`, `test`, `refactor`.

### No AI Attribution

Do NOT add `Co-Authored-By: Claude ...` lines to commits. Do NOT add "Generated with Claude Code" or similar attribution to PR descriptions. Commits and PRs should look like normal human contributions.

### Copyright Headers

All Java source files must have:

```java
/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
```

Spotless enforces this header automatically via `spotless:apply`.

### Code Style

- Compiler has `-Werror` enabled — warnings are errors
- Spotless enforces Palantir Java Format with import order: `java|javax, org, <others>, static`
- Follow existing patterns in the codebase (e.g., `hasFocusSupport()`/`trackFocus()` for terminal feature pairs)

## Branches and Releases

### Branch Structure

- **`master`** — main development branch for 4.x. All new features and refactoring land here first.
- **`4.0.x`** — maintenance branch for 4.0.x patch releases. Only bugfixes are cherry-picked here from master.
- **`jline-3.x`** — maintenance branch for 3.x patch releases. Only bugfixes are backported here.

### Build Wrappers

- `master` and `4.0.x`: use `./mvx` (Maven 4 with nisse extension for version derivation from git tags)
- `jline-3.x`: use `./mvnw` (standard Maven wrapper)

### Release Process

**4.x releases (master and 4.0.x):**
1. Create and push a tag matching `[0-9]*.[0-9]*.[0-9]*` on the correct branch
2. The `Manual Maven Release` workflow triggers on tag push
3. Version is derived from the tag by the nisse Maven extension
4. **Important**: tag `4.0.x` patch releases on the `4.0.x` branch, NOT on `master`
5. Artifacts must be manually published through central.sonatype.com portal after the workflow completes
6. Create GitHub release notes via `gh release create`

**3.x releases (jline-3.x):**
1. Trigger the `Manual Maven Release` workflow via `workflow_dispatch` on branch `jline-3.x`
2. Provide `version` (e.g., `3.30.11`) and `next-version` (e.g., `3.30.12-SNAPSHOT`) inputs
3. The workflow runs `./mvnw release:prepare release:perform`
4. Artifacts must be manually published through central.sonatype.com portal after the workflow completes
5. Create GitHub release notes via `gh release create`

### Backporting Workflow

1. Cherry-pick bugfix commits from `master` to `4.0.x` and/or `jline-3.x`
2. Create a PR against the target branch for CI validation
3. Merge, then release from the maintenance branch

## Website / Documentation

```bash
./mvx website build    # Build docs site
```

Docs live in `website/docs/`. Advanced terminal feature docs go in `website/docs/advanced/`.

## Testing Patterns

- JUnit 5 (`@Test` from `org.junit.jupiter.api`)
- Tests in same package as source (`org.jline.terminal.impl`) for access to package-private members
- Use `LineDisciplineTerminal` with `ByteArrayOutputStream` for testing terminal I/O without a real terminal
- See `MouseSupportTest` and `GraphemeClusterModeTest` for examples
