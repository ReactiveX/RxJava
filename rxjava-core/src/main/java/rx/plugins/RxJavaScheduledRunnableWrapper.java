/**
 * Copyright 2014 Netflix, Inc.
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
package rx.plugins;

/**
 * This is a hook for allowing wrapping to take place before a {@link Runnable} is submitted
 * to an {@link java.util.concurrent.ExecutorService}.
 *
 * This is an alternative to replacing all {@link rx.Scheduler}s wholesale, as in {@link RxJavaDefaultSchedulers}
 */
public abstract class RxJavaScheduledRunnableWrapper {

    /**
     * Runnable to submit to {@link java.util.concurrent.ExecutorService}
     * @param runnable original {@link Runnable}
     * @return wrapped {@link Runnable} - this could be a passthrough of the initial, or decorated to perform
     * extra logic
     */
    public abstract Runnable getRunnable(Runnable runnable);
}
