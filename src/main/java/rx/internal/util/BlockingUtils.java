/**
 * Copyright 2015 Netflix, Inc.
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

package rx.internal.util;

import rx.Subscription;
import rx.annotations.Experimental;

import java.util.concurrent.CountDownLatch;

/**
 * Utility functions relating to blocking types.
 * <p/>
 * Not intended to be part of the public API.
 */
@Experimental
public final class BlockingUtils {

    private BlockingUtils() { }

    /**
     * Blocks and waits for a {@link Subscription} to complete.
     *
     * @param latch        a CountDownLatch
     * @param subscription the Subscription to wait on.
     */
    @Experimental
    public static void awaitForComplete(CountDownLatch latch, Subscription subscription) {
        if (latch.getCount() == 0) {
            // Synchronous observable completes before awaiting for it.
            // Skip await so InterruptedException will never be thrown.
            return;
        }
        // block until the subscription completes and then return
        try {
            latch.await();
        } catch (InterruptedException e) {
            subscription.unsubscribe();
            // set the interrupted flag again so callers can still get it
            // for more information see https://github.com/ReactiveX/RxJava/pull/147#issuecomment-13624780
            Thread.currentThread().interrupt();
            // using Runtime so it is not checked
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }
    }
}
