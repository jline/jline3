package org.jline.reader;

@FunctionalInterface
public interface Widget {

    boolean apply(ConsoleReaderImpl reader);

}
