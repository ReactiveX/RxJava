/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.schedulers;

import java.security.*;

/**
 * Holds onto a flag that enables the capture of the current stacktrace when
 * a Scheduler.Worker is created.
 * 
 * Use the system property {@code rx.scheduler-worker.debug} ({@code true|false})
 * to initialize its value, or use the setEnabled() method during runtime.
 */
public enum WorkerDebugSupport {
    ;
    
    private static volatile boolean enabled;
    
    static {
        String s = System.getProperty("rx.scheduler-worker.debug", "false");
        enabled = "true".equals(s);
    }
    /**
     * Returns the current state of the worker debug mode.
     * @return the current state of the worker debug mode
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Enables or disables the worker debug mode.
     * @param value the new state
     */
    public static void setEnabled(final boolean value) {
        SecurityManager smgr = System.getSecurityManager();
        if (smgr == null) {
            enabled = value;
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    enabled = value;
                    return null;
                }
            });
        }
    }
}
