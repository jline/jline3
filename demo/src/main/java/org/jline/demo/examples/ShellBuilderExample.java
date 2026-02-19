/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.jline.builtins.PosixCommandGroup;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.shell.Command;
import org.jline.shell.CommandSession;
import org.jline.shell.Shell;
import org.jline.shell.impl.*;

/**
 * Example demonstrating how Shell.builder() creates a feature-rich shell.
 * <p>
 * This example showcases:
 * <ul>
 *   <li>POSIX builtins (ls, cd, pwd, cat, echo, grep, head, tail, wc, sort, date, sleep, clear)</li>
 *   <li>Variable expansion ($VAR, ${VAR}, ~)</li>
 *   <li>Subcommands (git commit, git status)</li>
 *   <li>Script execution (source command)</li>
 *   <li>I/O redirection (&lt;, 2&gt;, &amp;&gt;)</li>
 *   <li>Job control, aliases, syntax highlighting, help</li>
 * </ul>
 * <p>
 * Try these commands:
 * <pre>
 *   echo hello world
 *   echo $HOME
 *   echo ~/docs
 *   FOO=bar
 *   echo $FOO
 *   set NAME=world
 *   echo ${NAME:-default}
 *   unset NAME
 *   echo ${NAME:-default}
 *   ls
 *   pwd
 *   cd /tmp
 *   echo hello &gt; /tmp/test.txt
 *   cat &lt; /tmp/test.txt
 *   git status
 *   git commit -m "my message"
 *   sleep 5 &amp;
 *   jobs
 *   help
 * </pre>
 */
public class ShellBuilderExample {

    // SNIPPET_START: ShellBuilderExample
    /**
     * A command with subcommands, demonstrating the subcommand routing feature.
     */
    static class GitCommand extends AbstractCommand {
        private final Map<String, Command> subcommands = Map.of(
                "commit",
                new AbstractCommand("commit") {
                    @Override
                    public String description() {
                        return "Record changes";
                    }

                    @Override
                    public Object execute(CommandSession session, String[] args) {
                        session.out().println("committed: " + String.join(" ", args));
                        return null;
                    }
                },
                "status",
                new AbstractCommand("status") {
                    @Override
                    public String description() {
                        return "Show working tree status";
                    }

                    @Override
                    public Object execute(CommandSession session, String[] args) {
                        Path wd = session.workingDirectory();
                        session.out().println("On branch main");
                        session.out().println("Working directory: " + wd);
                        session.out().println("nothing to commit, working tree clean");
                        return null;
                    }
                });

        GitCommand() {
            super("git");
        }

        @Override
        public String description() {
            return "Version control (subcommands: commit, status)";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            session.out().println("usage: git <command> [<args>]");
            session.out().println("  commit    Record changes");
            session.out().println("  status    Show working tree status");
            return null;
        }

        @Override
        public Map<String, Command> subcommands() {
            return subcommands;
        }
    }

    public static void main(String[] args) {
        try (Shell shell = Shell.builder()
                .prompt("demo> ")
                // POSIX builtins: ls, cd, pwd, cat, echo, grep, etc.
                .groups(new PosixCommandGroup()) // HIGHLIGHT
                // Custom commands with subcommand support
                .groups(new SimpleCommandGroup("demo", new GitCommand())) // HIGHLIGHT
                // Variable expansion: $VAR, ${VAR}, ~
                .lineExpander(new DefaultLineExpander()) // HIGHLIGHT
                // Script execution: source/. commands
                .scriptRunner(new DefaultScriptRunner()) // HIGHLIGHT
                .scriptCommands(true) // HIGHLIGHT
                // Variable commands: set, unset, export + bare VAR=VALUE
                .variableCommands(true) // HIGHLIGHT
                // Job control, aliases, built-in commands
                .jobManager(new DefaultJobManager())
                .aliasManager(new DefaultAliasManager())
                .historyCommands(true)
                .helpCommands(true)
                .optionCommands(true)
                .commandHighlighter(true)
                .variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".demo_history"))
                .option(Option.INSERT_BRACKET, true)
                .option(Option.DISABLE_EVENT_EXPANSION, true)
                .build()) {
            shell.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: ShellBuilderExample
}
