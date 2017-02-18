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

package io.reactivex.internal.operators.flowable;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableBuffer<T, C extends Collection<? super T>> extends AbstractFlowableWithUpstream<T, C> {
    final int size;

    final int skip;

    final Callable<C> bufferSupplier;

    public FlowableBuffer(Flowable<T> source, int size, int skip, Callable<C> bufferSupplier) {
        super(source);
        this.size = size;
        this.skip = skip;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    public void subscribeActual(Subscriber<? super C> s) {
        if (size == skip) {
            source.subscribe(new PublisherBufferExactSubscriber<T, C>(s, size, bufferSupplier));
        } else if (skip > size) {
            source.subscribe(new PublisherBufferSkipSubscriber<T, C>(s, size, skip, bufferSupplier));
        } else {
            source.subscribe(new PublisherBufferOverlappingSubscriber<T, C>(s, size, skip, bufferSupplier));
        }
    }

    static final class PublisherBufferExactSubscriber<T, C extends Collection<? super T>>
      implements FlowableSubscriber<T>, Subscription {

        final Subscriber<? super C> actual;

        final Callable<C> bufferSupplier;

        final int size;

        C buffer;

        Subscription s;

        boolean done;

        int index;

        PublisherBufferExactSubscriber(Subscriber<? super C> actual, int size, Callable<C> bufferSupplier) {
            this.actual = actual;
            this.size = size;
            this.bufferSupplier = bufferSupplier;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                s.request(BackpressureHelper.multiplyCap(n, size));
            }
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            C b = buffer;
            if (b == null) {

                try {
                    b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The bufferSupplier returned a null buffer");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    onError(e);
                    return;
                }

                buffer = b;
            }

            b.add(t);

            int i = index + 1;
            if (i == size) {
                index = 0;
                buffer = null;
                actual.onNext(b);
            } else {
                index = i;
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            C b = buffer;

            if (b != null && !b.isEmpty()) {
                actual.onNext(b);
            }
            actual.onComplete();
        }
    }

    static final class PublisherBufferSkipSubscriber<T, C extends Collection<? super T>>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription {


        private static final long serialVersionUID = -5616169793639412593L;

        final Subscriber<? super C> actual;

        final Callable<C> bufferSupplier;

        final int size;

        final int skip;

        C buffer;

        Subscription s;

        boolean done;

        int index;

        PublisherBufferSkipSubscriber(Subscriber<? super C> actual, int size, int skip,
                Callable<C> bufferSupplier) {
            this.actual = actual;
            this.size = size;
            this.skip = skip;
            this.bufferSupplier = bufferSupplier;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                if (get() == 0 && compareAndSet(0, 1)) {
                    // n full buffers
                    long u = BackpressureHelper.multiplyCap(n, size);
                    // + (n - 1) gaps
                    long v = BackpressureHelper.multiplyCap(skip - size, n - 1);

                    s.request(BackpressureHelper.addCap(u, v));
                } else {
                    // n full buffer + gap
                    s.request(BackpressureHelper.multiplyCap(skip, n));
                }
            }
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            C b = buffer;

            int i = index;

            if (i++ == 0) {
                try {
                    b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The bufferSupplier returned a null buffer");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();

                    onError(e);
                    return;
                }

                buffer = b;
            }

            if (b != null) {
                b.add(t);
                if (b.size() == size) {
                    buffer = null;
                    actual.onNext(b);
                }
            }

            if (i == skip) {
                i = 0;
            }
            index = i;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }

            done = true;
            buffer = null;

            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }

            done = true;
            C b = buffer;
            buffer = null;

            if (b != null) {
                actual.onNext(b);
            }

            actual.onComplete();
        }
    }


    static final class PublisherBufferOverlappingSubscriber<T, C extends Collection<? super T>>
    extends AtomicLong
    implements FlowableSubscriber<T>, Subscription, BooleanSupplier {

        private static final long serialVersionUID = -7370244972039324525L;

        final Subscriber<? super C> actual;

        final Callable<C> bufferSupplier;

        final int size;

        final int skip;

        final ArrayDeque<C> buffers;

        final AtomicBoolean once;

        Subscription s;

        boolean done;

        int index;

        volatile boolean cancelled;

        long produced;

        PublisherBufferOverlappingSubscriber(Subscriber<? super C> actual, int size, int skip,
                Callable<C> bufferSupplier) {
            this.actual = actual;
            this.size = size;
            this.skip = skip;
            this.bufferSupplier = bufferSupplier;
            this.once = new AtomicBoolean();
            this.buffers = new ArrayDeque<C>();
        }

        @Override
        public boolean getAsBoolean() {
            return cancelled;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                if (QueueDrainHelper.postCompleteRequest(n, actual, buffers, this, this)) {
                    return;
                }

                if (!once.get() && once.compareAndSet(false, true)) {
                    // (n - 1) skips
                    long u = BackpressureHelper.multiplyCap(skip, n - 1);

                    // + 1 full buffer
                    long r = BackpressureHelper.addCap(size, u);
                    s.request(r);
                } else {
                    // n skips
                    long r = BackpressureHelper.multiplyCap(skip, n);
                    s.request(r);
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            ArrayDeque<C> bs = buffers;

            int i = index;

            if (i++ == 0) {
                C b;

                try {
                    b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The bufferSupplier returned a null buffer");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    onError(e);
                    return;
                }

                bs.offer(b);
            }

            C b = bs.peek();

            if (b != null && b.size() + 1 == size) {
                bs.poll();

                b.add(t);

                produced++;

                actual.onNext(b);
            }

            for (C b0 : bs) {
                b0.add(t);
            }

            if (i == skip) {
                i = 0;
            }
            index = i;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }

            done = true;
            buffers.clear();

            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }

            done = true;

            long p = produced;
            if (p != 0L) {
                BackpressureHelper.produced(this, p);
            }
            QueueDrainHelper.postComplete(actual, buffers, this, this);
        }
    }
}
