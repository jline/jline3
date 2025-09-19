---
sidebar_position: 2
---

# Terminal

import CodeSnippet from '@site/src/components/CodeSnippet';

The Terminal is the foundation of JLine, providing access to the underlying terminal capabilities. This page explains how to create and use Terminal objects in your applications.

JLine provides a terminal abstraction using the `Terminal` interface which provides the following features:

- Signals support
- Input stream, output stream, non-blocking reader, writer
- Terminal size and parameters
- Infocmp capabilities

There are two different types of terminals:

- **System terminals** that handle an OS terminal window
- **Virtual terminals** to support incoming connections

## System Terminals

There is only a single system terminal for a given JVM, the one that has been used to launch the JVM. The terminal is the same terminal which is accessible using the [`Console` JDK API](https://docs.oracle.com/javase/8/docs/api/java/io/Console.html).

The easiest way to obtain such a `Terminal` object in JLine is by using the `TerminalBuilder` class:

```java
Terminal terminal = TerminalBuilder.terminal();
```

or

```java
Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
```

The `TerminalBuilder` will figure out the current Operating System and which actual `Terminal` implementation to use. Note that on the Windows platform you need to have either Jansi or JNA library in your classpath.

<CodeSnippet name="TerminalCreationExample" />

## Terminal Capabilities

You can check the capabilities of a Terminal:

<CodeSnippet name="TerminalCapabilitiesExample" />

## Terminal Output

You can write to the Terminal:

<CodeSnippet name="TerminalOutputExample" />

## Terminal Input

You can read from the Terminal:

<CodeSnippet name="TerminalInputExample" />

## Virtual Terminals

Virtual terminals are used when there's no OS terminal to wrap. A common use case is when setting up some kind of server with SSH or Telnet support. Each incoming connection will need a virtual terminal.

Those terminals can be created using the following pattern:

```java
Terminal terminal = TerminalBuilder.builder()
                    .system(false)
                    .streams(input, output)
                    .build();
```

The builder can also be given the initial size and attributes of the terminal if they are known:

```java
int columns = ...;
int rows = ...;
Attributes attributes = new Attributes();
// set attributes...
Terminal terminal = TerminalBuilder.builder()
                    .system(false)
                    .streams(input, output)
                    .size(new Size(columns, rows))
                    .attributes(attributes)
                    .build();
```

## Terminal Signals

You can handle terminal signals:

<CodeSnippet name="TerminalSignalsExample" />

JLine terminals support signals. Signals can be raised and handled very easily.

System terminals can be built to intercept the native signals and forward them to the default handler.

```java
Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .signalHandler(Terminal.SignalHandler.SIG_IGN)
                    .build();
```

The LineReader does support signals and handle them accordingly.

## Terminal Attributes

You can get and set terminal attributes:

<CodeSnippet name="TerminalAttributesExample" />

## Terminal Size

You can get and handle changes to the terminal size:

<CodeSnippet name="TerminalSizeExample" />

## Cursor Control

You can control the cursor position:

<CodeSnippet name="TerminalCursorExample" />

## Terminal Colors

You can use colors in the terminal:

<CodeSnippet name="TerminalColorsExample" />

## Terminal Building Options

The `TerminalBuilder` supports various options for customizing the terminal:

| Option | System | Virtual | Description |
|--------|--------|---------|-------------|
| name | x | x | Name of the terminal, defaults to `"JLine terminal"` |
| type | x | x | Infocmp type of the terminal, defaults to `System.getenv("TERM")` |
| encoding | x | x | Encoding of the terminal, defaults to `Charset.defaultCharset().name()` |
| system | x | x | Forces or prohibits the use of a system terminal |
| streams | | x | Use the given streams for the input / output streams of the terminal |
| jna | x | x | Allow or prohibits using JNA based terminals, defaults to whether the JNA library is available or not |
| dumb | x | | Creates a dumb terminal if known terminals are not supported, else an exception is thrown |
| attributes | | x | Initial attributes of the terminal |
| size | | x | Initial size of the terminal |
| nativeSignals | x | | Handle JVM signals from the system terminal through the created terminal |
| signalHandler | x | | Default signal handler |

## Terminal Types

JLine has available the following terminal types:

| OS/Terminal | Infocmp terminal type |
|-------------|------------------------|
| ANSI | ansi |
| Dumb | dumb, dumb-color |
| rxvt | rxvt, rxvt-basic, rxvt-unicode, rxvt-unicode-256color |
| Screen | screen, screen-256color |
| Windows/ConEmu | windows-conemu |
| Windows/cmd | windows, windows-256color, windows-vtp |
| xterm | xterm, xterm-256color |

## Pseudo-Terminals (PTY)

JLine's `Terminal` can be combined with [Pty4j](https://github.com/JetBrains/pty4j)
for a "[Pseudoterminal](https://en.wikipedia.org/wiki/Pseudoterminal)" (PTY).

## Best Practices

When working with Terminal objects in JLine, consider these best practices:

1. **Resource Management**: Always close the Terminal when you're done with it.

2. **Signal Handling**: Handle signals like SIGINT (Ctrl+C) appropriately.

3. **Terminal Attributes**: Save and restore terminal attributes when making changes.

4. **Terminal Size**: Handle terminal resize events to update your UI.

5. **Terminal Capabilities**: Check terminal capabilities before using advanced features.

6. **Error Handling**: Handle exceptions that may occur when creating or using a Terminal.

7. **Terminal Type**: Be aware of the terminal type and adjust your behavior accordingly.

8. **Terminal Input**: Use non-blocking input when appropriate.

9. **Terminal Output**: Flush the output after writing to ensure it's displayed.

10. **Terminal Colors**: Use colors consistently and provide fallbacks for terminals that don't support them.
