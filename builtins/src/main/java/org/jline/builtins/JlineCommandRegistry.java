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
import java.util.stream.Collectors;

import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.utils.AttributedString;

import org.jline.builtins.Options;
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
            return Builtins.compileCommandInfo(e.getMessage());
        } catch (Exception e) {

        }
        throw new IllegalArgumentException("JlineCommandRegistry.commandInfo() method must be overridden in class "
                                          + this.getClass().getCanonicalName());
    }

    public CmdDesc commandDescription(String command) {
        try {
            if (command != null && !command.isEmpty()) {
                invoke(new CommandSession(), command, new Object[] {"--help"});
            } else {
                List<AttributedString> main = new ArrayList<>();
                Map<String, List<AttributedString>> options = new HashMap<>();
                for (String c : new TreeSet<String>(commandNames())) {
                    for (String info : commandInfo(c)) {
                        main.add(HelpException.highlightSyntax(c + " -  " + info, HelpException.defaultStyle(), true));
                        break;
                    }
                }
                return new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("")), options);
            }
        } catch (HelpException e) {
            return Builtins.compileCommandDescription(e.getMessage());
        } catch (Exception e) {

        }
        throw new IllegalArgumentException("JlineCommandRegistry.commandDescription() method must be overridden in class "
                                          + this.getClass().getCanonicalName());
    }
    
    public List<OptDesc> commandOptions(String command) {
        try {
            invoke(new CommandSession(), command, "--help");
        } catch (HelpException e) {
            return Builtins.compileCommandOptions(e.getMessage());
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
    
}
