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
package rx.observers;

import java.util.concurrent.atomic.AtomicReference;

import rx.CompletableSubscriber;
import rx.Subscription;
import rx.annotations.Experimental;
import rx.plugins.RxJavaHooks;

/**
 * An abstract base class for CompletableSubscriber implementations that want to expose an unsubscription
 * capability.
 * <p>
 * Calling {@link #unsubscribe()} and {@link #isUnsubscribed()} is thread-safe and can happen at any time, even
 * before or during an active {@link rx.Completable#subscribe(CompletableSubscriber)} call.
 * <p>
 * Override the {@link #onStart()} method to execute custom logic on the very first successful onSubscribe call.
 * <p>
 * If one wants to remain consistent regarding {@link #isUnsubscribed()} and being terminated,
 * the {@link #clear()} method should be called from the implementing onError and onCompleted methods.
 * <p>
 * <pre><code>
 * public final class MyCompletableSubscriber extends AsyncCompletableSubscriber {
 *     &#64;Override
 *     public void onStart() {
 *         System.out.println("Started!");
 *     }
 *
 *     &#64;Override
 *     public void onCompleted() {
 *         System.out.println("Completed!");
 *         clear();
 *     }
 *
 *     &#64;Override
 *     public void onError(Throwable e) {
 *         e.printStackTrace();
 *         clear();
 *     }
 * }
 * </code></pre>
 * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
 */
@Experimental
public abstract class AsyncCompletableSubscriber implements CompletableSubscriber, Subscription {
    /**
     * Indicates the unsubscribed state.
     */
    static final Unsubscribed UNSUBSCRIBED = new Unsubscribed();

    /**
     * Holds onto a deferred subscription and allows asynchronous cancellation before the call
     * to onSubscribe() by the upstream.
     */
    private final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

    @Override
    public final void onSubscribe(Subscription d) {
        if (!upstream.compareAndSet(null, d)) {
            d.unsubscribe();
            if (upstream.get() != UNSUBSCRIBED) {
                RxJavaHooks.onError(new IllegalStateException("Subscription already set!"));
            }
        } else {
            onStart();
        }
    }

    /**
     * Called before the first onSubscribe() call succeeds.
     */
    protected void onStart() {
        // default behavior is no op
    }

    @Override
    public final boolean isUnsubscribed() {
        return upstream.get() == UNSUBSCRIBED;
    }

    /**
     * Call to clear the upstream's subscription without unsubscribing it.
     */
    protected final void clear() {
        upstream.set(UNSUBSCRIBED);
    }

    @Override
    public final void unsubscribe() {
        Subscription current = upstream.get();
        if (current != UNSUBSCRIBED) {
            current = upstream.getAndSet(UNSUBSCRIBED);
            if (current != null && current != UNSUBSCRIBED) {
                current.unsubscribe();
            }
        }

    }

    static final class Unsubscribed implements Subscription {

        @Override
        public void unsubscribe() {
            // deliberately no op
        }

        @Override
        public boolean isUnsubscribed() {
            return true;
        }

    }
}
