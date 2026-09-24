/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins.telnet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the NEW-ENVIRON variable-count limit.
 *
 * <p>CVE-2026-56740 introduced a per-frame {@code varCount} counter capped at
 * {@code NE_VAR_COUNT_MAX} (100). However, the counter reset to 0 on every
 * subnegotiation call ({@code readNEVariables()}), so an attacker could send
 * N frames × 100 vars each and accumulate N×100 entries in the persistent
 * environment map. The follow-up fix adds a persistent-map size check:
 * reject new variable names when {@code env.size() >= NE_VAR_COUNT_MAX} and
 * the name is not already in the map (updates to existing names are allowed).
 *
 * <p>Frame byte format (bytes consumed by {@code readNEVariables()} after the
 * outer IAC–SB–NEWENV–IS header has been consumed by the dispatcher):
 * <pre>
 *   NE_VAR(0)                          ← leading type byte consumed by readNEVariables()
 *   { name-bytes... NE_VALUE(1)        ← readNEVariableName() → NE_VAR_DEFINED
 *     value-bytes... NE_VAR(0) }× N   ← readNEVariableValue() → NE_VAR_OK → put(); NE_VAR consumed
 *   IAC(255) SE(240)                   ← readNEVariableName() sees IAC SE → NE_IN_END → return
 * </pre>
 * Note: the trailing IAC SE serves as a pure terminator for the loop (handled by
 * {@code readNEVariableName()} at the start of the next iteration, not by
 * {@code readNEVariableValue()}). Using NE_VAR to end every value ensures the
 * variable is stored via {@code put()} before the frame ends.
 */
public class TelnetIONewEnvLimitTest {

    // Protocol constants mirrored from TelnetIO
    private static final int IAC = 255;
    private static final int SE = 240;
    private static final int NE_VAR = 0; // type byte: VAR
    private static final int NE_VALUE = 1; // terminates a variable name → NE_VAR_DEFINED
    private static final int NE_VAR_COUNT_MAX = 100; // matches TelnetIO.NE_VAR_COUNT_MAX

    /**
     * Builds the byte stream that {@code readNEVariables()} will consume for a frame
     * containing {@code count} distinct variables named {@code VAR_<startIndex>} …
     * {@code VAR_<startIndex+count-1>}, each with value {@code "v"}.
     *
     * <p>The stream starts with the leading {@code NE_VAR} type byte (consumed first
     * by {@code readNEVariables()}) and ends with {@code IAC SE}.
     */
    private byte[] buildNeFrame(int startIndex, int count) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        // readNEVariables() reads one leading type byte before entering the loop
        buf.write(NE_VAR);
        for (int i = 0; i < count; i++) {
            String name = "VAR_" + (startIndex + i);
            // Name bytes, terminated by NE_VALUE (=1) which causes readNEVariableName()
            // to return NE_VAR_DEFINED
            for (byte b : name.getBytes()) {
                buf.write(b & 0xFF);
            }
            buf.write(NE_VALUE);
            // Value bytes. Each value is terminated by NE_VAR (=0), which causes
            // readNEVariableValue() to return NE_VAR_OK and store the variable via put().
            // The NE_VAR byte is consumed by readNEVariableValue(), so the next call to
            // readNEVariableName() reads subsequent bytes directly.
            buf.write('v');
            buf.write(NE_VAR); // readNEVariableValue → NE_VAR_OK → put(), next name follows
        }
        // Frame terminator: after all variables are stored, readNEVariableName() is
        // called again. It sees IAC followed by SE → returns NE_IN_END → outer loop returns.
        buf.write(IAC);
        buf.write(SE);
        return buf.toByteArray();
    }

    /** Container for a test-ready {@link TelnetIO} wired to a controlled byte stream. */
    private static final class Fixture {
        final TelnetIO io;
        final Object iacHandler;

        @SuppressWarnings("serial")
        Fixture(byte[] inputBytes) throws Exception {
            // Build a minimal ConnectionData using an unconnected Socket subclass so that
            // getInetAddress() returns a real address without actually connecting.
            Socket loopbackSocket = new Socket() {
                @Override
                public InetAddress getInetAddress() {
                    return InetAddress.getLoopbackAddress();
                }

                @Override
                public int getPort() {
                    return 0;
                }

                @Override
                public synchronized void close() throws IOException {
                    // no-op: no real socket to close
                }
            };
            ConnectionData cd = new ConnectionData(loopbackSocket, null);

            // Build TelnetIO and inject connectionData + in via reflection
            TelnetIO telnetIO = new TelnetIO();
            setField(telnetIO, "connectionData", cd);
            setField(telnetIO, "in", new DataInputStream(new ByteArrayInputStream(inputBytes)));
            setField(telnetIO, "out", new DataOutputStream(new ByteArrayOutputStream()));

            // Instantiate the non-static inner IACHandler (requires outer TelnetIO instance)
            Class<?> iacClass = Class.forName("org.jline.builtins.telnet.TelnetIO$IACHandler");
            Constructor<?> ctor = iacClass.getDeclaredConstructor(TelnetIO.class);
            ctor.setAccessible(true);
            Object handler = ctor.newInstance(telnetIO);
            setField(telnetIO, "iacHandler", handler);

            this.io = telnetIO;
            this.iacHandler = handler;
        }

        /** Returns the live environment map from the ConnectionData. */
        java.util.Map<String, String> env() throws Exception {
            Field cdField = TelnetIO.class.getDeclaredField("connectionData");
            cdField.setAccessible(true);
            ConnectionData cd = (ConnectionData) cdField.get(io);
            return cd.getEnvironment();
        }

        /** Calls {@code IACHandler.readNEVariables()} via reflection. */
        void readNEVariables() throws Exception {
            Method m = iacHandler.getClass().getDeclaredMethod("readNEVariables");
            m.setAccessible(true);
            m.invoke(iacHandler);
        }

        /** Replaces the input stream so a second frame can be fed to the same fixture. */
        void feedFrame(byte[] bytes) throws Exception {
            setField(io, "in", new DataInputStream(new ByteArrayInputStream(bytes)));
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * A single frame with exactly {@code NE_VAR_COUNT_MAX} variables must fill
     * the environment map to the limit without overflow.
     */
    @Test
    void singleFrameAtLimitFillsExactly() throws Exception {
        Fixture f = new Fixture(buildNeFrame(0, NE_VAR_COUNT_MAX));
        f.readNEVariables();
        assertEquals(NE_VAR_COUNT_MAX, f.env().size(), "Environment should contain exactly NE_VAR_COUNT_MAX entries");
    }

    /**
     * A single frame with {@code NE_VAR_COUNT_MAX + 1} variables must be truncated:
     * the per-frame {@code varCount} guard fires on the 101st variable.
     */
    @Test
    void singleFrameOverLimitTruncates() throws Exception {
        Fixture f = new Fixture(buildNeFrame(0, NE_VAR_COUNT_MAX + 1));
        f.readNEVariables();
        assertTrue(
                f.env().size() <= NE_VAR_COUNT_MAX,
                "Environment must not exceed NE_VAR_COUNT_MAX after a single oversized frame, got: "
                        + f.env().size());
    }

    /**
     * Multi-frame bypass regression test (the bypass of CVE-2026-56740):
     * two consecutive frames each contributing {@code NE_VAR_COUNT_MAX} distinct
     * variables must not accumulate beyond the limit.
     *
     * <p>Without the persistent-map size check, the per-frame {@code varCount} reset
     * to 0 on each call so frame 2 could add another 100 entries to the persistent map.
     */
    @Test
    void multiFrameBypassIsBlocked() throws Exception {
        // Frame 1: fill the map to the limit
        Fixture f = new Fixture(buildNeFrame(0, NE_VAR_COUNT_MAX));
        f.readNEVariables();
        assertEquals(NE_VAR_COUNT_MAX, f.env().size(), "Frame 1 should fill map to the limit");

        // Frame 2: attempt to add another NE_VAR_COUNT_MAX *new*, distinct variables
        f.feedFrame(buildNeFrame(NE_VAR_COUNT_MAX, NE_VAR_COUNT_MAX));
        f.readNEVariables();

        assertEquals(
                NE_VAR_COUNT_MAX,
                f.env().size(),
                "Environment must not grow beyond NE_VAR_COUNT_MAX across multiple frames");
    }

    /**
     * Updating an existing variable when the map is at capacity must succeed.
     *
     * <p>The refined guard allows {@code put()} for names already present in the map,
     * so that a later subnegotiation can correct a previously-sent value.
     */
    @Test
    void updateExistingVarAtLimitSucceeds() throws Exception {
        // Fill to limit using VAR_0 … VAR_99
        Fixture f = new Fixture(buildNeFrame(0, NE_VAR_COUNT_MAX));
        f.readNEVariables();
        assertEquals(NE_VAR_COUNT_MAX, f.env().size());
        assertEquals("v", f.env().get("VAR_0"), "Initial value should be 'v'");

        // Build a minimal frame re-sending VAR_0 with value "updated"
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(NE_VAR); // leading type byte
        for (byte b : "VAR_0".getBytes()) buf.write(b & 0xFF);
        buf.write(NE_VALUE); // terminates name → NE_VAR_DEFINED
        for (byte b : "updated".getBytes()) buf.write(b & 0xFF);
        buf.write(NE_VAR); // readNEVariableValue → NE_VAR_OK → put()
        buf.write(IAC);
        buf.write(SE); // readNEVariableName → NE_IN_END

        f.feedFrame(buf.toByteArray());
        f.readNEVariables();

        assertEquals(NE_VAR_COUNT_MAX, f.env().size(), "Map size must remain at limit after update");
        assertEquals("updated", f.env().get("VAR_0"), "Existing variable must be updated even at the size limit");
    }
}
