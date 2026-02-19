---
sidebar_position: 8
title: "Shell: Features"
---

import CodeSnippet from '@site/src/components/CodeSnippet';

# Shell Features

The `jline-shell` module provides a rich set of features for building interactive command-line applications. This page covers the key extensibility points and built-in capabilities.

## Aliases

The alias system allows users to define short names for frequently used commands.

### Setting Up

Wire an `AliasManager` into the shell builder:

```java
Shell shell = Shell.builder()
    .aliasManager(new DefaultAliasManager())
    .build();
```

For persistence across sessions, provide a file path:

```java
Path aliasFile = Paths.get(System.getProperty("user.home"), ".myapp-aliases");
DefaultAliasManager aliasManager = new DefaultAliasManager(aliasFile);
aliasManager.load();
```

### Built-in Commands

When an `AliasManager` is configured, the shell automatically registers:

- `alias` — list all aliases, define an alias (`alias name=expansion`), or show one (`alias name`)
- `unalias` — remove an alias (`unalias name`)

### Parameter Substitution

Alias expansions support parameter markers:

- `$1`, `$2`, ... — positional arguments
- `$@` — all arguments

```
alias grp=grep $1 $2
grp pattern file.txt    → grep pattern file.txt

alias e=echo $@
e hello world           → echo hello world
```

### Demo

<CodeSnippet name="ShellAliasExample" />

---

## Built-in Commands

The shell provides several opt-in command groups that can be enabled via the builder.

### History

```java
Shell.builder().historyCommands(true)
```

Adds the `history` command:

| Usage | Description |
|-------|-------------|
| `history` | List all entries |
| `history N` | List last N entries |
| `history -c` | Clear history |
| `history -s` | Save history to file |
| `history /pattern` | Search by regex |

### Help

```java
Shell.builder().helpCommands(true)
```

Adds the `help` command (also aliased as `?`):

| Usage | Description |
|-------|-------------|
| `help` | List all commands grouped by category |
| `help <command>` | Show detailed help for a specific command |

### Options

```java
Shell.builder().optionCommands(true)
```

Adds commands for runtime LineReader configuration:

| Command | Description |
|---------|-------------|
| `setopt OPTION` | Enable a LineReader option |
| `setopt` | List enabled options |
| `unsetopt OPTION` | Disable a LineReader option |
| `setvar NAME VALUE` | Set a LineReader variable |
| `setvar` | List all variables |

---

## Syntax Highlighting

The `CommandHighlighter` provides command-aware syntax highlighting:

- **Known commands** are highlighted in bold
- **Unknown commands** are highlighted in red
- **Pipeline operators** (`|`, `&&`, `||`, `;`, etc.) are highlighted in cyan

### Enabling

```java
Shell.builder().commandHighlighter(true)
```

### Custom Highlighters

You can provide a custom `Highlighter` implementation:

```java
Shell.builder().highlighter(myHighlighter)
```

Or combine both — the command highlighter can delegate to a custom highlighter:

```java
Shell.builder()
    .highlighter(myLanguageHighlighter)
    .commandHighlighter(true)
```

---

## Job Control

The shell supports background execution and job management.

### Enabling

```java
Shell.builder().jobManager(new DefaultJobManager())
```

### Usage

| Syntax | Description |
|--------|-------------|
| `command &` | Run in background |
| `jobs` | List active jobs |
| `fg [id]` | Bring job to foreground |
| `bg [id]` | Resume job in background |

Jobs are tracked with status: `Foreground`, `Background`, `Suspended`, `Done`.

### Demo

<CodeSnippet name="ShellJobExample" />

---

## Pipeline Extensibility

The pipeline parser supports standard shell operators and can be extended with custom operators.

### Built-in Operators

| Operator | Name | Description |
|----------|------|-------------|
| `\|` | Pipe | Send output to next command |
| `\|;` | Flip | Append output as argument |
| `&&` | AND | Execute next only if previous succeeded |
| `\|\|` | OR | Execute next only if previous failed |
| `;` | Sequence | Execute next unconditionally |
| `>` | Redirect | Write output to file |
| `>>` | Append | Append output to file |
| `&` | Background | Run in background (at end of line) |

### Custom Operators

Register custom operator symbols that map to existing operator types:

```java
PipelineParser parser = new PipelineParser(Map.of(
    "==>", Pipeline.Operator.PIPE,
    "-->", Pipeline.Operator.FLIP
));
Shell.builder().pipelineParser(parser)
```

### Subclassing

For more control, subclass `PipelineParser` and override `matchOperator()`:

```java
PipelineParser parser = new PipelineParser() {
    @Override
    protected String matchOperator(String line, int pos) {
        // Custom matching logic
        if (line.startsWith("::", pos)) {
            return "::";
        }
        return super.matchOperator(line, pos);
    }
};
```

### I/O Redirection

In addition to `>` and `>>`, the pipeline parser supports:

| Operator | Name | Description |
|----------|------|-------------|
| `<` | Input redirect | Read stdin from file |
| `2>` | Stderr redirect | Redirect stderr to file |
| `&>` | Combined redirect | Redirect both stdout and stderr to file |

```
echo hello > out.txt       # write to file
cat < out.txt              # read from file
cmd 2> errors.txt          # capture stderr
cmd &> all.txt             # capture both stdout and stderr
```

### Demo

<CodeSnippet name="ShellPipelineExample" />

---

## Variable Expansion

The `LineExpander` interface provides pluggable variable expansion that runs after alias expansion and before pipeline parsing.

### Setting Up

```java
Shell.builder()
    .lineExpander(new DefaultLineExpander())
    .build();
```

### Built-in Expansions

The `DefaultLineExpander` supports:

| Syntax | Description |
|--------|-------------|
| `$VAR` | Session variable, then environment variable |
| `${VAR}` | Same, with braces for disambiguation |
| `~` | Expands to `user.home` (at word start) |
| `'...'` | Single-quoted regions are not expanded |
| `"..."` | Double-quoted regions expand variables |
| `${VAR:-default}` | Use default if VAR is unset or empty |
| `${VAR:=default}` | Assign default if VAR is unset or empty |
| `${VAR:+alt}` | Use alt if VAR is set and non-empty |
| `${VAR:?error}` | Error if VAR is unset or empty |

```
echo $HOME              → /home/user
echo ${HOME}/docs       → /home/user/docs
echo ~/docs             → /home/user/docs
echo '$HOME'            → $HOME  (no expansion)
echo "$HOME"            → /home/user
echo ${MISSING:-hello}  → hello
echo ${NAME:+yes}       → yes  (if NAME is set)
```

Unknown variables are left as-is (`$UNKNOWN` → `$UNKNOWN`).

### Setting Variables

Variables can be set in several ways:

| Syntax | Description |
|--------|-------------|
| `NAME=VALUE` | Bare assignment (at dispatcher level) |
| `set NAME=VALUE` | Set command |
| `set NAME VALUE` | Set command (space form) |
| `export NAME=VALUE` | Export command (alias for set) |
| `unset NAME` | Remove a variable |
| `set` | List all session variables |

Enable the `set`/`unset`/`export` commands:

```java
Shell.builder().variableCommands(true)
```

Bare `NAME=VALUE` assignment is always available when a `LineExpander` is configured.

### Custom Expansion

Implement `LineExpander` for custom syntax:

```java
LineExpander expander = (line, session) -> {
    // Custom expansion logic
    return line.replace("@user", System.getProperty("user.name"));
};
```

Or subclass `DefaultLineExpander` and override `expandBracedExpression()` or `resolve()`:

```java
new DefaultLineExpander() {
    @Override
    protected String resolve(String name, CommandSession session) {
        // Custom variable resolution, Groovy evaluation, etc.
        return super.resolve(name, session);
    }
};
```

---

## Subcommands

Commands can define subcommands via the `subcommands()` method. The dispatcher routes `git commit -m msg` to the `commit` subcommand with args `[-m, msg]`.

### Defining Subcommands

```java
public class GitCommand extends AbstractCommand {
    private final Map<String, Command> subs = Map.of(
        "commit", new CommitCommand(),
        "status", new StatusCommand()
    );

    public GitCommand() { super("git"); }

    @Override
    public Map<String, Command> subcommands() { return subs; }

    @Override
    public Object execute(CommandSession session, String[] args) {
        session.out().println("usage: git <command>");
        return null;
    }
}
```

Tab completion automatically offers subcommand names after the parent command.

---

## Script Execution

The `ScriptRunner` interface enables executing script files line by line through the dispatcher.

### Setting Up

```java
Shell.builder()
    .scriptRunner(new DefaultScriptRunner())
    .scriptCommands(true)  // registers source/. commands
    .build();
```

### Usage

```
source ~/.myapprc
. ./setup.sh
```

### Script Format

The `DefaultScriptRunner` supports:

- Lines starting with `#` are comments
- Blank lines are skipped
- `\` at end of line joins with the next line (continuation)

```bash
# This is a comment
alias ll=ls -la

echo hello \
  world
```

### Init Scripts

Run a script at shell startup:

```java
Shell.builder()
    .scriptRunner(new DefaultScriptRunner())
    .initScript(new File("~/.myapprc"))
    .build();
```

---

## Signal Handling

When the dispatcher is a `DefaultCommandDispatcher`, the shell automatically registers signal handlers:

| Signal | Behavior |
|--------|----------|
| `INT` (Ctrl-C) | Interrupts the running command, not the shell |
| `TSTP` (Ctrl-Z) | Suspends the foreground job (when job manager is configured) |

Previous signal handlers are restored when the shell exits.

---

## Builtins Integration

The `PosixCommandGroup` wraps the builtins module's POSIX commands as shell commands:

```java
Shell.builder()
    .groups(new PosixCommandGroup())
    .build();
```

Available commands: `cd`, `pwd`, `echo`, `cat`, `ls`, `grep`, `head`, `tail`, `wc`, `sort`, `date`, `sleep`, `clear`.

For interactive commands (`nano`, `less`, `ttop`):

```java
Shell.builder()
    .groups(new InteractiveCommandGroup())
    .build();
```

### Full Example

<CodeSnippet name="ShellBuilderExample" />
