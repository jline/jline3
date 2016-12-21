module org.jline {
    exports org.jline.builtins;
    exports org.jline.keymap;
    exports org.jline.reader;
    exports org.jline.reader.impl;
    exports org.jline.reader.impl.completer;
    exports org.jline.reader.impl.history;
    exports org.jline.terminal;
    exports org.jline.terminal.impl;
    exports org.jline.terminal.spi;
    exports org.jline.utils;
    requires java.logging;
    requires static org.fusesource.jansi;
    requires static net.java.dev.jna;
    requires static com.googlecode.juniversalchardet;
}