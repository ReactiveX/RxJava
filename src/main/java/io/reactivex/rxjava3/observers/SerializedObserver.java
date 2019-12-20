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
package io.reactivex.rxjava3.observers;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Serializes access to the {@link Observer#onNext(Object)}, {@link Observer#onError(Throwable)} and
 * {@link Observer#onComplete()} methods of another {@link Observer}.
 *
 * <p>Note that {@link #onSubscribe(Disposable)} is not serialized in respect of the other methods so
 * make sure the {@code onSubscribe()} is called with a non-null {@link Disposable}
 * before any of the other methods are called.
 *
 * <p>The implementation assumes that the actual {@code Observer}'s methods don't throw.
 *
 * @param <T> the value type
 */
public final class SerializedObserver<T> implements Observer<T>, Disposable {
    final Observer<? super T> downstream;
    final boolean delayError;

    static final int QUEUE_LINK_SIZE = 4;

    Disposable upstream;

    boolean emitting;
    AppendOnlyLinkedArrayList<Object> queue;

    volatile boolean done;

    /**
     * Construct a {@code SerializedObserver} by wrapping the given actual {@link Observer}.
     * @param downstream the actual {@code Observer}, not {@code null} (not verified)
     */
    public SerializedObserver(@NonNull Observer<? super T> downstream) {
        this(downstream, false);
    }

    /**
     * Construct a SerializedObserver by wrapping the given actual {@link Observer} and
     * optionally delaying the errors till all regular values have been emitted
     * from the internal buffer.
     * @param actual the actual {@code Observer}, not {@code null} (not verified)
     * @param delayError if {@code true}, errors are emitted after regular values have been emitted
     */
    public SerializedObserver(@NonNull Observer<? super T> actual, boolean delayError) {
        this.downstream = actual;
        this.delayError = delayError;
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        if (DisposableHelper.validate(this.upstream, d)) {
            this.upstream = d;

            downstream.onSubscribe(this);
        }
    }

    @Override
    public void dispose() {
        done = true;
        upstream.dispose();
    }

    @Override
    public boolean isDisposed() {
        return upstream.isDisposed();
    }

    @Override
    public void onNext(@NonNull T t) {
        if (done) {
            return;
        }
        if (t == null) {
            upstream.dispose();
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
    public void onError(@NonNull Throwable t) {
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
}
