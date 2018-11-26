/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/*
 * Java TelnetD library (embeddable telnet daemon)
 * Copyright (c) 2000-2005 Dieter Wimberger
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ***/

package org.jline.builtins.telnet;


/**
 * Interface to be implemented if a class wants to
 * qualify as a ConnectionListener.<br>
 * Note that a Shell is per contract also forced to
 * implement this interface.
 *
 * @author Dieter Wimberger
 * @version 2.0 (16/07/2006)
 * @see ConnectionEvent
 */
public interface ConnectionListener {

    /**
     * Called when a CONNECTION_IDLE event occured.
     *
     * @param ce ConnectionEvent instance.
     * @see ConnectionEvent.Type#CONNECTION_IDLE
     */
    default void connectionIdle(ConnectionEvent ce) {
    }

    /**
     * Called when a CONNECTION_TIMEDOUT event occured.
     *
     * @param ce ConnectionEvent instance.
     * @see ConnectionEvent.Type#CONNECTION_TIMEDOUT
     */
    default void connectionTimedOut(ConnectionEvent ce) {
    }

    /**
     * Called when a CONNECTION_LOGOUTREQUEST occured.
     *
     * @param ce ConnectionEvent instance.
     * @see ConnectionEvent.Type#CONNECTION_LOGOUTREQUEST
     */
    default void connectionLogoutRequest(ConnectionEvent ce) {
    }

    /**
     * Called when a CONNECTION_BREAK event occured.
     *
     * @param ce ConnectionEvent instance.
     * @see ConnectionEvent.Type#CONNECTION_BREAK
     */
    default void connectionSentBreak(ConnectionEvent ce) {
    }

    /**
     * Called when a CONNECTION_TERMINAL_GEOMETRY_CHANGED event occured.
     *
     * @param ce ConnectionEvent instance.
     * @see ConnectionEvent.Type#CONNECTION_TERMINAL_GEOMETRY_CHANGED
     */
    default void connectionTerminalGeometryChanged(ConnectionEvent ce) {
    }

}//interface ConnectionListener