---
sidebar_position: 1.5
---

# What's New in JLine 4

JLine 4 is a major release that modernizes the library with new terminal providers, a powerful shell module, improved Unicode handling, and terminal graphics support. This page summarizes the key changes from JLine 3.x.

## Requirements

| | JLine 3.x | JLine 4.x |
|---|---|---|
| **Minimum Java** | Java 8 | **Java 11** |
| **Build JDK** | Java 8 | Java 22 (for FFM provider) |
| **Maven** | 3.x | 3.9+ |

:::tip JDK11 Classifier
If you're using Java 11-21 and encounter class file version errors, use the `jdk11` classifier to exclude the FFM provider (compiled with Java 22). See [Terminal Providers](./modules/terminal-providers.md) for details.
:::

## New Modules

### Shell Module (`jline-shell`)

A modern, clean API for building interactive command-line applications, replacing the old Console command infrastructure.

- **Fluent builder API** via `Shell.builder()`
- **Pipeline execution** with operators: `|`, `&&`, `||`, `;`, `>`, `>>`, `<`, `&`
- **Alias system** with parameter substitution (`$1`, `$@`)
- **Job control** — background execution, `jobs`/`fg`/`bg` commands
- **Variable expansion** — `$VAR`, `${VAR:-default}`, `~` home expansion
- **Script execution** — `source` / `.` commands with line continuation
- **Subcommands** — automatic routing and tab completion
- **Signal handling** — Ctrl-C interrupts the command, not the shell
- **Syntax highlighting** — known commands bold, unknown commands red, operators cyan
- **Built-in commands** — `history`, `help`, `setopt`/`unsetopt`/`setvar`

See [Shell: Getting Started](./shell-getting-started.md) and [Shell: Features](./shell-features.md).

### Prompt Module (`jline-prompt`)

A modern replacement for the Console UI module, built natively on JLine.

- **11+ prompt types**: list, checkbox, choice, input, confirm, password, number, toggle, editor, search, key press
- **Multi-column layouts** with automatic terminal width adaptation
- **Grid-based navigation** using arrow keys
- **Script integration** via `PromptCommands` for shell scripts and automation
- Full backward compatibility with the Console UI API

See [Prompt Module](./modules/prompt.md).

## Terminal Providers

JLine 4 replaces the Jansi and JNA providers with modern alternatives.

| Provider | JLine 3 | JLine 4 | Notes |
|---|:---:|:---:|---|
| **FFM** | -- | **New** | Foreign Function & Memory API (Java 22+), best performance |
| **JNI** | -- | **New** | Java Native Interface, works on Java 11+ |
| **Jansi** | Yes | **Removed** | Use JNI or FFM instead |
| **JNA** | Yes | **Removed** | Use JNI or FFM instead |
| **Exec** | Yes | Yes | External commands (`stty`, `tput`), no native access needed |
| **Dumb** | Yes | Yes | Pure-Java fallback |

Default provider order: **FFM, JNI, Exec** (Dumb is fallback-only).

### Migrating from Jansi

If you used Jansi purely for ANSI output, JLine 4's `AttributedString.toAnsi()` is a direct replacement — no `AnsiConsole.systemInstall()` needed. See [Terminal Providers: Migrating from Jansi](./modules/terminal-providers.md#migrating-from-jansi).

## New Terminal Features

### Terminal Graphics

Display images directly in the terminal via multiple protocols:

- **Sixel** — widely supported (xterm, iTerm2, foot, WezTerm)
- **Kitty Graphics Protocol** — modern, feature-rich
- **iTerm2 Inline Images** — iTerm2-specific

A unified `TerminalGraphics` interface handles protocol detection and fallback automatically. See [Terminal Graphics](./advanced/terminal-graphics.md).

### Grapheme Cluster Support

Proper handling of combining characters, emoji, and complex scripts via Unicode Mode 2027 (DECRQM probing). Updated to Unicode 16.0. See [Grapheme Cluster Mode](./advanced/grapheme-cluster-mode.md).

### Signal Handling via Panama FFM

Modernized POSIX signal handling using Panama FFM `sigaction()`, providing better Ctrl-C and Ctrl-Z behavior without JNI overhead.

### `/dev/tty` Fallback

When stdin/stdout are piped (e.g., via `exec-maven-plugin`), JLine can now open `/dev/tty` directly to access the controlling terminal:

```java
TerminalBuilder.builder().devTty(true).build();
```

## JPMS Support

JLine 4 provides full Java Platform Module System support with proper `module-info.java` descriptors:

- `org.jline.terminal` — core terminal functionality
- `org.jline.terminal.ffm` — FFM provider (Java 22+)
- `org.jline.reader` — line editing
- `org.jline.style` — styling and coloring
- `org.jline.builtins` — built-in commands
- `org.jline.shell` — shell framework
- `org.jline.prompt` — prompt module

See [JPMS documentation](./modules/jpms.md) for module usage and migration details.

## Deprecated APIs

### Console Module (`jline-console`)

The old command infrastructure is deprecated but still functional. Bridge adapters are provided for migration:

| Old (jline-console) | New (jline-shell) |
|---|---|
| `CommandRegistry` | `CommandGroup` |
| `SystemRegistry` | `CommandDispatcher` |
| `CommandMethods` + `CommandInput` | `Command.execute()` |
| `CmdDesc` | `CommandDescription` |
| `CmdLine` | `CommandLine` |
| `ArgDesc` | `ArgumentDescription` |

Use `CommandGroupAdapter` and `CommandRegistryAdapter` to bridge old and new APIs. See [Shell: Migration Guide](./shell-migration.md).

### Console UI Module (`jline-console-ui`)

Still functional but superseded by the [Prompt Module](./modules/prompt.md), which offers the same API with multi-column layouts, more prompt types, and better performance.

## Migration Checklist

1. **Update Java version** to 11 or higher
2. **Update Maven version** to 3.9 or higher
3. **Update dependency coordinates**:
   ```xml
   <dependency>
       <groupId>org.jline</groupId>
       <artifactId>jline</artifactId>
       <version>4.0.0</version>
   </dependency>
   ```
   Use `<classifier>jdk11</classifier>` if targeting Java 11-21.
4. **Remove Jansi/JNA dependencies** — native access is built-in via JNI and FFM
5. **Migrate Console API** to Shell if writing new code (or use adapters for existing code)
6. **Migrate Console UI** to Prompt module for new prompt code
7. **Add `--enable-native-access`** if using Java 22+ with modules:
   ```
   --enable-native-access=org.jline.terminal.ffm
   ```
8. **Test on target platforms** — provider auto-selection handles most cases
