---
sidebar_position: 15
---

# Auto Indentation and Pairing

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides features for auto-indentation and auto-pairing of brackets and quotes, which can enhance the user experience when writing multi-line commands or code snippets.

## AutopairWidgets

A simple plugin that auto-closes, deletes and skips over matching delimiters in JLine. `AutopairWidgets` has been ported to JLine/Java from [zsh-autopair](https://github.com/hlissner/zsh-autopair/).

<CodeSnippet name="AutopairWidgetsExample" />

### Functionality

`AutopairWidgets` does 5 things for you:

1. **It inserts matching pairs** (by default, that means brackets, quotes and spaces):
   
   e.g. `echo |` => " => `echo "|"`

2. **It skips over matched pairs**:
   
   e.g. `cat ./*.{py,rb|}` => } => `cat ./*.{py,rb}|`

3. **It auto-deletes pairs on backspace**:
   
   e.g. `git commit -m "|"` => backspace => `git commit -m |`

4. **And does all of the above only when it makes sense to do so**. e.g. when the pair is balanced and when the cursor isn't next to a boundary character:
   
   e.g. `echo "|""` => backspace => `echo |""` (doesn't aggressively eat up too many quotes)

5. **Spaces between brackets are expanded and contracted**:
   
   e.g. `echo [|]` => space => `echo [ | ]` => backspace => `echo [|]`

**Note:** _In above examples cursor position is marked as |._

### Configuration

By default curly brackets `{}` are not paired by AutopairWidgets. Curly bracket pairing can be enabled by creating AutopairWidgets as:

```java
AutopairWidgets autopairWidgets = new AutopairWidgets(reader, true);
```

### Key Bindings

This plugin provides `autopair-toggle` widget that toggles between enabled/disabled autopair.

## Auto Indentation

Command line auto indentation has been implemented in `DefaultParser` and `LineReaderImpl` classes.

<CodeSnippet name="AutoIndentationExample" />

### Functionality

Widget `accept-line` calls `Parser`'s method `parser(...)` that in case of unclosed brackets will throw `EOFError` exception that is caught by `accept-line` widget. `EOFError` exception contains necessary information to add indentation and the next closing bracket to the buffer.

Note that the last closing bracket must be entered manually even when option `INSERT_BRACKET` has been set to `true`.

## Best Practices

When using auto-indentation and pairing in JLine, consider these best practices:

1. **Selective Enabling**: Enable these features only when they add value to the user experience, such as in REPL environments or code editors.

2. **User Control**: Always provide a way for users to toggle these features on/off.

3. **Consistent Indentation**: Use a consistent indentation size throughout your application.

4. **Bracket Handling**: Consider whether you want to include curly brackets in auto-pairing based on your application's needs.

5. **Documentation**: Document these features and their key bindings for your users.

6. **Testing**: Test these features with various input patterns to ensure they behave as expected.

7. **Combine with Syntax Highlighting**: These features work best when combined with syntax highlighting to provide visual feedback.

8. **Language-Specific Configuration**: Consider customizing these features based on the programming language or command syntax your application supports.
