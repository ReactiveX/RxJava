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

package io.reactivex.processors;

import io.reactivex.annotations.Nullable;
import org.reactivestreams.*;

import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Serializes calls to the Subscriber methods.
 * <p>All other Publisher and Subject methods are thread-safe by design.
 *
 * @param <T> the item value type
 */
/* public */ final class SerializedProcessor<T> extends FlowableProcessor<T> {
    /** The actual subscriber to serialize Subscriber calls to. */
    final FlowableProcessor<T> actual;
    /** Indicates an emission is going on, guarded by this. */
    boolean emitting;
    /** If not null, it holds the missed NotificationLite events. */
    AppendOnlyLinkedArrayList<Object> queue;
    /** Indicates a terminal event has been received and all further events will be dropped. */
    volatile boolean done;

    /**
     * Constructor that wraps an actual subject.
     * @param actual the subject wrapped
     */
    SerializedProcessor(final FlowableProcessor<T> actual) {
        this.actual = actual;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        actual.subscribe(s);
    }

    @Override
    public void onSubscribe(Subscription s) {
        boolean cancel;
        if (!done) {
            synchronized (this) {
                if (done) {
                    cancel = true;
                } else {
                    if (emitting) {
                        AppendOnlyLinkedArrayList<Object> q = queue;
                        if (q == null) {
                            q = new AppendOnlyLinkedArrayList<Object>(4);
                            queue = q;
                        }
                        q.add(NotificationLite.subscription(s));
                        return;
                    }
                    emitting = true;
                    cancel = false;
                }
            }
        } else {
            cancel = true;
        }
        if (cancel) {
            s.cancel();
        } else {
            actual.onSubscribe(s);
            emitLoop();
        }
    }

    @Override
    public void onNext(T t) {
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
                    q = new AppendOnlyLinkedArrayList<Object>(4);
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
            } else {
                done = true;
                if (emitting) {
                    AppendOnlyLinkedArrayList<Object> q = queue;
                    if (q == null) {
                        q = new AppendOnlyLinkedArrayList<Object>(4);
                        queue = q;
                    }
                    q.setFirst(NotificationLite.error(t));
                    return;
                }
                reportError = false;
                emitting = true;
            }
        }
        if (reportError) {
            RxJavaPlugins.onError(t);
            return;
        }
        actual.onError(t);
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
            done = true;
            if (emitting) {
                AppendOnlyLinkedArrayList<Object> q = queue;
                if (q == null) {
                    q = new AppendOnlyLinkedArrayList<Object>(4);
                    queue = q;
                }
                q.add(NotificationLite.complete());
                return;
            }
            emitting = true;
        }
        actual.onComplete();
    }

    /** Loops until all notifications in the queue has been processed. */
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

            q.accept(actual);
        }
    }

    @Override
    public boolean hasSubscribers() {
        return actual.hasSubscribers();
    }

    @Override
    public boolean hasThrowable() {
        return actual.hasThrowable();
    }

    @Override
    @Nullable
    public Throwable getThrowable() {
        return actual.getThrowable();
    }

    @Override
    public boolean hasComplete() {
        return actual.hasComplete();
    }
}
