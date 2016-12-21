module org.jline.terminal.jna {
    exports org.jline.terminal.impl.jna;
    requires transitive org.jline.terminal;
    requires net.java.dev.jna;
}