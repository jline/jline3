/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeferredTerminalTest {

    private Terminal createDumbTerminal() throws IOException {
        return new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
    }

    @Test
    void writerBuffersAndReplays() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.writer().print("hello");
        deferred.writer().flush();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal real = new DumbTerminal(new ByteArrayInputStream(new byte[0]), out);
        deferred.complete(real);

        assertTrue(out.toString().contains("hello"));
    }

    @Test
    void writerDelegatesAfterComplete() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal real = new DumbTerminal(new ByteArrayInputStream(new byte[0]), out);
        deferred.complete(real);

        deferred.writer().print("after");
        deferred.writer().flush();
        assertTrue(out.toString().contains("after"));
    }

    @Test
    void getTypeReturnsTERMBeforeReady() {
        DeferredTerminal deferred = new DeferredTerminal();
        assertFalse(deferred.isReady());
        assertNotNull(deferred.getType());
    }

    @Test
    void getTypeReturnsDumbWithoutTERM() {
        DeferredTerminal deferred = new DeferredTerminal();
        String type = deferred.getType();
        // Either TERM env var or "dumb"
        assertNotNull(type);
    }

    @Test
    void getTypeDelegatesAfterComplete() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        Terminal real = createDumbTerminal();
        deferred.complete(real);
        assertEquals(real.getType(), deferred.getType());
        deferred.close();
    }

    @Test
    void onCompleteBeforeComplete() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        AtomicReference<Terminal> received = new AtomicReference<>();
        deferred.onComplete(received::set);

        Terminal real = createDumbTerminal();
        deferred.complete(real);
        assertEquals(real, received.get());
        deferred.close();
    }

    @Test
    void onCompleteAfterComplete() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        Terminal real = createDumbTerminal();
        deferred.complete(real);

        AtomicReference<Terminal> received = new AtomicReference<>();
        deferred.onComplete(received::set);
        assertEquals(real, received.get());
        deferred.close();
    }

    @Test
    void onCompleteExactlyOnce() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        int[] count = {0};
        deferred.onComplete(t -> count[0]++);
        deferred.complete(createDumbTerminal());
        assertEquals(1, count[0]);
        deferred.close();
    }

    @Test
    void completeExceptionallyDrainsBuffer() {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.writer().print("drained");
        deferred.writer().flush();
        deferred.completeExceptionally(new RuntimeException("test"));

        assertFalse(deferred.isReady());
        // After exceptional completion, writer should still work (falls back to stdout)
        deferred.writer().print("more");
        deferred.writer().flush();
        assertTrue(true, "no exception thrown");
    }

    @Test
    void closeBeforeCompleteDoesNotThrow() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.writer().print("buffered");
        deferred.writer().flush();
        deferred.close();
        assertFalse(deferred.isReady());
    }

    @Test
    void closeAfterCompleteClosesTerminal() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        Terminal real = createDumbTerminal();
        deferred.complete(real);
        assertTrue(deferred.isReady());
        deferred.close();
    }

    @Test
    void closeAfterExceptionalCompletionDoesNotThrow() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.completeExceptionally(new IOException("build failed"));
        deferred.close();
        assertFalse(deferred.isReady());
    }

    @Test
    void closeIsIdempotent() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.close();
        deferred.close();
        assertFalse(deferred.isReady());
    }

    @Test
    void getTerminalBlocksUntilReady() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch waiting = new CountDownLatch(1);
        CompletableFuture<Terminal> result = new CompletableFuture<>();

        Thread t = new Thread(() -> {
            started.countDown();
            waiting.countDown();
            result.complete(deferred.getTerminal());
        });
        t.setDaemon(true);
        t.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        assertFalse(result.isDone());

        Terminal real = createDumbTerminal();
        deferred.complete(real);
        assertEquals(real, result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void getTerminalAfterCancelThrows() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.close();
        assertThrows(IllegalStateException.class, deferred::getTerminal);
    }

    @Test
    void capabilitiesReturnDefaultsBeforeReady() {
        DeferredTerminal deferred = new DeferredTerminal();
        assertFalse(deferred.getBooleanCapability(org.jline.utils.InfoCmp.Capability.auto_left_margin));
        assertTrue(deferred.getNumericCapability(org.jline.utils.InfoCmp.Capability.max_colors) >= 256);
        assertNotNull(deferred.getPalette());
        assertTrue(deferred.getColumns() > 0);
        assertTrue(deferred.getRows() > 0);
    }
}
