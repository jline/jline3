package org.jline.console;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fusesource.jansi.Pty;
import org.fusesource.jansi.Pty.Attributes;
import org.jline.Console;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.Log;

public abstract class AbstractConsole implements Console {

    private final String type;
    private Set<Capability> bools = new HashSet<Capability>();
    private Map<Capability, Integer> ints = new HashMap<Capability, Integer>();
    private Map<Capability, String> strings = new HashMap<Capability, String>();

    public AbstractConsole(String type) {
        this.type = type;
    }

    public boolean echo() throws IOException {
        return getAttributes().getLocalFlag(Pty.ECHO);
    }

    public boolean echo(boolean echo) throws IOException {
        Attributes attr = getAttributes();
        boolean prev = attr.getLocalFlag(Pty.ECHO);
        if (prev != echo) {
            attr.setLocalFlag(Pty.ECHO, echo);
            setAttributes(attr);
        }
        return prev;
    }

    public String getType() {
        return type;
    }

    public boolean puts(Capability capability, Object... params) throws IOException {
        String str = getStringCapability(capability);
        if (str == null) {
            return false;
        }
        Curses.tputs(writer(), str, params);
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

}
