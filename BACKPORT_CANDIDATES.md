# Bug Fixes to Backport from master to jline-3.x

This document lists bug fixes from the master branch (JLine 4.x) that should be considered for backporting to the jline-3.x branch.

## High Priority Bug Fixes

### 1. Fix threading issues in Display and curses library
**Commit:** fbeac3fb66de854cb9805799c04f29693d291480  
**PR:** #1448  
**Date:** Oct 2, 2025

**Issue:** ConcurrentModificationException when using the curses library on Windows due to concurrent access to the Display class from signal handlers.

**Changes:**
- Added displayLock object to synchronize Display access in GUIImpl.java
- Synchronized signal handler (handle method) for WINCH events
- Synchronized redraw() method to prevent concurrent screen updates
- Enhanced Display class thread safety documentation
- Added thread safety test (ThreadSafetyTest.java)

**Files Modified:**
- curses/src/main/java/module-info.java
- curses/src/main/java/org/jline/curses/impl/GUIImpl.java
- curses/src/test/java/org/jline/curses/ThreadSafetyTest.java
- terminal/src/main/java/org/jline/utils/Display.java

**Backport Recommendation:** HIGH - Fixes crash/exception in production use


### 2. Fix setting line reader options via system properties
**Commit:** 1cf79bb566dce4c8449064e918c43771a0b4cba9  
**PR:** #1413  
**Date:** Sep 5, 2025  
**Author:** Keith Turner

**Issue:** Setting line reader options via system properties (e.g., `-Dorg.jline.reader.props.disable-event-expansion=on`) did not work because the code was comparing the enum name to the system property value instead of the key.

**Changes:**
- Fixed LineReaderImpl.java to compare against the option key instead of enum name
- Added test to verify system property configuration works

**Files Modified:**
- reader/src/main/java/org/jline/reader/impl/LineReaderImpl.java
- reader/src/test/java/org/jline/reader/impl/LineReaderImplTest.java

**Backport Recommendation:** HIGH - Breaks documented configuration mechanism


### 3. Fix single-digit options in Options parser
**Commit:** 60bea8fbf8f953a424782a18b0772a854af37d13  
**PR:** #1418  
**Issue:** #1396  
**Date:** Sep 9, 2025

**Issue:** Options parser incorrectly treated single-digit options like `-1` as non-option arguments, preventing the ls command's `-1` option from working.

**Changes:**
- Modified Options.java to only treat arguments as non-options when they have at least 3 characters and both the 2nd and 3rd characters are digits
- Allows single-digit options like `-1` and combined options like `-1a` to work correctly
- Added comprehensive tests

**Files Modified:**
- builtins/src/main/java/org/jline/builtins/Options.java
- builtins/src/test/java/org/jline/builtins/OptionsTest.java
- builtins/src/test/java/org/jline/builtins/PosixCommandsTest.java

**Backport Recommendation:** HIGH - Breaks standard command-line option parsing


### 4. Implement POSIX ** globstar semantics in PosixCommands
**Commit:** c0fdc69745e560d80a3a23ad4e35ad8018544260  
**PR:** #1424 (fixes #1399)  
**Date:** Sep 10, 2025

**Issue:** Glob patterns like `src/**/*.java` and `**/{foo,bar}/*.txt` were being treated as literal filenames instead of being expanded.

**Changes:**
- Fixed glob character detection logic in maybeExpandGlob()
- Implemented proper POSIX ** semantics (matches 0 or more directories)
- Transform `**/` to `{**/,}` to handle both zero and one-or-more directory cases
- Added proper resource management with try-with-resources
- Enhanced test coverage

**Files Modified:**
- builtins/src/main/java/org/jline/builtins/PosixCommands.java
- builtins/src/test/java/org/jline/builtins/PosixCommandsTest.java

**Backport Recommendation:** HIGH - Breaks glob expansion functionality


### 5. Fix PosixCommands.grep incorrect context logic
**Commit:** 7a2a8de230ee5a8df99724cdec3aa944a8867dc9  
**PR:** #1390  
**Issue:** #1389  
**Date:** Aug 25, 2025  
**Author:** Paul King

**Issue:** grep command had incorrect context logic for displaying lines before/after matches.

**Changes:**
- Fixed context line handling in grep command
- Added comprehensive tests

**Files Modified:**
- builtins/src/main/java/org/jline/builtins/PosixCommands.java
- builtins/src/test/java/org/jline/builtins/PosixCommandsTest.java

**Backport Recommendation:** MEDIUM - Incorrect output but not a crash


### 6. Fix PosixCommands ls, cat, grep glob argument handling
**Commit:** 178e18f73dc6948b0f72c63bca845b2744ffca8a  
**PR:** #1400  
**Issue:** #1399  
**Date:** Aug 25, 2025  
**Author:** Paul King

**Issue:** ls, cat, and grep commands didn't handle glob arguments correctly.

**Changes:**
- Fixed glob expansion in ls, cat, and grep commands
- Optimized stream usage
- Added tests

**Files Modified:**
- builtins/src/main/java/org/jline/builtins/PosixCommands.java
- builtins/src/test/java/org/jline/builtins/PosixCommandsTest.java

**Backport Recommendation:** HIGH - Breaks basic command functionality


### 7. Catch UnsupportedOperationException in PosixCommands
**Commit:** 1718f5d937134f917f766cfc20e815767289caba  
**PR:** #1387  
**Date:** Aug 25, 2025  
**Author:** Paul King

**Issue:** UnsupportedOperationException prevented further Windows handling from executing.

**Changes:**
- Added proper exception handling for UnsupportedOperationException

**Files Modified:**
- builtins/src/main/java/org/jline/builtins/PosixCommands.java

**Backport Recommendation:** MEDIUM - Windows-specific fix


## Lower Priority Fixes

### 8. Clean up posix commands and improve ls formatting
**Commit:** 28e0be76a6ca624a234758e086462b4fe499e414  
**PR:** #1407  
**Date:** Aug 28, 2025

**Changes:**
- Clean up posix commands implementation
- Correctly pad all fields in ls with a single format call
- Various fixes and improvements

**Files Modified:**
- builtins/src/main/java/org/jline/builtins/PosixCommands.java
- builtins/src/main/java/org/jline/builtins/Source.java

**Backport Recommendation:** LOW - Cosmetic improvements


### 9. Display currentdir as dot not blank
**Commit:** 5511f179d715685a9aa42eb5640ebf0081545cdc  
**PR:** #1398  
**Issue:** #1397  
**Date:** Aug 25, 2025  
**Author:** Paul King

**Issue:** ls command displayed current directory as blank instead of dot.

**Changes:**
- Display current directory as "." in ls output
- Updated test

**Files Modified:**
- builtins/src/main/java/org/jline/builtins/PosixCommands.java
- builtins/src/test/java/org/jline/builtins/PosixCommandsTest.java

**Backport Recommendation:** LOW - Minor formatting issue


### 10. Fix typo and clarification in sort command
**Commit:** 9d5a51560d5872743d6931c6c81682dd27b44589  
**PR:** #1404  
**Date:** Aug 26, 2025  
**Author:** Paul King

**Changes:**
- Fixed typo in sort command
- Slight clarification in documentation

**Files Modified:**
- builtins/src/main/java/org/jline/builtins/PosixCommands.java

**Backport Recommendation:** LOW - Documentation fix


## Summary

**High Priority (should backport):** 7 commits
**Medium Priority (consider backporting):** 2 commits  
**Low Priority (optional):** 3 commits

## How to Backport Using the Backport Tool

The repository is now configured to use the [backport CLI tool](https://github.com/sorenlouv/backport) which properly links PRs and maintains commit history.

### Installation

```bash
npm install -g backport
```

### Usage

To backport a specific PR to jline-3.x:

```bash
# Backport a single PR
backport --pr 1448

# Backport multiple PRs
backport --pr 1448 1413 1418

# Interactive mode - select from recent PRs
backport
```

The tool will:
1. Create a new branch from jline-3.x
2. Cherry-pick the commits from the PR
3. Create a new PR targeting jline-3.x
4. Add the "backport" label
5. Link to the original PR in the description

### Alternative: Manual Cherry-Pick

If you prefer manual cherry-picking:

```bash
git checkout jline-3.x
git pull origin jline-3.x
git checkout -b backport-fix-threading
git cherry-pick fbeac3fb
# Resolve conflicts if any
git push origin backport-fix-threading
# Create PR manually targeting jline-3.x
```

### Recommended Backport Order

1. **First batch (critical fixes):**
   - #1448 - Threading issues (fbeac3fb)
   - #1413 - System properties (1cf79bb5)
   - #1418 - Single-digit options (60bea8fb)

2. **Second batch (glob fixes):**
   - #1424 - POSIX globstar (c0fdc697)
   - #1400 - Glob arguments (178e18f7)
   - #1390 - Grep context (7a2a8de2)

3. **Third batch (Windows/misc):**
   - #1387 - Windows exception (1718f5d9)

## Next Steps

1. Install the backport tool: `npm install -g backport`
2. Review each high-priority fix for compatibility with JLine 3.x codebase
3. Use `backport --pr <number>` to backport fixes to jline-3.x branch
4. Run full test suite on jline-3.x after backporting
5. Consider creating a JLine 3.30.7 patch release with these fixes

