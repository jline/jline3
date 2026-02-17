/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Picocli integration module.
 * <p>
 * Provides a {@link org.jline.picocli.PicocliCommandRegistry} that bridges
 * picocli {@link picocli.CommandLine} commands into the JLine
 * {@link org.jline.console.CommandRegistry} framework.
 */
module org.jline.picocli {
    requires transitive org.jline.console;
    requires transitive org.jline.shell;
    requires transitive info.picocli;

    exports org.jline.picocli;

    opens org.jline.picocli to
            info.picocli;
}
