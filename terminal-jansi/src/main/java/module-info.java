module org.jline.terminal.jansi {
    exports org.jline.terminal.impl.jansi;
    requires transitive org.jline.terminal;
    requires org.fusesource.jansi;
}