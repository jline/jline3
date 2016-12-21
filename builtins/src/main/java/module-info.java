module org.jline.builtins {
    exports org.jline.builtins;
    requires transitive org.jline.reader;
    requires transitive org.jline.terminal;
    requires static com.googlecode.juniversalchardet;
}