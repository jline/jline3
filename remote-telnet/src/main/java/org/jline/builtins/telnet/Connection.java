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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that implements a connection with this telnet daemon.
 * <p>
 * It is derived from java.lang.Thread, which reflects the architecture
 * constraint of one thread per connection. This might seem a waste of
 * resources, but as a matter of fact sharing threads would require a
 * far more complex imlementation, due to the fact that telnet is not a
 * stateless protocol (i.e. alive throughout a session of multiple requests
 * and responses).
 * <p>
 * Each Connection instance is created by the listeners ConnectionManager
 * instance, making it part of a threadgroup and passing in an associated
 * ConnectionData instance, that holds vital information about the connection.
 * Be sure to take a look at their documention.
 * <p>
 * Once the thread has started and is running, it will get a login
 * shell instance from the ShellManager and run passing its own reference.
 *
 * @author Dieter Wimberger
 * @version 2.0 (16/07/2006)
 * @see ConnectionManager
 * @see ConnectionData
 */
public abstract class Connection
        extends Thread {

    private static final Logger LOG = Logger.getLogger(Connection.class.getName());
    private static int number;            //unique number for a thread in the thread group
    private boolean dead;
    private List<ConnectionListener> listeners;

    //Associations
    private ConnectionData connectionData;    //associated information

    /**
     * Constructs a TelnetConnection by invoking its parent constructor
     * and setting of various members.<br>
     * Subsequently instantiates the whole i/o subsystem, negotiating
     * telnet protocol level options etc.<br>
     *
     * @param tcg ThreadGroup that this instance is running in.
     * @param cd  ConnectionData instance containing all vital information
     *            of this connection.
     * @see ConnectionData
     */
    public Connection(ThreadGroup tcg, ConnectionData cd) {
        super(tcg, ("Connection" + (++number)));

        connectionData = cd;
        //init the connection listeners for events
        //(there should actually be only one or two)
        listeners = new CopyOnWriteArrayList<ConnectionListener>();
        dead = false;
    }//constructor

    /**
     * Method overloaded to implement following behaviour:
     * <ol>
     * <li> On first entry, retrieve an instance of the configured
     * login shell from the ShellManager and run it.
     * <li> Handle a shell switch or close down disgracefully when
     * problems (i.e. unhandled unchecked exceptions) occur in the
     * running shell.
     * </ol>
     */
    public void run() {
        try {
            doRun();

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "run()", ex); //Handle properly
        } finally {
            //call close if not dead already
            if (!dead) {
                close();
            }
        }
        LOG.log(Level.FINE, "run():: Returning from " + this.toString());
    }//run

    protected abstract void doRun() throws Exception;

    protected abstract void doClose() throws Exception;

    /**
     * Method to access the associated connection data.
     *
     * @return ConnectionData associated with the Connection instance.
     * @see ConnectionData
     */
    public ConnectionData getConnectionData() {
        return connectionData;
    }//getConnectionData

    /**
     * Closes the connection and its underlying i/o and network
     * resources.<br>
     */
    public synchronized void close() {
        if (dead) {
            return;
        } else {
            try {
                //connection dead
                dead = true;
                //close i/o
                doClose();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "close()", ex);
                //handle
            }
            try {
                //close socket
                connectionData.getSocket().close();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "close()", ex);
                //handle
            }
            try {
                //register closed connection in ConnectionManager
                connectionData.getManager().registerClosedConnection(this);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "close()", ex);
                //handle
            }
            try {
                //try to interrupt it
                interrupt();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "close()", ex);
                //handle
            }


            LOG.log(Level.FINE, "Closed " + this.toString() + " and inactive.");
        }
    }//close

    /**
     * Returns if a connection has been closed.<br>
     *
     * @return the state of the connection.
     */
    public boolean isActive() {
        return !dead;
    }//isClosed

    /****** Event handling ****************/

    /**
     * Method that registers a ConnectionListener with the
     * Connection instance.
     *
     * @param cl ConnectionListener to be registered.
     * @see ConnectionListener
     */
    public void addConnectionListener(ConnectionListener cl) {
        listeners.add(cl);
    }//addConnectionListener

    /**
     * Method that removes a ConnectionListener from the
     * Connection instance.
     *
     * @param cl ConnectionListener to be removed.
     * @see ConnectionListener
     */
    public void removeConnectionListener(ConnectionListener cl) {
        listeners.remove(cl);
    }//removeConnectionListener


    /**
     * Method called by the io subsystem to pass on a
     * "low-level" event. It will be properly delegated to
     * all registered listeners.
     *
     * @param ce ConnectionEvent to be processed.
     * @see ConnectionEvent
     */
    public void processConnectionEvent(ConnectionEvent ce) {
        for (ConnectionListener cl : listeners) {
            switch (ce.getType()) {
                case CONNECTION_IDLE:
                    cl.connectionIdle(ce);
                    break;
                case CONNECTION_TIMEDOUT:
                    cl.connectionTimedOut(ce);
                    break;
                case CONNECTION_LOGOUTREQUEST:
                    cl.connectionLogoutRequest(ce);
                    break;
                case CONNECTION_BREAK:
                    cl.connectionSentBreak(ce);
                    break;
                case CONNECTION_TERMINAL_GEOMETRY_CHANGED:
                    cl.connectionTerminalGeometryChanged(ce);
            }
        }
    }//processConnectionEvent

}//class Connection
