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

package jline.internal;

import static jline.internal.Preconditions.checkNotNull;

/**
 * Manages the JLine shutdown-hook.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.7
 */
public class ShutdownHook
{
    public static final String JLINE_SHUTDOWNHOOK = "jline.shutdownhook";

    private static boolean enabled = Configuration.getBoolean(JLINE_SHUTDOWNHOOK, true);

    private static Thread hook;

    public static void install(final Thread thread) {
        checkNotNull(thread);

        if (!enabled) {
            Log.debug("Shutdown-hook is disabled; not installing: ", thread);
            return;
        }

        if (hook != null) {
            throw new IllegalStateException("Shutdown-hook already installed");
        }

        try {
            Runtime.getRuntime().addShutdownHook(thread);
            hook = thread;
        }
        catch (AbstractMethodError e) {
            // JDK 1.3+ only method. Bummer.
            Log.trace("Failed to register shutdown-hook: ", e);
        }
    }

    public static void remove() {
        if (!enabled) {
            return;
        }

        if (hook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            }
            catch (AbstractMethodError e) {
                // JDK 1.3+ only method. Bummer.
                Log.trace("Failed to remove shutdown-hook: ", e);
            }
            catch (IllegalStateException e) {
                // The VM is shutting down, not a big deal; ignore
            }
            hook = null;
        }
    }
}