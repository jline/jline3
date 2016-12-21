module org.jline.reader {
    exports org.jline.keymap;
    exports org.jline.reader;
    exports org.jline.reader.impl;
    exports org.jline.reader.impl.completer;
    exports org.jline.reader.impl.history;
    requires transitive org.jline.terminal;
}