/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.schedulers;

import java.util.concurrent.*;

import rx.functions.Func0;
import rx.internal.util.RxThreadFactory;
import rx.plugins.RxJavaHooks;

/**
 * Utility class to create the individual ScheduledExecutorService instances for
 * the GenericScheduledExecutorService class.
 */
enum GenericScheduledExecutorServiceFactory {
    ;

    static final String THREAD_NAME_PREFIX = "RxScheduledExecutorPool-";
    static final RxThreadFactory THREAD_FACTORY = new RxThreadFactory(THREAD_NAME_PREFIX);

    static ThreadFactory threadFactory() {
        return THREAD_FACTORY;
    }

    /**
     * Creates a ScheduledExecutorService (either the default or given by a hook).
     * @return the ScheduledExecutorService created.
     */
    public static ScheduledExecutorService create() {
        Func0<? extends ScheduledExecutorService> f = RxJavaHooks.getOnGenericScheduledExecutorService();
        if (f == null) {
            return createDefault();
        }
        return f.call();
    }


    static ScheduledExecutorService createDefault() {
        return Executors.newScheduledThreadPool(1, threadFactory());
    }
}
