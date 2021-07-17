/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.schedulers;

import java.util.concurrent.*;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;

/**
 * Manages the creating of ScheduledExecutorServices and sets up purging.
 */
public final class SchedulerPoolFactory {
    /** Utility class. */
    private SchedulerPoolFactory() {
        throw new IllegalStateException("No instances!");
    }

    static final String PURGE_ENABLED_KEY = "rx3.purge-enabled";

    public static final boolean PURGE_ENABLED;

    static {
        SystemPropertyAccessor propertyAccessor = new SystemPropertyAccessor();
        PURGE_ENABLED = getBooleanProperty(true, PURGE_ENABLED_KEY, true, true, propertyAccessor);
    }

    static boolean getBooleanProperty(boolean enabled, String key, boolean defaultNotFound, boolean defaultNotEnabled, Function<String, String> propertyAccessor) {
        if (enabled) {
            try {
                String value = propertyAccessor.apply(key);
                if (value == null) {
                    return defaultNotFound;
                }
                return "true".equals(value);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                return defaultNotFound;
            }
        }
        return defaultNotEnabled;
    }

    static final class SystemPropertyAccessor implements Function<String, String> {
        @Override
        public String apply(String t) {
            return System.getProperty(t);
        }
    }

    /**
     * Creates a ScheduledExecutorService with the given factory.
     * @param factory the thread factory
     * @return the ScheduledExecutorService
     */
    public static ScheduledExecutorService create(ThreadFactory factory) {
        final ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, factory);
        exec.setRemoveOnCancelPolicy(PURGE_ENABLED);
        return exec;
    }
}
