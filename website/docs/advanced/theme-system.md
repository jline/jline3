---
sidebar_position: 16
---

# Theme System

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides a flexible theme system that allows you to customize the appearance of your terminal applications. This page explains how to use and configure the theme system.

## Overview

In the JLine theme system, highlight styles are specified in nanorc theme configuration files using token type names, mixins, and parser configurations, instead of hard-coded styles. This approach provides a more flexible and maintainable way to define syntax highlighting and other visual elements.

The theme system is particularly useful for:

- Creating consistent syntax highlighting across different file types
- Customizing the appearance of command-line interfaces
- Supporting different color schemes for different environments

## Functionality

Theme system configuration is set by adding a `theme theme-system-file` command in the `jnanorc` configuration file. At startup, theme system token type name configurations are copied to the CLI console map variable `CONSOLE_OPTIONS[NANORC_THEME]`.

All the REPL highlight styles are defined by either nano `syntaxHighlighter`s or console variables. Highlight styles in various configurations are specified using the theme system token type names.

The console variable `CONSOLE_OPTIONS[NANORC_THEME]` is recreated when executing the built-in command: `highlighter --refresh` or `highlighter --switch=<new-theme>`. The `highlighter` command will refresh also `lineReader`'s `COMPLETION_STYLE`s and all `syntaxHighlighter`s used by the REPL demo.

<CodeSnippet name="ThemeSystemExample" />

## Theme System File Syntax

### Token Type Name

Token type name configuration:

```
TOKEN_TYPE_NAME <style>
```

Where:
- `TOKEN_TYPE_NAME` is the name of the token type in camel case
- `<style>` is its style (see [nanorc style definition syntax](https://jline.org/docs/advanced/syntax-highlighting))

### Mixin

Mixin configuration:

```
+MIXIN_NAME TOKEN_TYPE_1 <regex> ... \n TOKEN_TYPE_2 <regex2> ... \n ...
```

Where:
- `MIXIN_NAME` is the name in camel case
- `TOKEN_TYPE_<n>` are the token type names

Mixin configurations are relevant only in nanorc syntax highlighting.

### Parser Configurations

The theme system implements three parsing configurations for line comments, block comments, and literal strings:
- `$LINE_COMMENT`
- `$BLOCK_COMMENT`
- `$BALANCED_DELIMITERS`

The literal string and comment delimiters parsing configuration is placed in the nanorc syntax file, and syntax highlight rules are in the system theme file.

nanorc syntax file:

```
$LINE_COMMENT        "<delimiter>"
$BLOCK_COMMENT       "<start_delimiter>, <end_delimiter>"
$BALANCED_DELIMITERS "<delimiter_1>, <delimiter_2>, ..."
```

nanorctheme file:

```
$PARSER TOKEN_TYPE \n TOKEN_TYPE_2: <condition> \n ...
```

Where:
- `<condition> = <regex> ... | startWith=<value> | continueAs=<regex>`
- `TOKEN_TYPE_<n>` are the token type names

Parser configurations are relevant only in nanorc syntax highlighting.

## Example

Theme system configuration file:

```
BOOLEAN     brightwhite
NUMBER      blue
CONSTANT    yellow
COMMENT     brightblack
DOC_COMMENT white
TODO        brightwhite,yellow
WHITESPACE  ,green
#
# mixin
#
+LINT   WHITESPACE: "[[:space:]]+$" \n WHITESPACE: "\t*"
#
# parser
#
$LINE_COMMENT   COMMENT \n TODO: "(FIXME|TODO|XXX)"
$BLOCK_COMMENT  COMMENT \n DOC_COMMENT: startWith=/** \n TODO: "(FIXME|TODO|XXX)"
```

nanorc syntax file using the above system theme configurations:

```
BOOLEAN:  "\b(true|false)\b"
CONSTANT: "\b[A-Z]+([_]{1}[A-Z]+){0,}\b"
~NUMBER:  "\b(([1-9][0-9]+)|0+)\.[0-9]+\b" "\b[1-9][0-9]*\b" "\b0[0-7]*\b" "\b0x[1-9a-f][0-9a-f]*\b"
$LINE_COMMENT:        "//"
$BLOCK_COMMENT:       "/*, */"
+LINT
```

Equivalent self-contained nanorc syntax file that does not use the theme system:

```
color brightwhite "\b(true|false)\b"
color yellow      "\b[A-Z]+([_]{1}[A-Z]+){0,}\b"
icolor blue       "\b(([1-9][0-9]+)|0+)\.[0-9]+\b" "\b[1-9][0-9]*\b" "\b0[0-7]*\b" "\b0x[1-9a-f][0-9a-f]*\b"
color brightblack "//.*"
color brightblack start="^\s*/\*" end="\*/"
color white       start="/\*\*" end="\*/"
color brightwhite,yellow "(FIXME|TODO|XXX)"
color ,green      "[[:space:]]+$"
color ,green      "\t*"
```

**Note:** In the case of a self-contained nanorc syntax file, the highlighter uses only regexes for highlighting.

## Creating Nanorc Themes

You can define your nanorc theme using a 16-color palette and switch between different color themes by changing your terminal palette. Available themes and installation instructions can be found in projects like:
- [Gogh](https://mayccoll.github.io/Gogh/)
- [iTerm2-Color-Schemes](https://github.com/mbadolato/iTerm2-Color-Schemes)
- [microsoft/ColorTool](https://github.com/microsoft/terminal/tree/main/src/tools/ColorTool)

If your terminal color palette is not easily replaceable (or a 16-color palette is not enough), you can use hard-coded color definitions in your nanorc theme file. A nanorc theme with hard-coded color definitions can be created using `nanorctheme.template`, `apply-colors.sh`, and [Gogh](https://gogh-co.github.io/Gogh/).

On Linux:

```bash
cd git/jline3
./build rebuild
cd demo/target/nanorc
bash -c "$(wget -qO- https://git.io/vQgMr)"
```

## Best Practices

When using the JLine theme system, consider these best practices:

1. **Consistent Naming**: Use consistent token type names across your theme files.

2. **Reuse Mixins**: Create mixins for common patterns to avoid duplication.

3. **Theme Organization**: Keep your theme files organized and well-documented.

4. **Testing**: Test your themes in different terminal environments to ensure compatibility.

5. **Color Accessibility**: Consider color blindness and other accessibility concerns when designing themes.

6. **Documentation**: Document your themes and their intended use cases.

7. **Version Control**: Keep your theme files under version control to track changes.

8. **Sharing**: Share your themes with the community to benefit others.
