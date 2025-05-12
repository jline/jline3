---
sidebar_position: 6
---

# REPL Console

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides built-in support for creating REPL (Read-Eval-Print Loop) consoles with features like console variables, scripts, custom pipes, widgets, and object printing.

## Overview

The REPL console module provides a rich interactive environment for command-line applications, similar to shells like Bash or Zsh, but with integration for JVM scripting languages.

<CodeSnippet name="ReplConsoleExample" />

## Integration with JVM Scripting Languages

JLine's REPL console can integrate with JVM scripting languages like Groovy, making it easy to create powerful interactive environments.

### Key Interfaces

1. **ScriptEngine** - Manages JVM scripting language variables and script execution.
2. **ConsoleEngine** - Extends the CommandRegistry interface. Manages console variables, console script execution, and object printing. ConsoleEngineImpl implements console commands: `show`, `del`, `prnt`, `pipe`, `alias`, `unalias`, and `slurp`.
3. **SystemRegistry** - Extends the CommandRegistry interface. Aggregates CommandRegistries, compiles command pipelines, and dispatches command executions to the command registries. SystemRegistryImpl implements commands: `help` and `exit`.

## Variables

JLine's REPL console provides a rich variable system for storing and manipulating data.

### Reserved Variable Names

- `PATH` - A list of directories from where scripts are searched
- `NANORC` - Nanorc configuration file
- `PRNT_OPTIONS` - `prnt` command default options
- `CONSOLE_OPTIONS` - Console options
- `_` - The last result of the command execution
- `exception` - The last exception
- `output` - Redirected print output if command also returns value
- `_args` - Command parameter list for the groovy script (used inside a script)
- `_buffer` - Closure that will call `lineReader.getBuffer()` (available for widget functions)
- `_reader` - `lineReader` (available for widget functions)
- `_widget` - Closure that will call `lineReader.callWidget()` (available for widget functions)
- `_widgetFunction` - Function that will be launched when executing widget
- `_pipe<N>` - Temporary variables used to execute command pipeline
- `_executionResult` - Command execution result
- `_return` - Used to return execution result from console script

### Variable Name Conventions

1. Variables whose name start with underscore character `_` are temporary variables that are deleted in the REPL loop.
2. Variables whose name is in CAMEL_CASE format are not deleted when using wildcard in delete command like `del *`.

### Variable Expansion

Variable expansion substitutes variable references with their values:

```
groovy-repl> # create a map and print it
groovy-repl> user=[name:'Pippo',age:10]
groovy-repl> prnt $user
name Pippo
age  10
groovy-repl> prnt ${user}.name
Pippo
```

Command object parameters can also be written directly using either Groovy object notation or JSON:

```
groovy-repl> prnt -s JSON [user:'pippo',age:10]
{
    "user": "pippo",
    "age": 10
}
groovy-repl> prnt -s JSON {user:pippo,age:10}
{
    "user": "pippo",
    "age": 10
}
```

## Scripts

JLine's REPL console supports two types of scripts:

### Console Scripts

In console scripts, application commands are entered just like you do interactively. Script parameters are accessed inside a script using the variables `$1`, `$2`, `$3`, and so on.

Note that inside a code block, command line must be either of the forms `:command arg1 arg2` or `var=:command arg1 arg2`. Use the `exit` command to exit and return a value from the script.

Console scripts have two built-in options: `-?` for script help and `-v` for verbose execution. Console script output cannot be redirected.

<CodeSnippet name="ConsoleScriptExample" />

### Groovy Scripts

A temporary list variable `_args` will be created to assign command parameters to the script. Application commands can be executed inside a script using statements like:

```java
SystemRegistry.get().invoke('prnt', '-s', 'JSON', map)
```

Groovy scripts have a built-in help option `-?`.

<CodeSnippet name="GroovyScriptExample" />

## Pipes and Output Redirection

The main purpose of pipes is to make things more concise and create a more familiar REPL console for those who are used to working in bash/zsh.

### Builtin Pipe Operators

#### Output Redirection

Command output/return value can be redirected to a variable:

```
groovy-repl> resp=widget -l
groovy-repl> resp
_tailtip-accept-line (_tailtip-accept-line)
_tailtip-backward-delete-char (_tailtip-backward-delete-char)
...
```

Note that no spaces are allowed between variable/command and equal sign.

Command output can be redirected to a file using standard redirection operators `>` and `>>`.

#### Logical Pipe Operators

- The _and_ operator (`&&`) executes the second command only if the execution of the first command succeeds.
- The _or_ operator (`||`) executes the second command only if the execution of the first command fails.

Note that all pipe operators must be enclosed by space characters, and the command line cannot be enclosed by parentheses or quotes.

#### Flip Pipe Operator

The flip pipe operator `|;` flips around the command and argument:

```
groovy-repl> widget -l |; prnt -s JSON
[
    "_tailtip-accept-line (_tailtip-accept-line)",
    "_tailtip-backward-delete-char (_tailtip-backward-delete-char)",
    ...
]
```

#### Named Pipe Operator

The reserved pipe operator `|` is used by custom named pipes and pipeline aliases.

### Custom Pipe Operators

Pipe operator name convention: If a pipe operator name contains only alphanumeric characters, it is a named pipe operator and will be used with the operator `|`.

```
# Custom Groovy pipes
pipe |.  '.collect{' '}'
pipe |:  '.collectEntries{' '}'
pipe |:: '.collectMany{' '}'
pipe |?  '.findAll{' '}'
pipe |?1 '.find{' '}'
pipe |&  '.' ' '

# Named pipe and two pipe line aliases
pipe grep '.collect{it.toString()}.findAll{it=~/' '/}'
alias null '|& identity{}' 
alias xargs '|; %{0} %{1} %{2} %{3} %{4} %{5} %{6} %{7} %{8} %{9}'
```

Note that pipeline alias value starts with a known pipe operator.

## Widgets

In widget functions, you will have available temporary variables: `_reader` (`lineReader`), `_buffer` (closure that will call `lineReader.getBuffer()`), and `_widget` (closure that will call `lineReader.callWidget()`).

```groovy
# Create test-widget and bind it to ctrl-alt-x
# It will read widget name from buffer and execute it
def testWidget() {
    def name = _buffer().toString().split('\\s+')[0]
    _widget "$name"
}
widget -N test-widget testWidget 
keymap '^[^x' test-widget
```

## Best Practices

When using JLine's REPL console, consider these best practices:

1. **Consistent Variable Naming**: Follow the variable naming conventions to make your scripts more maintainable.

2. **Script Organization**: Keep scripts organized in a logical directory structure and set the PATH variable accordingly.

3. **Error Handling**: Implement proper error handling in your scripts, especially when integrating with external systems.

4. **Custom Pipes**: Create custom pipes for common operations to make your command lines more concise.

5. **Documentation**: Document your custom commands, widgets, and pipes for other users.

6. **Testing**: Test your scripts and commands thoroughly to ensure they work as expected.

7. **Security**: Be careful when executing user-provided scripts or commands, especially in multi-user environments.

8. **Performance**: Be mindful of performance, especially when working with large datasets or complex operations.
