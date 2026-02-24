---
sidebar_position: 1
---

# Glossary

This glossary provides definitions for common terms used in JLine and terminal-related programming.

## Terminal

A terminal is a device or program that takes inputs and displays output from the terminating end of communication. In modern computing, terminals are typically software implementations that emulate the behavior of hardware terminals.

## Dumb Terminal

A dumb terminal only shows text and takes input but doesn't process input locally. All processing is done on the other end of the communication channel. In JLine, a dumb terminal is a fallback option when more advanced terminal capabilities aren't available.

## Console

Originally, a console was a physical cabinet that contained a terminal. In software development, the terms "terminal" and "console" are often used interchangeably. In JLine, the console typically refers to the interactive environment where users enter commands and view output.

## TTY

TTY is an abbreviation for "teletypewriter," which was the first type of terminal. In modern systems, TTY refers to the device files that represent terminals in Unix-like operating systems.

## Shell

A shell is a command interpreter that processes commands entered in a terminal. Examples include Bash, Zsh, and PowerShell. JLine provides functionality that is often used to build shell-like applications.

## PTY

PTY (pseudoterminal) is a program that sits between a terminal and a shell. It allows more privileged programs to run and provides terminal emulation. JLine uses PTYs to provide advanced terminal functionality.

## Parser

A parser is a program, function, or method that takes input (like command lines) and formats or interprets it to make it meaningful. In JLine, parsers are used to interpret command lines, handle quoting, and support features like command completion.

## ANSI

ANSI (American National Standards Institute) defines standards for character encoding. In terminal programming, ANSI often refers to the ANSI escape codes used for terminal control, such as changing text colors or moving the cursor.

## Escape Codes

Escape codes are special sequences of characters that, when printed to a terminal, cause the terminal to perform operations beyond simply displaying text. These operations include changing text colors, moving the cursor, clearing the screen, etc.

## ANSI Escape Codes

ANSI escape codes are a specific set of escape codes defined by the ANSI standard. They begin with the ESC character (ASCII 27) followed by a bracket character ([) and then one or more characters that define the operation to perform.

## LineReader

In JLine, a LineReader is a component that reads lines of text from a terminal, providing features like line editing, history, and completion.

## Completer

A Completer is a component in JLine that provides tab completion functionality, suggesting possible completions for partially typed commands or arguments.

## Highlighter

A Highlighter is a component in JLine that provides syntax highlighting for command lines, making them more readable and easier to understand.

## Widget

In JLine, a Widget is a function that can be bound to a key or key sequence to perform a specific action, such as moving the cursor, deleting text, or completing a command.

## REPL

REPL stands for Read-Eval-Print Loop, which is a type of interactive programming environment that reads user input, evaluates it, prints the result, and then loops back to read more input. JLine provides components for building REPL environments.

## JNI

JNI (Java Native Interface) is a Java framework that allows Java code to call native (platform-specific) code and vice versa. JLine's `jline-terminal-jni` module uses JNI to access native terminal functionality such as reading terminal size, setting raw mode, and handling signals. JNI works with all Java versions supported by JLine but requires `--enable-native-access` on JDK 24+.

## FFM

FFM (Foreign Function & Memory) is a Java API introduced as a preview in JDK 19 and finalized in JDK 22 ([JEP 454](https://openjdk.org/jeps/454)). It provides a pure-Java way to call native code without traditional JNI boilerplate. JLine's `jline-terminal-ffm` module uses FFM for native terminal access and requires Java 22+. FFM is the recommended provider for Java 22+ as it avoids the need for pre-compiled native libraries. Like JNI, it requires `--enable-native-access` on JDK 24+.

## Native Access

Native access refers to calling platform-specific (non-Java) code from Java. JLine uses native access for features like raw terminal mode, terminal size detection, and signal handling on Unix and Windows. JLine provides multiple strategies: JNI (pre-compiled native libraries), FFM (Java 22+ built-in), Exec (spawning external commands like `stty`), and Dumb (no native access at all). See [Terminal Providers](../modules/terminal-providers.md) for details.

## Sources

- [What's The Difference Between A Console, A Terminal, And A Shell?](https://www.hanselman.com/blog/WhatsTheDifferenceBetweenAConsoleATerminalAndAShell.aspx)
- [ANSI escape code - Wikipedia](http://en.wikipedia.org/wiki/ANSI_escape_code)
