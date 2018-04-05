/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

/**
 * An extension of {@link ParsedLine} that, being aware of the quoting and escaping rules
 * of the {@link org.jline.reader.Parser} that produced it, knows if and how a completion candidate
 * should be escaped/quoted.
 *
 * @author Eric Bottard
 */
@FunctionalInterface
public interface CompletingParsedLine {

    CharSequence emit(CharSequence candidate);

}
