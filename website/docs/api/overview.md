---
sidebar_position: 1
---

# API Overview

JLine provides a comprehensive API for building interactive command-line applications. This section provides an overview of the key interfaces and classes in JLine.

## Core Components

JLine's API is organized around several core components:

### Terminal

The `Terminal` interface represents the terminal device. It provides methods for:

- Reading input
- Writing output
- Querying terminal capabilities
- Handling signals
- Managing terminal attributes

Key classes:
- `Terminal`: The main interface
- `TerminalBuilder`: Factory for creating terminals
- `Attributes`: Terminal attributes (raw mode, echo, etc.)
- `Size`: Terminal dimensions

[View Terminal JavaDoc](https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/terminal/Terminal.java)

### LineReader

The `LineReader` interface provides line editing capabilities. It handles:

- Reading lines with editing
- History management
- Completion
- Syntax highlighting
- Key binding

Key classes:
- `LineReader`: The main interface
- `LineReaderBuilder`: Factory for creating line readers
- `ParsedLine`: Parsed command line
- `Candidate`: Completion candidate

[View LineReader JavaDoc](https://github.com/jline/jline3/blob/master/reader/src/main/java/org/jline/reader/LineReader.java)

### Completer

The `Completer` interface provides tab completion. Key classes:

- `Completer`: The main interface
- `StringsCompleter`: Completes from a list of strings
- `FileNameCompleter`: Completes file names
- `ArgumentCompleter`: Completes command arguments
- `TreeCompleter`: Hierarchical completion

[View Completer JavaDoc](https://github.com/jline/jline3/blob/master/reader/src/main/java/org/jline/reader/Completer.java)

### History

The `History` interface manages command history. Key classes:

- `History`: The main interface
- `DefaultHistory`: Standard history implementation
- `MemoryHistory`: In-memory history
- `FileHistory`: File-based history

[View History JavaDoc](https://github.com/jline/jline3/blob/master/reader/src/main/java/org/jline/reader/History.java)

### Parser

The `Parser` interface parses command lines. Key classes:

- `Parser`: The main interface
- `DefaultParser`: Standard parser implementation
- `ParsedLine`: Parsed command line

[View Parser JavaDoc](https://github.com/jline/jline3/blob/master/reader/src/main/java/org/jline/reader/Parser.java)

### Highlighter

The `Highlighter` interface provides syntax highlighting. Key classes:

- `Highlighter`: The main interface
- `DefaultHighlighter`: Standard highlighter implementation

[View Highlighter JavaDoc](https://github.com/jline/jline3/blob/master/reader/src/main/java/org/jline/reader/Highlighter.java)

## Utility Classes

JLine provides several utility classes:

### AttributedString

`AttributedString` represents styled text with ANSI colors and attributes:

- `AttributedString`: Immutable styled string
- `AttributedStringBuilder`: Builder for creating attributed strings
- `AttributedStyle`: Style attributes (color, bold, etc.)

[View AttributedString JavaDoc](https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/AttributedString.java)

### Display

`Display` manages the terminal display:

- `Display`: Terminal display manager
- `InfoCmp`: Terminal capability database
- `Curses`: Terminal control sequences

[View Display JavaDoc](https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/Display.java)

## Package Structure

JLine is organized into several packages:

- `org.jline.terminal`: Terminal handling
- `org.jline.reader`: Line reading and editing
- `org.jline.reader.impl`: Implementation classes
- `org.jline.reader.impl.completer`: Completion implementations
- `org.jline.reader.impl.history`: History implementations
- `org.jline.utils`: Utility classes
- `org.jline.builtins`: Built-in commands and widgets

## API Stability

JLine follows semantic versioning:

- Major version changes may include breaking API changes
- Minor version changes add functionality in a backward-compatible manner
- Patch version changes include backward-compatible bug fixes

Classes and methods marked with `@Deprecated` may be removed in future major versions.

## Thread Safety

Most JLine classes are not thread-safe. In particular:

- `Terminal` methods should be called from a single thread
- `LineReader` methods should be called from a single thread
- `History` can be accessed concurrently if properly synchronized

## Error Handling

JLine uses exceptions to indicate errors:

- `IOError`: I/O-related errors
- `UserInterruptException`: User pressed Ctrl+C
- `EndOfFileException`: End of input (Ctrl+D)
- `SyntaxError`: Syntax error in input

## Further Reading

For detailed API documentation, refer to the JavaDoc for each class. The JLine GitHub repository also includes examples demonstrating various aspects of the API.
