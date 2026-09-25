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
import java.util.function.Predicate;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.*;

/**
 * Converts between JLine {@link Attributes} and platform-native {@link TermiosData}.
 *
 * <p>Each platform subclass provides EnumMap tables that map JLine flag/control-char enums
 * to their native bitmask or c_cc index values. The base class iterates those tables in
 * {@link #toTermios(Attributes, TermiosData)} and {@link #toAttributes(TermiosData)}, so subclasses
 * are pure data declarations with no conversion logic.</p>
 *
 * @see AixTermiosMapping
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
     * <p>Unmapped bits such as baud-rate flags and {@code c_ispeed}/{@code c_ospeed} are zero
     * in the returned structure. Prefer {@link #toTermios(Attributes, TermiosData)} when applying
     * attributes onto an existing terminal so those fields are preserved.</p>
     *
     * @param attr the JLine attributes
     * @return the corresponding native termios data
     */
    public final TermiosData toTermios(Attributes attr) {
        return toTermios(attr, null);
    }

    /**
     * Converts JLine {@link Attributes} onto an existing native termios snapshot.
     *
     * <p>Mapped flags and control characters are replaced from {@code attr}. Unmapped bits
     * (including baud-rate flags such as {@code CBAUD}/{@code CBAUDEX}), input/output speeds,
     * and unmapped {@code c_cc} entries from {@code existing} are preserved. When
     * {@code existing} is {@code null}, this is equivalent to {@link #toTermios(Attributes)}.</p>
     *
     * @param attr the JLine attributes
     * @param existing the current native termios data, or {@code null}
     * @return native termios data with JLine flags applied
     */
    public final TermiosData toTermios(Attributes attr, TermiosData existing) {
        TermiosData tio = new TermiosData();
        if (existing != null) {
            tio.iflag(existing.iflag());
            tio.oflag(existing.oflag());
            tio.cflag(existing.cflag());
            tio.lflag(existing.lflag());
            tio.ispeed(existing.ispeed());
            tio.ospeed(existing.ospeed());
            System.arraycopy(existing.cc(), 0, tio.cc(), 0, tio.cc().length);
        }
        tio.iflag(applyMappedFlags(inputFlagMap, attr::getInputFlag, tio.iflag()));
        tio.oflag(applyMappedFlags(outputFlagMap, attr::getOutputFlag, tio.oflag()));
        tio.cflag(applyMappedFlags(controlFlagMap, attr::getControlFlag, tio.cflag()));
        tio.lflag(applyMappedFlags(localFlagMap, attr::getLocalFlag, tio.lflag()));
        for (var e : controlCharMap.entrySet()) {
            tio.cc()[e.getValue()] = (byte) attr.getControlChar(e.getKey());
        }
        return tio;
    }

    private static <E extends Enum<E>> long applyMappedFlags(
            EnumMap<E, Long> map, Predicate<E> enabled, long existing) {
        long mapped = 0;
        long value = 0;
        for (var e : map.entrySet()) {
            mapped |= e.getValue();
            if (enabled.test(e.getKey())) {
                value |= e.getValue();
            }
        }
        return (existing & ~mapped) | value;
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
            } else if (osName.contains("AIX")) {
                return AixTermiosMapping.INSTANCE;
            }
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }
    }
}
