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
package io.reactivex.rxjava3.internal.jdk8;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Wraps a {@link Stream} and emits its values as a Flowable sequence.
 * @param <T> the element type of the Stream
 * @since 3.0.0
 */
public final class FlowableFromStream<T> extends Flowable<T> {

    final Stream<T> stream;

    public FlowableFromStream(Stream<T> stream) {
        this.stream = stream;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        subscribeStream(s, stream);
    }

    /**
     * Subscribes to the Stream by picking the normal or conditional stream Subscription implementation.
     * @param <T> the element type of the flow
     * @param s the subscriber to drive
     * @param stream the sequence to consume
     */
    public static <T> void subscribeStream(Subscriber<? super T> s, Stream<T> stream) {
        Iterator<T> iterator;
        try {
            iterator = stream.iterator();

            if (!iterator.hasNext()) {
                EmptySubscription.complete(s);
                closeSafely(stream);
                return;
            }
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            closeSafely(stream);
            return;
        }

        if (s instanceof ConditionalSubscriber) {
            s.onSubscribe(new StreamConditionalSubscription<T>((ConditionalSubscriber<? super T>)s, iterator, stream));
        } else {
            s.onSubscribe(new StreamSubscription<>(s, iterator, stream));
        }
    }

    static void closeSafely(AutoCloseable c) {
        try {
            c.close();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    }

    abstract static class AbstractStreamSubscription<T> extends AtomicLong implements QueueSubscription<T> {

        private static final long serialVersionUID = -9082954702547571853L;

        Iterator<T> iterator;

        AutoCloseable closeable;

        volatile boolean cancelled;

        boolean once;

        AbstractStreamSubscription(Iterator<T> iterator, AutoCloseable closeable) {
            this.iterator = iterator;
            this.closeable = closeable;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                if (BackpressureHelper.add(this, n) == 0L) {
                    run(n);
                }
            }
        }

        abstract void run(long n);

        @Override
        public void cancel() {
            cancelled = true;
            request(1L);
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & SYNC) != 0) {
                lazySet(Long.MAX_VALUE);
                return SYNC;
            }
            return NONE;
        }

        @Override
        public boolean offer(@NonNull T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(@NonNull T v1, @NonNull T v2) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public T poll() {
            if (iterator == null) {
                return null;
            }
            if (!once) {
                once = true;
            } else {
                if (!iterator.hasNext()) {
                    return null;
                }
            }
            return Objects.requireNonNull(iterator.next(), "Iterator.next() returned a null value");
        }

        @Override
        public boolean isEmpty() {
            return iterator == null || !iterator.hasNext();
        }

        @Override
        public void clear() {
            iterator = null;
            AutoCloseable c = closeable;
            closeable = null;
            if (c != null) {
                closeSafely(c);
            }
        }
    }

    static final class StreamSubscription<T> extends AbstractStreamSubscription<T> {

        private static final long serialVersionUID = -9082954702547571853L;

        final Subscriber<? super T> downstream;

        StreamSubscription(Subscriber<? super T> downstream, Iterator<T> iterator, AutoCloseable closeable) {
            super(iterator, closeable);
            this.downstream = downstream;
        }

        @Override
        public void run(long n) {
            long emitted = 0L;
            Iterator<T> iterator = this.iterator;
            Subscriber<? super T> downstream = this.downstream;

            for (;;) {

                if (cancelled) {
                    clear();
                    break;
                } else {
                    T next;
                    try {
                        next = Objects.requireNonNull(iterator.next(), "The Stream's Iterator returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        downstream.onError(ex);
                        cancelled = true;
                        continue;
                    }

                    downstream.onNext(next);

                    if (cancelled) {
                        continue;
                    }

                    try {
                        if (!iterator.hasNext()) {
                            downstream.onComplete();
                            cancelled = true;
                            continue;
                        }
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        downstream.onError(ex);
                        cancelled = true;
                        continue;
                    }

                    if (++emitted != n) {
                        continue;
                    }
                }

                n = get();
                if (emitted == n) {
                    if (compareAndSet(n, 0L)) {
                        break;
                    }
                    n = get();
                }
            }
        }
    }

    static final class StreamConditionalSubscription<T> extends AbstractStreamSubscription<T> {

        private static final long serialVersionUID = -9082954702547571853L;

        final ConditionalSubscriber<? super T> downstream;

        StreamConditionalSubscription(ConditionalSubscriber<? super T> downstream, Iterator<T> iterator, AutoCloseable closeable) {
            super(iterator, closeable);
            this.downstream = downstream;
        }

        @Override
        public void run(long n) {
            long emitted = 0L;
            Iterator<T> iterator = this.iterator;
            ConditionalSubscriber<? super T> downstream = this.downstream;

            for (;;) {

                if (cancelled) {
                    clear();
                    break;
                } else {
                    T next;
                    try {
                        next = Objects.requireNonNull(iterator.next(), "The Stream's Iterator returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        downstream.onError(ex);
                        cancelled = true;
                        continue;
                    }

                    if (downstream.tryOnNext(next)) {
                        emitted++;
                    }

                    if (cancelled) {
                        continue;
                    }

                    try {
                        if (!iterator.hasNext()) {
                            downstream.onComplete();
                            cancelled = true;
                            continue;
                        }
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        downstream.onError(ex);
                        cancelled = true;
                        continue;
                    }

                    if (emitted != n) {
                        continue;
                    }
                }

                n = get();
                if (emitted == n) {
                    if (compareAndSet(n, 0L)) {
                        break;
                    }
                    n = get();
                }
            }
        }
    }
}
