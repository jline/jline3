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
