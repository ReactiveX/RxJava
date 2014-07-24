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
 * Default implementation of {@link RxJavaScheduledRunnableWrapper}.  Just passes through the {@link Runnable}.
 *
 * @ExcludeFromJavadoc
 */
/*package-private*/ class RxJavaDefaultScheduledRunnableWrapper extends RxJavaScheduledRunnableWrapper {

    private static RxJavaDefaultScheduledRunnableWrapper INSTANCE = new RxJavaDefaultScheduledRunnableWrapper();

    private RxJavaDefaultScheduledRunnableWrapper() {

    }

    public static RxJavaDefaultScheduledRunnableWrapper getInstance() {
        return INSTANCE;
    }

    @Override
    public Runnable getRunnable(final Runnable runnable) {
        return runnable;
    }
}
