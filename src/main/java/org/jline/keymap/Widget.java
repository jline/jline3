/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.keymap;

/**
 *
 * @param <T>
 */
@FunctionalInterface
public interface Widget<T> extends Binding {

    boolean apply(T reader);

}
