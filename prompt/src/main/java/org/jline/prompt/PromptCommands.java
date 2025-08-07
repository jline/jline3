/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jline.builtins.Options;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

/**
 * Prompt command implementations for JLine applications.
 * <p>
 * This class provides command-line interfaces to the JLine prompt system,
 * allowing prompts to be used from scripts and REPL consoles.
 * </p>
 * <p>
 * Available commands include:
 * </p>
 * <ul>
 *   <li>prompt - interactive prompts (list, checkbox, choice, input, confirm)</li>
 * </ul>
 */
public class PromptCommands {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private PromptCommands() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Context for command execution, providing I/O streams and current directory.
     */
    public static class Context {
        private final InputStream in;
        private final PrintStream out;
        private final PrintStream err;
        private final Path currentDir;
        private final Terminal terminal;
        private final Function<String, Object> variables;

        public Context(
                InputStream in,
                PrintStream out,
                PrintStream err,
                Path currentDir,
                Terminal terminal,
                Function<String, Object> variables) {
            this.in = in;
            this.out = out;
            this.err = err;
            this.currentDir = currentDir;
            this.terminal = terminal;
            this.variables = variables;
        }

        public InputStream in() {
            return in;
        }

        public PrintStream out() {
            return out;
        }

        public PrintStream err() {
            return err;
        }

        public Path currentDir() {
            return currentDir;
        }

        public Terminal terminal() {
            return terminal;
        }

        public Function<String, Object> variables() {
            return variables;
        }
    }

    /**
     * Parse command options using the Options parser.
     */
    private static Options parseOptions(Context context, String[] usage, String[] argv) throws Exception {
        try {
            Options opt = Options.compile(usage).parse(argv);
            if (opt.isSet("help")) {
                throw new Options.HelpException(opt.usage());
            }
            return opt;
        } catch (Options.HelpException e) {
            context.out().println(e.getMessage());
            return null;
        } catch (Exception e) {
            context.err().println("Error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Prompt command - create interactive prompts.
     *
     * @param context the execution context
     * @param argv command arguments
     * @throws Exception if the command fails
     */
    public static void prompt(Context context, String[] argv) throws Exception {
        final String[] usage = {
            "prompt - create interactive prompts",
            "Usage: prompt [OPTIONS] TYPE [ITEMS...]",
            "  -? --help                show help",
            "  -m --message=MESSAGE     prompt message",
            "  -t --title=TITLE         prompt title/header",
            "  -d --default=VALUE       default value",
            "  -k --key=KEYS            choice keys (for choice type)",
            "",
            "Types:",
            "  list                     single selection list",
            "  checkbox                 multiple selection checkboxes",
            "  choice                   single character choice",
            "  input                    text input",
            "  confirm                  yes/no confirmation",
            "",
            "Examples:",
            "  prompt list \"Choose option\" \"Option 1\" \"Option 2\" \"Option 3\"",
            "  prompt checkbox \"Select items\" \"Item A\" \"Item B\" \"Item C\"",
            "  prompt choice \"Pick color\" \"Red\" \"Green\" \"Blue\" -k \"rgb\"",
            "  prompt input \"Enter name\" -d \"John\"",
            "  prompt confirm \"Continue?\" -d \"y\""
        };

        Options opt = parseOptions(context, usage, argv);
        if (opt == null) return;

        List<String> args = opt.args();
        if (args.isEmpty()) {
            context.err().println("Error: prompt type required");
            return;
        }

        String type = args.get(0);
        String message = opt.isSet("message") ? opt.get("message") : "";
        String title = opt.isSet("title") ? opt.get("title") : "";
        String defaultValue = opt.isSet("default") ? opt.get("default") : "";
        String keys = opt.isSet("key") ? opt.get("key") : "";

        List<String> items = args.subList(1, args.size());

        try {
            Prompter prompter = PrompterFactory.create(context.terminal());
            List<AttributedString> header =
                    title.isEmpty() ? new ArrayList<>() : Arrays.asList(new AttributedString(title));

            switch (type.toLowerCase()) {
                case "list":
                    handleListPrompt(context, prompter, header, message, items);
                    break;
                case "checkbox":
                    handleCheckboxPrompt(context, prompter, header, message, items);
                    break;
                case "choice":
                    handleChoicePrompt(context, prompter, header, message, items, keys);
                    break;
                case "input":
                    handleInputPrompt(context, prompter, header, message, defaultValue);
                    break;
                case "confirm":
                    handleConfirmPrompt(context, prompter, header, message, defaultValue);
                    break;
                default:
                    context.err().println("Error: unknown prompt type '" + type + "'");
                    context.err().println("Valid types: list, checkbox, choice, input, confirm");
                    return;
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("interrupted")) {
                context.err().println("Prompt cancelled");
            } else {
                context.err().println("Error: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Object array version for compatibility.
     */
    public static void prompt(Context context, Object[] argv) throws Exception {
        String[] stringArgv = new String[argv.length];
        for (int i = 0; i < argv.length; i++) {
            stringArgv[i] = argv[i] != null ? argv[i].toString() : "";
        }
        prompt(context, stringArgv);
    }

    private static void handleListPrompt(
            Context context, Prompter prompter, List<AttributedString> header, String message, List<String> items)
            throws IOException {
        if (items.isEmpty()) {
            context.err().println("Error: list prompt requires at least one item");
            return;
        }

        PromptBuilder builder = prompter.newBuilder();
        ListBuilder listBuilder = builder.createListPrompt().name("list").message(message);

        for (int i = 0; i < items.size(); i++) {
            listBuilder.newItem("item" + i).text(items.get(i)).add();
        }

        listBuilder.addPrompt();

        Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

        ListResult result = (ListResult) results.get("list");
        if (result != null) {
            context.out().println(result.getSelectedId());
        }
    }

    private static void handleCheckboxPrompt(
            Context context, Prompter prompter, List<AttributedString> header, String message, List<String> items)
            throws IOException {
        if (items.isEmpty()) {
            context.err().println("Error: checkbox prompt requires at least one item");
            return;
        }

        PromptBuilder builder = prompter.newBuilder();
        CheckboxBuilder checkboxBuilder =
                builder.createCheckboxPrompt().name("checkbox").message(message);

        for (int i = 0; i < items.size(); i++) {
            checkboxBuilder.newItem("item" + i).text(items.get(i)).add();
        }

        checkboxBuilder.addPrompt();

        Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

        CheckboxResult result = (CheckboxResult) results.get("checkbox");
        if (result != null) {
            for (String selectedId : result.getSelectedIds()) {
                context.out().println(selectedId);
            }
        }
    }

    private static void handleChoicePrompt(
            Context context,
            Prompter prompter,
            List<AttributedString> header,
            String message,
            List<String> items,
            String keys)
            throws IOException {
        if (items.isEmpty()) {
            context.err().println("Error: choice prompt requires at least one item");
            return;
        }

        PromptBuilder builder = prompter.newBuilder();
        ChoiceBuilder choiceBuilder =
                builder.createChoicePrompt().name("choice").message(message);

        for (int i = 0; i < items.size(); i++) {
            Character key = null;
            if (keys != null && i < keys.length()) {
                key = keys.charAt(i);
            }
            boolean isDefault = i == 0; // First item is default

            if (key != null) {
                choiceBuilder
                        .newChoice("item" + i)
                        .text(items.get(i))
                        .key(key)
                        .defaultChoice(isDefault)
                        .add();
            } else {
                choiceBuilder
                        .newChoice("item" + i)
                        .text(items.get(i))
                        .defaultChoice(isDefault)
                        .add();
            }
        }

        choiceBuilder.addPrompt();

        Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

        ChoiceResult result = (ChoiceResult) results.get("choice");
        if (result != null) {
            context.out().println(result.getSelectedId());
        }
    }

    private static void handleInputPrompt(
            Context context, Prompter prompter, List<AttributedString> header, String message, String defaultValue)
            throws IOException {
        PromptBuilder builder = prompter.newBuilder();
        InputBuilder inputBuilder = builder.createInputPrompt().name("input").message(message);

        if (defaultValue != null && !defaultValue.isEmpty()) {
            inputBuilder.defaultValue(defaultValue);
        }

        inputBuilder.addPrompt();

        Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

        InputResult result = (InputResult) results.get("input");
        if (result != null) {
            context.out().println(result.getInput());
        }
    }

    private static void handleConfirmPrompt(
            Context context, Prompter prompter, List<AttributedString> header, String message, String defaultValue)
            throws IOException {
        boolean defaultBool = defaultValue.toLowerCase().startsWith("y") || defaultValue.equals("1");

        PromptBuilder builder = prompter.newBuilder();
        builder.createConfirmPrompt()
                .name("confirm")
                .message(message)
                .defaultValue(defaultBool)
                .addPrompt();

        Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(header, builder.build());

        ConfirmResult result = (ConfirmResult) results.get("confirm");
        if (result != null) {
            context.out().println(result.isConfirmed() ? "true" : "false");
        }
    }
}
