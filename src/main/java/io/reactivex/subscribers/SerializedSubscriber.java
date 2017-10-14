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

import org.reactivestreams.*;

import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Serializes access to the onNext, onError and onComplete methods of another Subscriber.
 *
 * <p>Note that {@link #onSubscribe(Subscription)} is not serialized in respect of the other methods so
 * make sure the {@code onSubscribe} is called with a non-null {@code Subscription}
 * before any of the other methods are called.
 *
 * <p>The implementation assumes that the actual Subscriber's methods don't throw.
 *
 * @param <T> the value type
 */
public final class SerializedSubscriber<T> implements FlowableSubscriber<T>, Subscription {
    final Subscriber<? super T> actual;
    final boolean delayError;

    static final int QUEUE_LINK_SIZE = 4;

    Subscription subscription;

    boolean emitting;
    AppendOnlyLinkedArrayList<Object> queue;

    volatile boolean done;

    /**
     * Construct a SerializedSubscriber by wrapping the given actual Subscriber.
     * @param actual the actual Subscriber, not null (not verified)
     */
    public SerializedSubscriber(Subscriber<? super T> actual) {
        this(actual, false);
    }

    /**
     * Construct a SerializedSubscriber by wrapping the given actual Observer and
     * optionally delaying the errors till all regular values have been emitted
     * from the internal buffer.
     * @param actual the actual Subscriber, not null (not verified)
     * @param delayError if true, errors are emitted after regular values have been emitted
     */
    public SerializedSubscriber(Subscriber<? super T> actual, boolean delayError) {
        this.actual = actual;
        this.delayError = delayError;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.subscription, s)) {
            this.subscription = s;
            actual.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }
        if (t == null) {
            subscription.cancel();
            onError(new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
            return;
        }
        synchronized (this) {
            if (done) {
                return;
            }
            if (emitting) {
                AppendOnlyLinkedArrayList<Object> q = queue;
                if (q == null) {
                    q = new AppendOnlyLinkedArrayList<Object>(QUEUE_LINK_SIZE);
                    queue = q;
                }
                q.add(NotificationLite.next(t));
                return;
            }
            emitting = true;
        }

        actual.onNext(t);

        emitLoop();
    }

    @Override
    public void onError(Throwable t) {
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        boolean reportError;
        synchronized (this) {
            if (done) {
                reportError = true;
            } else
            if (emitting) {
                done = true;
                AppendOnlyLinkedArrayList<Object> q = queue;
                if (q == null) {
                    q = new AppendOnlyLinkedArrayList<Object>(QUEUE_LINK_SIZE);
                    queue = q;
                }
                Object err = NotificationLite.error(t);
                if (delayError) {
                    q.add(err);
                } else {
                    q.setFirst(err);
                }
                return;
            } else {
                done = true;
                emitting = true;
                reportError = false;
            }
        }

        if (reportError) {
            RxJavaPlugins.onError(t);
            return;
        }

        actual.onError(t);
        // no need to loop because this onError is the last event
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        synchronized (this) {
            if (done) {
                return;
            }
            if (emitting) {
                AppendOnlyLinkedArrayList<Object> q = queue;
                if (q == null) {
                    q = new AppendOnlyLinkedArrayList<Object>(QUEUE_LINK_SIZE);
                    queue = q;
                }
                q.add(NotificationLite.complete());
                return;
            }
            done = true;
            emitting = true;
        }

        actual.onComplete();
        // no need to loop because this onComplete is the last event
    }

    void emitLoop() {
        for (;;) {
            AppendOnlyLinkedArrayList<Object> q;
            synchronized (this) {
                q = queue;
                if (q == null) {
                    emitting = false;
                    return;
                }
                queue = null;
            }

            if (q.accept(actual)) {
                return;
            }
        }
    }

    @Override
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        subscription.cancel();
    }
}
