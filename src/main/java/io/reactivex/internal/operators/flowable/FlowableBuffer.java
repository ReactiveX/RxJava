/**
 * Copyright 2016 Netflix, Inc.
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

    public FlowableBuffer(Publisher<T> source, int size, Callable<C> bufferSupplier) {
        this(source, size, size, bufferSupplier);
    }

    public FlowableBuffer(Publisher<T> source, int size, int skip, Callable<C> bufferSupplier) {
        super(source);
        if (size <= 0) {
            throw new IllegalArgumentException("size > 0 required but it was " + size);
        }

        if (skip <= 0) {
            throw new IllegalArgumentException("skip > 0 required but it was " + size);
        }

        this.size = size;
        this.skip = skip;
        this.bufferSupplier = ObjectHelper.requireNonNull(bufferSupplier, "bufferSupplier");
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
      implements Subscriber<T>, Subscription {

        final Subscriber<? super C> actual;

        final Callable<C> bufferSupplier;

        final int size;

        C buffer;

        Subscription s;

        boolean done;

        public PublisherBufferExactSubscriber(Subscriber<? super C> actual, int size, Callable<C> bufferSupplier) {
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
                    b = bufferSupplier.call();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    onError(e);
                    return;
                }

                if (b == null) {
                    cancel();

                    onError(new NullPointerException("The bufferSupplier returned a null buffer"));
                    return;
                }
                buffer = b;
            }

            b.add(t);

            if (b.size() == size) {
                buffer = null;
                actual.onNext(b);
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
    implements Subscriber<T>, Subscription {

        /** */
        private static final long serialVersionUID = -5616169793639412593L;

        final Subscriber<? super C> actual;

        final Callable<C> bufferSupplier;

        final int size;

        final int skip;

        C buffer;

        Subscription s;

        boolean done;

        long index;

        public PublisherBufferSkipSubscriber(Subscriber<? super C> actual, int size, int skip,
                Callable<C> bufferSupplier) {
            this.actual = actual;
            this.size = size;
            this.skip = skip;
            this.bufferSupplier = bufferSupplier;
        }

        @Override
        public void request(long n) {
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

            long i = index;

            if (i % skip == 0L) { // FIXME no need for modulo
                try {
                    b = bufferSupplier.call();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();

                    onError(e);
                    return;
                }

                if (b == null) {
                    cancel();

                    onError(new NullPointerException("The bufferSupplier returned a null buffer"));
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

            index = i + 1;
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
    implements Subscriber<T>, Subscription, BooleanSupplier {
        /** */
        private static final long serialVersionUID = -7370244972039324525L;

        final Subscriber<? super C> actual;

        final Callable<C> bufferSupplier;

        final int size;

        final int skip;

        final ArrayDeque<C> buffers;

        final AtomicBoolean once;

        Subscription s;

        boolean done;

        long index;

        volatile boolean cancelled;

        long produced;
        
        public PublisherBufferOverlappingSubscriber(Subscriber<? super C> actual, int size, int skip,
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

            if (!SubscriptionHelper.validate(n)) {
                return;
            }

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

            long i = index;

            if (i % skip == 0L) { // FIXME no need for modulo
                C b;

                try {
                    b = bufferSupplier.call();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    onError(e);
                    return;
                }

                if (b == null) {
                    cancel();

                    onError(new NullPointerException("The bufferSupplier returned a null buffer"));
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

            index = i + 1;
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
