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

import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.utils.AttributedString;

import org.jline.builtins.Options;
import org.jline.builtins.Options.HelpException;
import org.jline.console.CommandRegistry.CommandSession;

public abstract class AbstractCommandRegistry {
    
    public AbstractCommandRegistry() {}

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
        throw new IllegalArgumentException("default CommandRegistry.commandInfo() method must be overridden in class "
                                          + this.getClass().getCanonicalName());
    }

    public Set<String> commandNames() {
        return new HashSet<>();
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
        throw new IllegalArgumentException("default CommandRegistry.commandDescription() method must be overridden in class "
                                          + this.getClass().getCanonicalName());
    }

    public Object execute(CommandSession session, String command, String[] args) throws Exception {
        throw new IllegalArgumentException("CommandRegistry method execute(String command, String[] args) is not implemented!");
    }

    public Object invoke(CommandSession session, String command, Object... args) throws Exception {
        String[] _args = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof String)) {
                throw new IllegalArgumentException();
            }
            _args[i] = args[i].toString();
        }
        return execute(session, command, _args);
    }

    public Options parseOptions(String[] usage, Object[] args) throws HelpException {
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        return opt;
    }

}
