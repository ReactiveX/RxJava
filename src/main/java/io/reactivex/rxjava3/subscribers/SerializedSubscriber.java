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
package io.reactivex.rxjava3.subscribers;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Serializes access to the {@link Subscriber#onNext(Object)}, {@link Subscriber#onError(Throwable)} and
 * {@link Subscriber#onComplete()} methods of another {@link Subscriber}.
 *
 * <p>Note that {@link #onSubscribe(Subscription)} is not serialized in respect of the other methods so
 * make sure the {@code onSubscribe} is called with a non-{@code null} {@link Subscription}
 * before any of the other methods are called.
 *
 * <p>The implementation assumes that the actual {@code Subscriber}'s methods don't throw.
 *
 * @param <T> the value type
 */
public final class SerializedSubscriber<T> implements FlowableSubscriber<T>, Subscription {
    final Subscriber<? super T> downstream;
    final boolean delayError;

    static final int QUEUE_LINK_SIZE = 4;

    Subscription upstream;

    boolean emitting;
    AppendOnlyLinkedArrayList<Object> queue;

    volatile boolean done;

    /**
     * Construct a {@code SerializedSubscriber} by wrapping the given actual {@link Subscriber}.
     * @param downstream the actual {@code Subscriber}, not null (not verified)
     */
    public SerializedSubscriber(Subscriber<? super T> downstream) {
        this(downstream, false);
    }

    /**
     * Construct a {@code SerializedSubscriber} by wrapping the given actual {@link Subscriber} and
     * optionally delaying the errors till all regular values have been emitted
     * from the internal buffer.
     * @param actual the actual {@code Subscriber}, not {@code null} (not verified)
     * @param delayError if {@code true}, errors are emitted after regular values have been emitted
     */
    public SerializedSubscriber(@NonNull Subscriber<? super T> actual, boolean delayError) {
        this.downstream = actual;
        this.delayError = delayError;
    }

    @Override
    public void onSubscribe(@NonNull Subscription s) {
        if (SubscriptionHelper.validate(this.upstream, s)) {
            this.upstream = s;
            downstream.onSubscribe(this);
        }
    }

    @Override
    public void onNext(@NonNull T t) {
        if (done) {
            return;
        }
        if (t == null) {
            upstream.cancel();
            onError(ExceptionHelper.createNullPointerException("onNext called with a null value."));
            return;
        }
        synchronized (this) {
            if (done) {
                return;
            }
            if (emitting) {
                AppendOnlyLinkedArrayList<Object> q = queue;
                if (q == null) {
                    q = new AppendOnlyLinkedArrayList<>(QUEUE_LINK_SIZE);
                    queue = q;
                }
                q.add(NotificationLite.next(t));
                return;
            }
            emitting = true;
        }

        downstream.onNext(t);

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
                    q = new AppendOnlyLinkedArrayList<>(QUEUE_LINK_SIZE);
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

        downstream.onError(t);
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
                    q = new AppendOnlyLinkedArrayList<>(QUEUE_LINK_SIZE);
                    queue = q;
                }
                q.add(NotificationLite.complete());
                return;
            }
            done = true;
            emitting = true;
        }

        downstream.onComplete();
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

            if (q.accept(downstream)) {
                return;
            }
        }
    }

    @Override
    public void request(long n) {
        upstream.request(n);
    }

    @Override
    public void cancel() {
        upstream.cancel();
    }
}
