---
sidebar_position: 1
---

# Library Integration

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine can be integrated with various command-line parsing libraries to create powerful interactive applications. This guide explains how to integrate JLine with popular libraries like Spring Shell, Apache Commons CLI, JCommander, and picocli.

## Spring Shell

Spring Shell is a framework that provides a powerful infrastructure for building command-line applications using Spring. It uses JLine for its interactive shell capabilities.

<CodeSnippet name="SpringShellJLineExample" />

In a real Spring Shell application, you would define commands using Spring's component model and annotations, and Spring Shell would handle the integration with JLine automatically.

## Apache Commons CLI

Apache Commons CLI is a library for parsing command-line options. It can be integrated with JLine to create interactive command-line applications.

<CodeSnippet name="CommonsCliJLineExample" />

This example shows how to integrate JLine with Commons CLI to create an interactive shell that parses commands using Commons CLI.

## JCommander

JCommander is a Java framework for parsing command-line parameters. It can be integrated with JLine to create interactive command-line applications.

<CodeSnippet name="JCommanderJLineExample" />

This example demonstrates how to integrate JLine with JCommander to create an interactive shell that parses commands using JCommander.

## Picocli

[Picocli](https://picocli.info) is a modern framework for building command-line applications. It has [built-in support for JLine](https://github.com/remkop/picocli/blob/main/picocli-shell-jline3/README.md), making it easy to create interactive applications.

<CodeSnippet name="PicocliJLineExample" />

This example shows how to integrate JLine with picocli to create an interactive shell that parses commands using picocli.

## Best Practices

When integrating JLine with command-line parsing libraries, keep these best practices in mind:

1. **Separation of Concerns**: Keep the command parsing logic separate from the interactive shell logic.

2. **Error Handling**: Provide clear error messages when command parsing fails.

3. **Help System**: Implement a comprehensive help system that explains available commands and their options.

4. **Tab Completion**: Configure tab completion to work with your command structure.

5. **History Management**: Configure history to save and restore command history between sessions.

6. **Terminal Configuration**: Configure the terminal to provide the best user experience for your application.

7. **Testing**: Test your application with various command inputs to ensure it behaves as expected.

8. **Documentation**: Document the available commands and their options for users.

By integrating JLine with a command-line parsing library, you can create powerful interactive applications that combine the rich input capabilities of JLine with the structured command parsing of the library.
