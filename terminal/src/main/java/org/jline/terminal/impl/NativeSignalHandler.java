/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;

public final class NativeSignalHandler implements SignalHandler {

    public static final NativeSignalHandler SIG_DFL = new NativeSignalHandler();

    public static final NativeSignalHandler SIG_IGN = new NativeSignalHandler();

    private NativeSignalHandler() {
    }

    public void handle(Signal signal) {
        throw new UnsupportedOperationException();
    }
}
