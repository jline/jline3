/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package jline;

/**
 * Jline logger.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public class Log
{
    ///CLOVER:OFF

    @SuppressWarnings({ "StringConcatenation" })
    public static final boolean DEBUG = Boolean.getBoolean(Log.class.getName() + ".debug");

    @SuppressWarnings({ "StringConcatenation" })
    public static final boolean TRACE = Boolean.getBoolean(Log.class.getName() + ".trace");

    public static void trace(final Object... messages) {
        if (TRACE) {
            synchronized (System.err) {
                System.err.print("[TRACE] ");

                for (Object message : messages) {
                    System.err.print(message);
                }

                System.err.println();
                System.err.flush();
            }
        }
    }

    public static void debug(final Object... messages) {
        if (TRACE || DEBUG) {
            synchronized (System.err) {
                System.err.print("[DEBUG] ");

                for (Object message : messages) {
                    System.err.print(message);
                }

                System.err.println();
                System.err.flush();
            }
        }
    }

    public static void error(final Object... messages) {
        synchronized (System.err) {
            System.err.print("[ERROR] ");

            for (Object message : messages) {
                System.err.print(message);
                if (message instanceof Throwable) {
                    ((Throwable)message).printStackTrace();
                }
            }

            System.err.println();
            System.err.flush();
        }
    }
}