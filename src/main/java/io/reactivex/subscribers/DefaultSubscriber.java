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

package io.reactivex.subscribers;

import org.reactivestreams.Subscription;

import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.EndConsumerHelper;

/**
 * Abstract base implementation of a {@link org.reactivestreams.Subscriber Subscriber} with
 * support for requesting via {@link #request(long)}, cancelling via
 * via {@link #cancel()} (both synchronously) and calls {@link #onStart()}
 * when the subscription happens.
 *
 * <p>All pre-implemented final methods are thread-safe.
 *
 * <p>The default {@link #onStart()} requests Long.MAX_VALUE by default. Override
 * the method to request a custom <em>positive</em> amount.
 *
 * <p>Note that calling {@link #request(long)} from {@link #onStart()} may trigger
 * an immediate, asynchronous emission of data to {@link #onNext(Object)}. Make sure
 * all initialization happens before the call to {@code request()} in {@code onStart()}.
 * Calling {@link #request(long)} inside {@link #onNext(Object)} can happen at any time
 * because by design, {@code onNext} calls from upstream are non-reentrant and non-overlapping.
 *
 * <p>Use the protected {@link #cancel()} to cancel the sequence from within an
 * {@code onNext} implementation.
 *
 * <p>Like all other consumers, {@code DefaultSubscriber} can be subscribed only once.
 * Any subsequent attempt to subscribe it to a new source will yield an
 * {@link IllegalStateException} with message {@code "It is not allowed to subscribe with a(n) <class name> multiple times."}.
 *
 * <p>Implementation of {@link #onStart()}, {@link #onNext(Object)}, {@link #onError(Throwable)}
 * and {@link #onComplete()} are not allowed to throw any unchecked exceptions.
 * If for some reason this can't be avoided, use {@link io.reactivex.Flowable#safeSubscribe(org.reactivestreams.Subscriber)}
 * instead of the standard {@code subscribe()} method.
 * @param <T> the value type
 *
 * <p>Example<pre><code>
 * Flowable.range(1, 5)
 *     .subscribe(new DefaultSubscriber&lt;Integer>() {
 *         &#64;Override public void onStart() {
 *             System.out.println("Start!");
 *             request(1);
 *         }
 *         &#64;Override public void onNext(Integer t) {
 *             if (t == 3) {
 *                 cancel();
 *             }
 *             System.out.println(t);
 *             request(1);
 *         }
 *         &#64;Override public void onError(Throwable t) {
 *             t.printStackTrace();
 *         }
 *         &#64;Override public void onComplete() {
 *             System.out.println("Done!");
 *         }
 *     });
 * </code></pre>
 */
public abstract class DefaultSubscriber<T> implements FlowableSubscriber<T> {
    private Subscription s;
    @Override
    public final void onSubscribe(Subscription s) {
        if (EndConsumerHelper.validate(this.s, s, getClass())) {
            this.s = s;
            onStart();
        }
    }

    /**
     * Requests from the upstream Subscription.
     * @param n the request amount, positive
     */
    protected final void request(long n) {
        Subscription s = this.s;
        if (s != null) {
            s.request(n);
        }
    }

    /**
     * Cancels the upstream's Subscription.
     */
    protected final void cancel() {
        Subscription s = this.s;
        this.s = SubscriptionHelper.CANCELLED;
        s.cancel();
    }
    /**
     * Called once the subscription has been set on this observer; override this
     * to perform initialization or issue an initial request.
     * <p>
     * The default implementation requests {@link Long#MAX_VALUE}.
     */
    protected void onStart() {
        request(Long.MAX_VALUE);
    }

}
