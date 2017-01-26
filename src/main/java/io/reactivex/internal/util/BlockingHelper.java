/**
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

package io.reactivex.internal.util;

import java.util.concurrent.CountDownLatch;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.NonBlockingThread;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility methods for helping common blocking operations.
 */
public final class BlockingHelper {
    /** Utility class. */
    private BlockingHelper() {
        throw new IllegalStateException("No instances!");
    }

    public static void awaitForComplete(CountDownLatch latch, Disposable subscription) {
        if (latch.getCount() == 0) {
            // Synchronous observable completes before awaiting for it.
            // Skip await so InterruptedException will never be thrown.
            return;
        }
        // block until the subscription completes and then return
        try {
            verifyNonBlocking();
            latch.await();
        } catch (InterruptedException e) {
            subscription.dispose();
            // set the interrupted flag again so callers can still get it
            // for more information see https://github.com/ReactiveX/RxJava/pull/147#issuecomment-13624780
            Thread.currentThread().interrupt();
            // using Runtime so it is not checked
            throw new IllegalStateException("Interrupted while waiting for subscription to complete.", e);
        }
    }

    /**
     * Checks if the {@code failOnNonBlockingScheduler} plugin setting is enabled and the current
     * thread is a Scheduler sensitive to blocking operators.
     * @throws IllegalStateException if the {@code failOnNonBlockingScheduler} and the current thread is sensitive to blocking
     */
    public static void verifyNonBlocking() {
        if (RxJavaPlugins.isFailOnNonBlockingScheduler()
                && (Thread.currentThread() instanceof NonBlockingThread
                        || RxJavaPlugins.onBeforeBlocking())) {
            throw new IllegalStateException("Attempt to block on a Scheduler " + Thread.currentThread().getName() + " that doesn't support blocking operators as they may lead to deadlock");
        }
    }
}
