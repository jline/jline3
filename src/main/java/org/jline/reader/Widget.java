package org.jline.reader;

@FunctionalInterface
public interface Widget {

    void apply(ConsoleReaderImpl reader);

}
