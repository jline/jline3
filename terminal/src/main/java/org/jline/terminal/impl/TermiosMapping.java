/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.util.EnumMap;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.*;

/**
 * Converts between JLine {@link Attributes} and platform-native {@link TermiosData}.
 *
 * <p>Each platform subclass provides EnumMap tables that map JLine flag/control-char enums
 * to their native bitmask or c_cc index values. The base class iterates those tables in
 * {@link #toTermios(Attributes)} and {@link #toAttributes(TermiosData)}, so subclasses
 * are pure data declarations with no conversion logic.</p>
 *
 * @see LinuxTermiosMapping
 * @see OsXTermiosMapping
 * @see FreeBsdTermiosMapping
 * @see SolarisTermiosMapping
 */
@SuppressWarnings("java:S6548")
public abstract class TermiosMapping {

    private final EnumMap<InputFlag, Long> inputFlagMap;
    private final EnumMap<OutputFlag, Long> outputFlagMap;
    private final EnumMap<ControlFlag, Long> controlFlagMap;
    private final EnumMap<LocalFlag, Long> localFlagMap;
    private final EnumMap<ControlChar, Integer> controlCharMap;

    protected TermiosMapping(
            EnumMap<InputFlag, Long> inputFlagMap,
            EnumMap<OutputFlag, Long> outputFlagMap,
            EnumMap<ControlFlag, Long> controlFlagMap,
            EnumMap<LocalFlag, Long> localFlagMap,
            EnumMap<ControlChar, Integer> controlCharMap) {
        this.inputFlagMap = inputFlagMap;
        this.outputFlagMap = outputFlagMap;
        this.controlFlagMap = controlFlagMap;
        this.localFlagMap = localFlagMap;
        this.controlCharMap = controlCharMap;
    }

    /**
     * Converts native termios data to JLine {@link Attributes}.
     *
     * @param tio the native termios data
     * @return the corresponding JLine attributes
     */
    public final Attributes toAttributes(TermiosData tio) {
        Attributes attr = new Attributes();
        for (var e : inputFlagMap.entrySet()) {
            if ((tio.iflag() & e.getValue()) != 0) {
                attr.setInputFlag(e.getKey(), true);
            }
        }
        for (var e : outputFlagMap.entrySet()) {
            if ((tio.oflag() & e.getValue()) != 0) {
                attr.setOutputFlag(e.getKey(), true);
            }
        }
        for (var e : controlFlagMap.entrySet()) {
            if ((tio.cflag() & e.getValue()) != 0) {
                attr.setControlFlag(e.getKey(), true);
            }
        }
        for (var e : localFlagMap.entrySet()) {
            if ((tio.lflag() & e.getValue()) != 0) {
                attr.setLocalFlag(e.getKey(), true);
            }
        }
        for (var e : controlCharMap.entrySet()) {
            attr.setControlChar(e.getKey(), tio.cc()[e.getValue()]);
        }
        return attr;
    }

    /**
     * Converts JLine {@link Attributes} to native termios data.
     *
     * @param attr the JLine attributes
     * @return the corresponding native termios data
     */
    public final TermiosData toTermios(Attributes attr) {
        TermiosData tio = new TermiosData();
        for (var e : inputFlagMap.entrySet()) {
            if (attr.getInputFlag(e.getKey())) {
                tio.iflag(tio.iflag() | e.getValue());
            }
        }
        for (var e : outputFlagMap.entrySet()) {
            if (attr.getOutputFlag(e.getKey())) {
                tio.oflag(tio.oflag() | e.getValue());
            }
        }
        for (var e : controlFlagMap.entrySet()) {
            if (attr.getControlFlag(e.getKey())) {
                tio.cflag(tio.cflag() | e.getValue());
            }
        }
        for (var e : localFlagMap.entrySet()) {
            if (attr.getLocalFlag(e.getKey())) {
                tio.lflag(tio.lflag() | e.getValue());
            }
        }
        for (var e : controlCharMap.entrySet()) {
            tio.cc()[e.getValue()] = (byte) attr.getControlChar(e.getKey());
        }
        return tio;
    }

    /**
     * Returns the {@link TermiosMapping} for the current operating system.
     *
     * <p>The result is cached after the first call via a lazy initialization holder,
     * so this method is safe to call from any context without performance concerns.</p>
     *
     * @return the platform-specific mapping
     * @throws UnsupportedOperationException if the OS is not recognized
     */
    public static TermiosMapping forCurrentPlatform() {
        return PlatformMappingHolder.INSTANCE;
    }

    private static class PlatformMappingHolder {
        static final TermiosMapping INSTANCE = detectPlatform();

        private static TermiosMapping detectPlatform() {
            String osName = System.getProperty("os.name");
            if (osName == null) {
                throw new UnsupportedOperationException("Unable to determine OS: os.name system property is null");
            }
            if (osName.startsWith("Linux")) {
                return LinuxTermiosMapping.INSTANCE;
            } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
                return OsXTermiosMapping.INSTANCE;
            } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
                return SolarisTermiosMapping.INSTANCE;
            } else if (osName.startsWith("FreeBSD")) {
                return FreeBsdTermiosMapping.INSTANCE;
            }
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }
    }
}
