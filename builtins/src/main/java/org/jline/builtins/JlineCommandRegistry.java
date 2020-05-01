/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.utils.AttributedString;

import org.jline.builtins.Options;
import org.jline.builtins.Completers.AnyCompleter;
import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.Options.HelpException;
import org.jline.console.AbstractCommandRegistry;
import org.jline.console.CommandRegistry.CommandSession;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.SystemCompleter;

/**
 * CommandRegistry common methods for JLine commands that are using HelpException.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public abstract class JlineCommandRegistry extends AbstractCommandRegistry {

    public JlineCommandRegistry() {
        super();
    }

    public List<String> commandInfo(String command) {
        try {
            Object[] args = {"--help"};
            if (command.equals("help")) {
                args = new Object[] {};
            }
            invoke(new CommandSession(), command, args);
        } catch (HelpException e) {
            return compileCommandInfo(e.getMessage());
        } catch (Exception e) {

        }
        throw new IllegalArgumentException("JlineCommandRegistry.commandInfo() method must be overridden in class "
                                          + this.getClass().getCanonicalName());
    }

    public CmdDesc commandDescription(String command) {
        try {
            invoke(new CommandSession(), command, new Object[] {"--help"});
        } catch (HelpException e) {
            return compileCommandDescription(e.getMessage());
        } catch (Exception e) {

        }
        throw new IllegalArgumentException("JlineCommandRegistry.commandDescription() method must be overridden in class "
                                          + this.getClass().getCanonicalName());
    }

    public List<OptDesc> commandOptions(String command) {
        try {
            invoke(new CommandSession(), command, "--help");
        } catch (HelpException e) {
            return compileCommandOptions(e.getMessage());
        } catch (Exception e) {

        }
        return null;
    }

    public List<Completer> defaultCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                           , new OptionCompleter(NullCompleter.INSTANCE
                                                               , this::commandOptions
                                                               , 1)
                                            ));
        return completers;
    }

    public Options parseOptions(String[] usage, Object[] args) throws HelpException {
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        return opt;
    }

    //
    // Utils for helpMessage parsing
    //
    private static AttributedString highlightComment(String comment) {
        return HelpException.highlightComment(comment, HelpException.defaultStyle());
    }

    private static String[] helpLines(String helpMessage, boolean body) {
        return new HelpLines(helpMessage, body).lines();
    }

    private static class HelpLines {
        private String helpMessage;
        private boolean body;
        private boolean subcommands;

        public HelpLines(String helpMessage, boolean body) {
            this.helpMessage = helpMessage;
            this.body = body;
        }

        public String[] lines() {
            String out = "";
            Matcher tm = Pattern.compile("(^|\\n)(Usage|Summary)(:)").matcher(helpMessage);
            if (tm.find()) {
                subcommands = tm.group(2).matches("Summary");
                if (body) {
                    out = helpMessage.substring(tm.end(3));
                } else {
                    out = helpMessage.substring(0,tm.start(1));
                }
            } else if (!body) {
                out = helpMessage;
            }
            return out.split("\\r?\\n");
        }

        public boolean subcommands() {
            return subcommands;
        }
    }

    protected static CmdDesc compileCommandDescription(String helpMessage) {
        List<AttributedString> main = new ArrayList<>();
        Map<String, List<AttributedString>> options = new HashMap<>();
        String prevOpt = null;
        boolean mainDone = false;
        HelpLines hl = new HelpLines(helpMessage, true);
        for (String s : hl.lines()) {
            if (s.matches("^\\s+-.*$")) {
                mainDone = true;
                int ind = s.lastIndexOf("  ");
                if (ind > 0) {
                    String o = s.substring(0, ind);
                    String d = s.substring(ind);
                    if (o.trim().length() > 0) {
                        prevOpt = o.trim();
                        options.put(prevOpt, new ArrayList<>(Arrays.asList(highlightComment(d.trim()))));
                    }
                }
            } else if (s.matches("^[\\s]{20}.*$") && prevOpt != null && options.containsKey(prevOpt)) {
                int ind = s.lastIndexOf("  ");
                if (ind > 0) {
                    options.get(prevOpt).add(highlightComment(s.substring(ind).trim()));
                }
            } else {
                prevOpt = null;
            }
            if (!mainDone) {
                main.add(HelpException.highlightSyntax(s.trim(), HelpException.defaultStyle(), hl.subcommands()));
            }
        }
        return new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("")), options);
    }

    protected static List<OptDesc> compileCommandOptions(String helpMessage) {
        List<OptDesc> out = new ArrayList<>();
        for (String s : helpLines(helpMessage, true)) {
            if (s.matches("^\\s+-.*$")) {
                int ind = s.lastIndexOf("  ");
                if (ind > 0) {
                    String[] op = s.substring(0, ind).trim().split("\\s+");
                    String d = s.substring(ind).trim();
                    String so = null;
                    String lo = null;
                    if (op.length == 1) {
                        if (op[0].startsWith("--")) {
                            lo = op[0];
                        } else {
                            so = op[0];
                        }
                    } else {
                        so = op[0];
                        lo = op[1];
                    }
                    boolean hasValue = false;
                    if (lo != null && lo.contains("=")) {
                        hasValue = true;
                        lo = lo.split("=")[0];
                    }
                    out.add(new OptDesc(so, lo, d, hasValue ? AnyCompleter.INSTANCE : null));
                }
            }
        }
        return out;
    }

    protected static List<String> compileCommandInfo(String helpMessage) {
        List<String> out = new ArrayList<>();
        boolean first = true;
        for (String s  : helpLines(helpMessage, false)) {
            if (first && s.contains(" - ")) {
                out.add(s.substring(s.indexOf(" - ") + 3).trim());
            } else {
                out.add(s.trim());
            }
            first = false;
        }
        return out;
    }

}
