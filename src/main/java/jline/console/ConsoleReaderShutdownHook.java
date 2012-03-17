/*
 * Copyright (c) 2012, Scott C. Gray. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>ConsoleReaderShutdownHook</code> is responsible for ensuring
 * that the terminal(s) that are associated with a {@link ConsoleReader}
 * are all reset to their original conditions when the JVM shuts down.
 */
public class ConsoleReaderShutdownHook extends Thread {
    
    private List<ConsoleReader> readers = new ArrayList<ConsoleReader>();
    private boolean shuttingDown = false;
    private static ConsoleReaderShutdownHook instance = null;
    
    /*
     * Don't let anyone but this class create it.
     */
    private ConsoleReaderShutdownHook() {
    }
    
    /**
     * Registers a <code>ConsoleReader</code> with the shutdown hook. When
     * the JVM is exiting the {@link ConsoleReader#close()} method will
     * be automatically called to restore the terminal.
     * 
     * @param reader The reader to register.
     */
    public static synchronized void register(ConsoleReader reader) {
        if (instance == null) {
            instance = new ConsoleReaderShutdownHook();
            Runtime.getRuntime().addShutdownHook(instance);
        }
        instance.readers.add(reader);
    }
    
    /**
     * Unregisters a <code>ConsoleReader</code>.
     * @param reader The reader to unregister.
     */
    public static synchronized void unregister(ConsoleReader reader) {
        if (instance != null) {
            if (instance.shuttingDown == false) {
                instance.readers.remove(reader);
            }
        }
    }
    
    @Override
    public void run() {
        shuttingDown = true;
        for (ConsoleReader reader : readers) {
            reader.close();
        }
    }
}
