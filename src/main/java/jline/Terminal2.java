/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline;

/**
 * Terminal extension.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.13
 */
public interface Terminal2 extends Terminal
{
    String getStringCapability(String capability);

    int getNumericCapability(String capability);

    boolean getBooleanCapability(String capability);

}
