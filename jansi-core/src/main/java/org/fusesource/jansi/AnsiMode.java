/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.fusesource.jansi;

/**
 * Ansi mode.
 *
 * @since 2.1
 * @deprecated Use {@link org.jline.jansi.AnsiMode} instead.
 */
@Deprecated
public enum AnsiMode {
    Strip("Strip all ansi sequences"),
    Default("Print ansi sequences if the stream is a terminal"),
    Force("Always print ansi sequences, even if the stream is redirected");

    private final String description;

    AnsiMode(String description) {
        this.description = description;
    }

    String getDescription() {
        return description;
    }
}
