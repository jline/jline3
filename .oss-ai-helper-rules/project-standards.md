# Project Standards

This rule file contains build tools, commands, and code style constraints for the project. Commands read this file to determine how to build, test, and format code.

- **Build tool:** Maven (via `./mvx` wrapper)
- **Build command:** `./mvx mvn verify -B`
- **Test command:** `./mvx mvn test -B`
- **Test with coverage command:** _(none)_
- **Format command:** `./mvx mvn spotless:apply -B`
- **Module-specific build:** yes (use `-pl <module>` to build/test a single module)
- **Parallelized Maven:** no
- **Code style restrictions:**
  - Spotless enforces Palantir Java Format with import order: `java|javax, org, <others>, static`
  - Compiler has `-Werror` enabled — warnings are errors
  - All Java source files must have BSD 3-Clause copyright header (enforced by Spotless)
  - Java 22 required for build, Java 11+ runtime target
  - Do NOT add `Co-Authored-By: Claude ...` lines to commits
  - Do NOT add "Generated with Claude Code" or similar attribution to PR descriptions

## Version
1735c1f2a22808a1a3d7837db0eca9714a66f4e8
