/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.IOError;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jline.Console;
import org.jline.ConsoleReader;
import org.jline.JLine.ConsoleReaderBuilder;
import org.jline.console.Attributes.ControlChar;
import org.jline.console.Attributes.InputFlag;
import org.jline.console.Attributes.LocalFlag;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Log;

import static org.jline.utils.Preconditions.checkNotNull;

public abstract class AbstractConsole implements Console {

    protected final String name;
    protected final String type;
    protected final ConsoleReaderBuilder consoleReaderBuilder;
    protected final Map<Signal, SignalHandler> handlers = new HashMap<>();
    protected final Set<Capability> bools = new HashSet<>();
    protected final Map<Capability, Integer> ints = new HashMap<>();
    protected final Map<Capability, String> strings = new HashMap<>();

    public AbstractConsole(String name, String type, ConsoleReaderBuilder consoleReaderBuilder) throws IOException {
        this.name = name;
        this.type = type;
        this.consoleReaderBuilder = consoleReaderBuilder;
        for (Signal signal : Signal.values()) {
            handlers.put(signal, SignalHandler.SIG_DFL);
        }
    }

    @Override
    public ConsoleReaderBuilder getConsoleReaderBuilder() {
        return consoleReaderBuilder;
    }

    public ConsoleReader newConsoleReader() {
        return consoleReaderBuilder.console(this).build();
    }

    public SignalHandler handle(Signal signal, SignalHandler handler) {
        checkNotNull(signal);
        checkNotNull(handler);
        return handlers.put(signal, handler);
    }

    public void raise(Signal signal) {
        checkNotNull(signal);
        SignalHandler handler = handlers.get(signal);
        if (handler == SignalHandler.SIG_DFL) {
            handleDefaultSignal(signal);
        } else if (handler != SignalHandler.SIG_IGN) {
            handler.handle(signal);
        }
    }

    protected void handleDefaultSignal(Signal signal) {
    }

    protected void echoSignal(Signal signal) {
        ControlChar cc = null;
        switch (signal) {
            case INT:
                cc = ControlChar.VINTR;
                break;
            case QUIT:
                cc = ControlChar.VQUIT;
                break;
            case TSTP:
                cc = ControlChar.VSUSP;
                break;
        }
        if (cc != null) {
            int vcc = getAttributes().getControlChar(cc);
            if (vcc > 0 && vcc < 32) {
                writer().write(new char[]{'^', (char) (vcc + '@')}, 0, 2);
            }
        }
    }

    public Attributes enterRawMode() {
        Attributes prvAttr = getAttributes();
        Attributes newAttr = new Attributes(prvAttr);
        newAttr.setLocalFlags(EnumSet.of(LocalFlag.ICANON, LocalFlag.ECHO, LocalFlag.IEXTEN), false);
        newAttr.setInputFlags(EnumSet.of(InputFlag.IXON, InputFlag.ICRNL, InputFlag.INLCR), false);
        newAttr.setControlChar(ControlChar.VMIN, 1);
        newAttr.setControlChar(ControlChar.VTIME, 0);
        setAttributes(newAttr);
        return prvAttr;
    }

    public boolean echo() {
        return getAttributes().getLocalFlag(LocalFlag.ECHO);
    }

    public boolean echo(boolean echo) {
        Attributes attr = getAttributes();
        boolean prev = attr.getLocalFlag(LocalFlag.ECHO);
        if (prev != echo) {
            attr.setLocalFlag(LocalFlag.ECHO, echo);
            setAttributes(attr);
        }
        return prev;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void flush() {
        writer().flush();
    }

    public boolean puts(Capability capability, Object... params) {
        String str = getStringCapability(capability);
        if (str == null) {
            return false;
        }
        try {
            Curses.tputs(writer(), str, params);
        } catch (IOException e) {
            throw new IOError(e);
        }
        return true;
    }

    public boolean getBooleanCapability(Capability capability) {
        return bools.contains(capability);
    }

    public Integer getNumericCapability(Capability capability) {
        return ints.get(capability);
    }

    public String getStringCapability(Capability capability) {
        return strings.get(capability);
    }

    protected void parseInfoCmp() {
        String capabilities = null;
        if (type != null) {
            try {
                capabilities = InfoCmp.getInfoCmp(type);
            } catch (Exception e) {
                Log.warn("Unable to retrieve infocmp for type " + type, e);
            }
        }
        if (capabilities == null) {
            capabilities = InfoCmp.ANSI_CAPS;
        }
        InfoCmp.parseInfoCmp(capabilities, bools, ints, strings);
    }

    @Override
    public String readLine() throws UserInterruptException, EndOfFileException {
        return newConsoleReader().readLine();
    }

    @Override
    public String readLine(Character mask) throws UserInterruptException, EndOfFileException {
        return newConsoleReader().readLine(mask);
    }

    @Override
    public String readLine(String prompt) throws UserInterruptException, EndOfFileException {
        return newConsoleReader().readLine(prompt);
    }

    @Override
    public String readLine(String prompt, Character mask) throws UserInterruptException, EndOfFileException {
        return newConsoleReader().readLine(prompt, mask);
    }

    @Override
    public String readLine(String prompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException {
        return newConsoleReader().readLine(prompt, mask, buffer);
    }

    @Override
    public String readLine(String prompt, String rightPrompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException {
        return newConsoleReader().readLine(prompt, rightPrompt, mask, buffer);
    }
}
