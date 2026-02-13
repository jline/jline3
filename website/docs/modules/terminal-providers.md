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

If you're using Java 11-21 (JLine 4.x) or Java 8-21 (JLine 3.x), the FFM provider will be automatically skipped during provider selection, or you can use the `jdk8` classifier to exclude it entirely from your classpath.

See [JPMS documentation](./jpms.md#terminal-providers) for more details on native access configuration.
:::

### Using the JDK8 Classifier

If you're building your project with Java 11-21 (or Java 8-21 with JLine 3.x) and want to avoid Java 22 class files in your dependencies, use the `jdk8` classifier for the jline bundle:

**Maven:**

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>%%JLINE_VERSION%%</version>
    <classifier>jdk8</classifier>
</dependency>
```

**Gradle:**

```groovy
implementation 'org.jline:jline:%%JLINE_VERSION%%:jdk8'
```

The `jdk8` classifier artifact:

- Excludes the `org.jline.terminal.impl.ffm.*` classes (compiled with Java 22)
- Contains all other JLine functionality
- Compatible with Java 11-21 (JLine 4.x) or Java 8-21 (JLine 3.x)
- Automatically uses JNI or Exec providers for native terminal access

This is particularly useful when:

- Your build tools warn about class file version mismatches
- You're targeting Java 21 or earlier
- You need to avoid dependencies with newer bytecode versions

## JNI Provider

The JNI (Java Native Interface) provider uses JNI to access native terminal functionality:

<CodeSnippet name="JniTerminalExample" />

## Exec Provider

The Exec provider uses external commands to access terminal functionality:

<CodeSnippet name="ExecTerminalExample" />

## Dumb Terminal

The Dumb terminal is a fallback option that provides basic terminal functionality without advanced features:

<CodeSnippet name="DumbTerminalExample" />

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
