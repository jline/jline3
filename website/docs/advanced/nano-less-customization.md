---
sidebar_position: 17
---

# Nano and Less Customization

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides built-in support for customizing the behavior of the `nano` and `less` commands through configuration files. This page explains how to customize these commands to suit your needs.

## Overview

Both `nano` and `less` commands in JLine can be customized using configuration files similar to the standard [nanorc](https://www.nano-editor.org/dist/latest/nanorc.5.html) format. These configuration files are named `jnanorc` and `jlessrc` respectively.

<CodeSnippet name="NanoLessCustomizationExample" />

## Configuration Setup

To set up the configuration for `nano` and `less` commands, you need to create a `ConfigurationPath` object that specifies where to look for configuration files:

```java
ConfigurationPath configPath = new ConfigurationPath(
    Paths.get("/pub/myApp"),                           // application-wide settings
    Paths.get(System.getProperty("user.home"), ".myApp") // user-specific settings
);
```

Then, you can use this configuration path when creating the `Builtins` and `SystemRegistry` objects:

```java
Supplier<Path> workDir = () -> Paths.get("");
Builtins builtins = new Builtins(workDir, configPath, null);
SystemRegistry systemRegistry = new SystemRegistry(parser, terminal, workDir, configPath);
systemRegistry.setCommandRegistries(builtins);
```

## Configuration Options

The `jnanorc`/`jlessrc` file contains the default settings for `nano`/`less`. During startup, the command tries to read the user-specific settings first and then its application-wide settings. Application-wide settings are read only if user-specific settings do not exist.

The configuration file accepts a series of **set** and **unset** commands, which can be used to configure `nano`/`less` on startup without using command-line options.

In addition, the configuration file might have '**include** syntaxfile' commands to read self-contained color syntaxes from a syntax file. The syntax file parameter can have '\*' and '?' wildcards in its file name.

If neither application-wide nor user-specific settings are found at command startup, then syntax files are searched from a standard installation location: `/usr/share/nano`.

### Example Configuration

Here's an example of a `jnanorc` configuration file:

```
include /usr/share/nano/*.nanorc
set tabstospaces
set autoindent
set tempfile
set historylog search.log
```

Note that the history log is saved in the user-specific directory, and the file can be shared between `nano` and `less` applications.

## Theme System

JLine version > 3.21.0 includes a [Theme System](theme-system.md) that allows highlight rules to be specified in terms of token type names, mixins, and parser configurations, instead of hard-coded colors in nanorc files.

Optional theme system configuration is set by adding a '**theme** theme-system-file' command in `jnanorc`/`jlessrc` configuration files.

## Syntax Highlighting

JLine's nanorc [SyntaxHighlighter](syntax-highlighting.md) works with standard nanorc [syntax files](https://github.com/scopatz/nanorc).

JLine supports the following nanorc keywords:
- **syntax**: Defines a new syntax highlighting rule set
- **color**: Defines a color for a specific pattern
- **icolor**: Defines a case-insensitive color for a specific pattern

Other nanorc keywords are quietly ignored.

## Rebinding Keys

Key rebinding is not currently supported in JLine's implementation of `nano` and `less`.

## Best Practices

When customizing `nano` and `less` in JLine, consider these best practices:

1. **Configuration Organization**: Keep your configuration files organized and well-documented.

2. **Syntax Highlighting**: Use syntax highlighting to improve readability of code and other structured text.

3. **Theme System**: Use the theme system to maintain consistent highlighting across different file types.

4. **User-Specific Settings**: Provide sensible defaults in application-wide settings, but allow users to override them with user-specific settings.

5. **Documentation**: Document your customizations for other users.

6. **Testing**: Test your customizations with different types of files to ensure they work as expected.

7. **Performance**: Be mindful of performance, especially when defining complex syntax highlighting rules.

8. **Compatibility**: Try to maintain compatibility with standard nanorc syntax where possible.
