/*
 * Copyright (c) 2002-2018, the original author or authors.
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
 * Class implementing a ConnectionEvent.<br>
 * These events are used to communicate things that are
 * supposed to be handled within the application context.
 * These events are processed by the Connection instance
 * calling upon its registered listeners.
 *
 * @author Dieter Wimberger
 * @version 2.0 (16/07/2006)
 * @see Connection
 * @see ConnectionListener
 */
public class ConnectionEvent {

    private final Connection source;
    private final Type type;
    /**
     * Constructs a new instance of a ConnectionEvent
     * with a given source (Connection) and a given type.
     *
     * @param source Connection that represents the source of this event.
     * @param type int that contains one of the defined event types.
     */
    public ConnectionEvent(Connection source, Type type) {
        this.type = type;
        this.source = source;
    }//constructor

    /**
     * Accessor method returning the source of the
     * ConnectionEvent instance.
     *
     * @return Connection representing the source.
     */
    public Connection getSource() {
        return source;
    }//getSource

    /**
     * Method that helps identifying the type.
     *
     * @return Event type.
     */
    public Type getType() {
        return type;
    }//getType

    public enum Type {
        /**
         * Defines the connection idle event type.<br>
         * It occurs if a connection has been idle exceeding
         * the configured time to warning.
         */
        CONNECTION_IDLE,

        /**
         * Defines the connection timed out event type.<br>
         * It occurs if a connection has been idle exceeding
         * the configured time to warning and the configured time
         * to timedout.
         */
        CONNECTION_TIMEDOUT,

        /**
         * Defines the connection requested logout event type.
         * <p>
         * It occurs if a connection requested disgraceful logout by
         * sending a <code>&lt;Ctrl&gt;-&lt;D&gt;</code> key combination.
         */
        CONNECTION_LOGOUTREQUEST,

        /**
         * Defines the connection sent break event type.<br>
         * It occurs when the connection sent a NVT BREAK.
         */
        CONNECTION_BREAK,

        /**
         * Defines the connection geometry event type.
         * It occurs when the connection sent a NAWS.
         */
        CONNECTION_TERMINAL_GEOMETRY_CHANGED;
    }


}//class ConnectionEvent