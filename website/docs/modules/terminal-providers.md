---
sidebar_position: 5
---

# Terminal Providers

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine supports multiple terminal providers, each with its own characteristics and capabilities. This allows JLine to work across different platforms and environments.

:::note JLine 4.x Changes
JLine 4.x has removed the **JNA** and **Jansi** providers. These providers are no longer available and have been replaced by more modern alternatives.

The recommended providers for JLine 4.x are **JNI** (for maximum compatibility) and **FFM** (for best performance on Java 22+).
:::

## Provider Selection

JLine can automatically select the best terminal provider for your environment, or you can explicitly specify which provider to use:

<CodeSnippet name="ProviderSelectionExample" />

## ~~JNA Provider~~ (Removed in JLine 4.x)

:::danger Removed in JLine 4.x
The JNA (Java Native Access) provider has been **removed** in JLine 4.x. Use the **JNI** or **FFM** providers instead for native terminal functionality.
:::

## ~~Jansi Provider~~ (Removed in JLine 4.x)

:::danger Removed in JLine 4.x
The Jansi provider has been **removed** in JLine 4.x. Use the **JNI** or **FFM** providers instead for ANSI support and terminal functionality.
:::

## FFM Provider

The FFM (Foreign Function & Memory) provider uses Java's FFM API (available in Java 22+) to access native terminal functionality:

<CodeSnippet name="FfmTerminalExample" />

:::warning FFM Requires Java 22+ and Native Access
The FFM provider is compiled with Java 22 bytecode and requires:

- **Java 22 or later** to run
- **Native access permissions** at runtime: `--enable-native-access=org.jline.terminal.ffm` (module path) or `--enable-native-access=ALL-UNNAMED` (classpath)

If you're using Java 11-21, the FFM provider will be automatically skipped during provider selection, or you can use the `jdk11` classifier to exclude it entirely from your classpath.

See [JPMS documentation](./jpms.md#terminal-providers) for more details on native access configuration.
:::

### Using the JDK11 Classifier

If you're building your project with Java 11-21 and want to avoid Java 22 class files in your dependencies, use the `jdk11` classifier for the jline bundle:

**Maven:**

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>4.0.0</version>
    <classifier>jdk11</classifier>
</dependency>
```

**Gradle:**

```groovy
implementation 'org.jline:jline:4.0.0:jdk11'
```

The `jdk11` classifier artifact:

- Excludes the `org.jline.terminal.impl.ffm.*` classes (compiled with Java 22)
- Contains all other JLine functionality
- Compatible with Java 11-21
- Automatically uses JNI or Exec providers for native terminal access

This is particularly useful when:

- Your build tools warn about class file version mismatches
- You're targeting Java 21 or earlier
- You need to avoid dependencies with newer bytecode versions

**Note:** For JLine 3.x, use the `jdk8` classifier which is compatible with Java 8-21.

## JNI Provider

The JNI (Java Native Interface) provider uses JNI to access native terminal functionality:

<CodeSnippet name="JniTerminalExample" />

## Exec Provider

The Exec provider uses external commands to access terminal functionality:

<CodeSnippet name="ExecTerminalExample" />

## Dumb Terminal

The Dumb terminal is a fallback option that provides basic terminal functionality without advanced features:

<CodeSnippet name="DumbTerminalExample" />

## Using JLine for ANSI Output Only (No Native Access)

If you only need JLine for generating ANSI escape sequences (styled/colored text output) and don't need interactive line editing or terminal control, you can use JLine without any native access at all.

### Minimal Dependencies

The `jline-terminal` module contains `AttributedString`, `AttributedStyle`, and `AttributedStringBuilder` in the `org.jline.utils` package. These classes generate ANSI escape sequences without requiring any native code:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-terminal</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
```

### Generating ANSI Sequences Without a Terminal

`AttributedString.toAnsi()` can be called without a terminal instance:

```java
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

// Build styled text
AttributedStringBuilder sb = new AttributedStringBuilder();
sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
sb.append("Error: ");
sb.style(AttributedStyle.DEFAULT);
sb.append("something went wrong");

// Convert to ANSI escape sequence string — no terminal needed
String ansi = sb.toAnsi();
System.out.println(ansi);
```

This produces: `\033[1;31mError: \033[0msomething went wrong`

### Available Style Attributes

`AttributedStyle` supports:

- **Text attributes**: `bold()`, `faint()`, `italic()`, `underline()`, `blink()`, `inverse()`, `conceal()`, `crossedOut()`
- **Standard colors** (0-7): `foreground(AttributedStyle.RED)`, `background(AttributedStyle.BLUE)`
- **256-color palette**: `foreground(196)` (indexed color)
- **24-bit true color**: `foreground(r, g, b)`

### When You Need a Terminal

If you need a `Terminal` object (e.g., for `toAnsi(Terminal)` to adapt output to terminal capabilities) but don't want native access, use the `exec` or `dumb` provider:

```java
// Exec provider: uses external commands (stty), no native libraries
Terminal terminal = TerminalBuilder.builder()
    .provider("exec")
    .build();

// Dumb provider: pure Java, no external commands, no native access
Terminal terminal = TerminalBuilder.builder()
    .dumb(true)
    .build();
```

The `exec` provider gives full terminal capabilities on Unix-like systems by spawning external commands (`stty`, `tput`) instead of loading native libraries. The `dumb` provider is a pure-Java fallback with no native access at all.

### Which Provider Needs Native Access?

| Provider | Native Access | Java Version | Capabilities |
|----------|:---:|:---:|---|
| **FFM** | Yes | 22+ | Full terminal control via Foreign Function & Memory API |
| **JNI** | Yes | 11+ | Full terminal control via Java Native Interface |
| **Exec** | No | 11+ | Full terminal control via external commands (Unix) |
| **Dumb** | No | 11+ | Basic I/O only, no terminal control |
| *No terminal* | No | 11+ | ANSI generation only (`AttributedString.toAnsi()`) |

### Migrating from Jansi

If you previously used Jansi purely for ANSI output generation, the migration is straightforward:

| Jansi | JLine 4 Equivalent |
|-------|---------------------|
| `Ansi.ansi().fg(RED).a("text").reset()` | `new AttributedString("text", AttributedStyle.DEFAULT.foreground(RED)).toAnsi()` |
| `Ansi.ansi().bold().a("text").reset()` | `new AttributedString("text", AttributedStyle.BOLD).toAnsi()` |
| `AnsiConsole.systemInstall()` | Not needed — modern Windows terminals support ANSI natively |
| `AnsiConsole.out().println(...)` | `System.out.println(attributedString.toAnsi())` |

For Windows ANSI support: Windows 10+ (including Windows Terminal, ConEmu, and modern cmd.exe) handles ANSI escape sequences natively. `AnsiConsole.systemInstall()` is no longer needed in most cases.

## Best Practices

When working with terminal providers in JLine, consider these best practices:

1. **Auto-Selection**: Let JLine automatically select the best provider for your environment when possible.

2. **Fallback Strategy**: Implement a fallback strategy if a specific provider is not available.

3. **Feature Detection**: Check for terminal capabilities before using advanced features.

4. **Cross-Platform Testing**: Test your application on different platforms to ensure it works with different providers.

5. **Error Handling**: Handle terminal-related errors gracefully.

6. **Terminal Cleanup**: Always close the terminal when your application exits.

7. **Signal Handling**: Handle terminal signals (like SIGINT) appropriately.

8. **Terminal Size**: Be aware of terminal size and adapt your UI accordingly.

9. **Color Support**: Check for color support before using colors.

10. **Documentation**: Document which terminal providers your application supports.
