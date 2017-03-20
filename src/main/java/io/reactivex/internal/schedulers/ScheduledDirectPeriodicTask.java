/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.schedulers;

import io.reactivex.plugins.RxJavaPlugins;

/**
 * A Callable to be submitted to an ExecutorService that runs a Runnable
 * action periodically and manages completion/cancellation.
 * @since 2.0.8
 */
public final class ScheduledDirectPeriodicTask extends AbstractDirectTask implements Runnable {

    private static final long serialVersionUID = 1811839108042568751L;

    public ScheduledDirectPeriodicTask(Runnable runnable) {
        super(runnable);
    }

    @Override
    public void run() {
        runner = Thread.currentThread();
        try {
            try {
                runnable.run();
            } catch (Throwable ex) {
                lazySet(FINISHED);
                RxJavaPlugins.onError(ex);
            }
        } finally {
            runner = null;
        }
    }
}
