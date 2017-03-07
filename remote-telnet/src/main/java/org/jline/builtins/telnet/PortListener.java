/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that implements a <tt>PortListener</tt>.<br>
 * If available, it accepts incoming connections and passes them
 * to an associated <tt>ConnectionManager</tt>.
 *
 * @author Dieter Wimberger
 * @version 2.0 (16/07/2006)
 * @see ConnectionManager
 */
public class PortListener
        implements Runnable {

    private static final Logger LOG = Logger.getLogger(PortListener.class.getName());
    private static final String logmsg =
            "Listening to Port {0,number,integer} with a connectivity queue size of {1,number,integer}.";
    private String name;
    private int port;                                        //port number running on
    private int floodProtection;                        //flooding protection
    private ServerSocket serverSocket = null; //server socket
    private Thread thread;
    private ConnectionManager connectionManager;    //connection management thread
    private boolean stopping = false;
    private boolean available;                    //Flag for availability

    /**
     * Constructs a PortListener instance.<br>
     *
     * @param port      int that specifies the port number of the server socket.
     * @param floodprot that specifies the server socket queue size.
     */
    public PortListener(String name, int port, int floodprot) {
        this.name = name;
        available = false;
        this.port = port;
        floodProtection = floodprot;
    }//constructor

    /**
     * Returns the name of this <tt>PortListener</tt>.
     *
     * @return the name as <tt>String</tt>.
     */
    public String getName() {
        return name;
    }//getName

    /**
     * Tests if this <tt>PortListener</tt> is available.
     *
     * @return true if available, false otherwise.
     */
    public boolean isAvailable() {
        return available;
    }//isAvailable

    /**
     * Sets the availability flag of this <tt>PortListener</tt>.
     *
     * @param b true if to be available, false otherwise.
     */
    public void setAvailable(boolean b) {
        available = b;
    }//setAvailable

    /**
     * Starts this <tt>PortListener</tt>.
     */
    public void start() {
        LOG.log(Level.FINE, "start()");
        thread = new Thread(this);
        thread.start();
        available = true;
    }//start

    /**
     * Stops this <tt>PortListener</tt>, and returns
     * when everything was stopped successfully.
     */
    public void stop() {
        LOG.log(Level.FINE, "stop()::" + this.toString());
        //flag stop
        stopping = true;
        available = false;
        //take down all connections
        connectionManager.stop();

        //close server socket
        try {
            serverSocket.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "stop()", ex);
        }

        //wait for thread to die
        try {
            thread.join();
        } catch (InterruptedException iex) {
            LOG.log(Level.SEVERE, "stop()", iex);
        }

        LOG.info("stop()::Stopped " + this.toString());
    }//stop

    /**
     * Listen constantly to a server socket and handles incoming connections
     * through the associated {a:link ConnectionManager}.
     *
     * @see ConnectionManager
     */
    public void run() {
        try {
            /*
                A server socket is opened with a connectivity queue of a size specified
                in int floodProtection.  Concurrent login handling under normal circumstances
                should be handled properly, but denial of service attacks via massive parallel
                program logins should be prevented with this.
            */
            serverSocket = new ServerSocket(port, floodProtection);

            //log entry
            LOG.info(MessageFormat.format(logmsg, port, floodProtection));

            do {
                try {
                    Socket s = serverSocket.accept();
                    if (available) {
                        connectionManager.makeConnection(s);
                    } else {
                        //just shut down the socket
                        s.close();
                    }
                } catch (SocketException ex) {
                    if (stopping) {
                        //server socket was closed blocked in accept
                        LOG.log(Level.FINE, "run(): ServerSocket closed by stop()");
                    } else {
                        LOG.log(Level.SEVERE, "run()", ex);
                    }
                }
            } while (!stopping);

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "run()", e);
        }
        LOG.log(Level.FINE, "run(): returning.");
    }//run

    /**
     * Returns reference to ConnectionManager instance associated
     * with the PortListener.
     *
     * @return the associated ConnectionManager.
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }//getConnectionManager

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

}//class PortListener
