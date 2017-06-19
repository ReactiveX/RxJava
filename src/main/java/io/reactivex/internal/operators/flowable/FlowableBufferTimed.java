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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.QueueDrainSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.QueueDrainHelper;
import io.reactivex.subscribers.SerializedSubscriber;

public final class FlowableBufferTimed<T, U extends Collection<? super T>> extends AbstractFlowableWithUpstream<T, U> {

    final long timespan;
    final long timeskip;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Callable<U> bufferSupplier;
    final int maxSize;
    final boolean restartTimerOnMaxSize;

    public FlowableBufferTimed(Flowable<T> source, long timespan, long timeskip, TimeUnit unit, Scheduler scheduler, Callable<U> bufferSupplier, int maxSize,
            boolean restartTimerOnMaxSize) {
        super(source);
        this.timespan = timespan;
        this.timeskip = timeskip;
        this.unit = unit;
        this.scheduler = scheduler;
        this.bufferSupplier = bufferSupplier;
        this.maxSize = maxSize;
        this.restartTimerOnMaxSize = restartTimerOnMaxSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        if (timespan == timeskip && maxSize == Integer.MAX_VALUE) {
            source.subscribe(new BufferExactUnboundedSubscriber<T, U>(
                    new SerializedSubscriber<U>(s),
                    bufferSupplier, timespan, unit, scheduler));
            return;
        }
        Scheduler.Worker w = scheduler.createWorker();

        if (timespan == timeskip) {
            source.subscribe(new BufferExactBoundedSubscriber<T, U>(
                    new SerializedSubscriber<U>(s),
                    bufferSupplier,
                    timespan, unit, maxSize, restartTimerOnMaxSize, w
            ));
            return;
        }
        // Can't use maxSize because what to do if a buffer is full but its
        // timespan hasn't been elapsed?
        source.subscribe(new BufferSkipBoundedSubscriber<T, U>(
                new SerializedSubscriber<U>(s),
                bufferSupplier, timespan, timeskip, unit, w));
    }


    static final class BufferExactUnboundedSubscriber<T, U extends Collection<? super T>>
    extends QueueDrainSubscriber<T, U, U> implements Subscription, Runnable, Disposable {
        final Callable<U> bufferSupplier;
        final long timespan;
        final TimeUnit unit;
        final Scheduler scheduler;

        Subscription s;

        U buffer;

        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();

        BufferExactUnboundedSubscriber(
                Subscriber<? super U> actual, Callable<U> bufferSupplier,
                long timespan, TimeUnit unit, Scheduler scheduler) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.timespan = timespan;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                U b;

                try {
                    b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The supplied buffer is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    EmptySubscription.error(e, actual);
                    return;
                }

                buffer = b;

                actual.onSubscribe(this);

                if (!cancelled) {
                    s.request(Long.MAX_VALUE);

                    Disposable d = scheduler.schedulePeriodicallyDirect(this, timespan, timespan, unit);
                    if (!timer.compareAndSet(null, d)) {
                        d.dispose();
                    }
                }
            }
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                U b = buffer;
                if (b != null) {
                    b.add(t);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            DisposableHelper.dispose(timer);
            synchronized (this) {
                buffer = null;
            }
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            DisposableHelper.dispose(timer);
            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }
                buffer = null;
            }
            queue.offer(b);
            done = true;
            if (enter()) {
                QueueDrainHelper.drainMaxLoop(queue, actual, false, this, this);
            }
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            s.cancel();

            DisposableHelper.dispose(timer);
        }

        @Override
        public void run() {
            U next;

            try {
                next = ObjectHelper.requireNonNull(bufferSupplier.call(), "The supplied buffer is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                actual.onError(e);
                return;
            }

            U current;

            synchronized (this) {
                current = buffer;
                if (current != null) {
                    buffer = next;
                }
            }

            if (current == null) {
                DisposableHelper.dispose(timer);
                return;
            }

            fastPathEmitMax(current, false, this);
        }

        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            actual.onNext(v);
            return true;
        }

        @Override
        public void dispose() {
            cancel();
        }

        @Override
        public boolean isDisposed() {
            return timer.get() == DisposableHelper.DISPOSED;
        }
    }

    static final class BufferSkipBoundedSubscriber<T, U extends Collection<? super T>>
    extends QueueDrainSubscriber<T, U, U> implements Subscription, Runnable {
        final Callable<U> bufferSupplier;
        final long timespan;
        final long timeskip;
        final TimeUnit unit;
        final Worker w;
        final List<U> buffers;

        Subscription s;


        BufferSkipBoundedSubscriber(Subscriber<? super U> actual,
                Callable<U> bufferSupplier, long timespan,
                long timeskip, TimeUnit unit, Worker w) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.timespan = timespan;
            this.timeskip = timeskip;
            this.unit = unit;
            this.w = w;
            this.buffers = new LinkedList<U>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (!SubscriptionHelper.validate(this.s, s)) {
                return;
            }
            this.s = s;

            final U b; // NOPMD

            try {
                b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The supplied buffer is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                w.dispose();
                s.cancel();
                EmptySubscription.error(e, actual);
                return;
            }

            buffers.add(b);

            actual.onSubscribe(this);

            s.request(Long.MAX_VALUE);

            w.schedulePeriodically(this, timeskip, timeskip, unit);

            w.schedule(new RemoveFromBuffer(b), timespan, unit);
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                for (U b : buffers) {
                    b.add(t);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            done = true;
            w.dispose();
            clear();
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            List<U> bs;
            synchronized (this) {
                bs = new ArrayList<U>(buffers);
                buffers.clear();
            }

            for (U b : bs) {
                queue.offer(b);
            }
            done = true;
            if (enter()) {
                QueueDrainHelper.drainMaxLoop(queue, actual, false, w, this);
            }
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            clear();
            s.cancel();
            w.dispose();
        }

        void clear() {
            synchronized (this) {
                buffers.clear();
            }
        }

        @Override
        public void run() {
            if (cancelled) {
                return;
            }
            final U b; // NOPMD

            try {
                b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The supplied buffer is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                actual.onError(e);
                return;
            }

            synchronized (this) {
                if (cancelled) {
                    return;
                }
                buffers.add(b);
            }

            w.schedule(new RemoveFromBuffer(b), timespan, unit);
        }

        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            a.onNext(v);
            return true;
        }

        final class RemoveFromBuffer implements Runnable {
            private final U buffer;

            RemoveFromBuffer(U buffer) {
                this.buffer = buffer;
            }

            @Override
            public void run() {
                synchronized (BufferSkipBoundedSubscriber.this) {
                    buffers.remove(buffer);
                }

                fastPathOrderedEmitMax(buffer, false, w);
            }
        }
    }

    static final class BufferExactBoundedSubscriber<T, U extends Collection<? super T>>
    extends QueueDrainSubscriber<T, U, U> implements Subscription, Runnable, Disposable {
        final Callable<U> bufferSupplier;
        final long timespan;
        final TimeUnit unit;
        final int maxSize;
        final boolean restartTimerOnMaxSize;
        final Worker w;

        U buffer;

        Disposable timer;

        Subscription s;

        long producerIndex;

        long consumerIndex;

        BufferExactBoundedSubscriber(
                Subscriber<? super U> actual,
                Callable<U> bufferSupplier,
                long timespan, TimeUnit unit, int maxSize,
                boolean restartOnMaxSize, Worker w) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.timespan = timespan;
            this.unit = unit;
            this.maxSize = maxSize;
            this.restartTimerOnMaxSize = restartOnMaxSize;
            this.w = w;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (!SubscriptionHelper.validate(this.s, s)) {
                return;
            }
            this.s = s;

            U b;

            try {
                b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The supplied buffer is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                w.dispose();
                s.cancel();
                EmptySubscription.error(e, actual);
                return;
            }

            buffer = b;

            actual.onSubscribe(this);

            timer = w.schedulePeriodically(this, timespan, timespan, unit);

            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }

                b.add(t);

                if (b.size() < maxSize) {
                    return;
                }

                buffer = null;
                producerIndex++;
            }

            if (restartTimerOnMaxSize) {
                timer.dispose();
            }

            fastPathOrderedEmitMax(b, false, this);

            try {
                b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The supplied buffer is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                actual.onError(e);
                return;
            }

            synchronized (this) {
                buffer = b;
                consumerIndex++;
            }
            if (restartTimerOnMaxSize) {
                timer = w.schedulePeriodically(this, timespan, timespan, unit);
            }
        }

        @Override
        public void onError(Throwable t) {
            synchronized (this) {
                buffer = null;
            }
            actual.onError(t);
            w.dispose();
        }

        @Override
        public void onComplete() {
            U b;
            synchronized (this) {
                b = buffer;
                buffer = null;
            }

            queue.offer(b);
            done = true;
            if (enter()) {
                QueueDrainHelper.drainMaxLoop(queue, actual, false, this, this);
            }

            w.dispose();
        }

        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            a.onNext(v);
            return true;
        }


        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                dispose();
            }
        }

        @Override
        public void dispose() {
            synchronized (this) {
                buffer = null;
            }
            s.cancel();
            w.dispose();
        }

        @Override
        public boolean isDisposed() {
            return w.isDisposed();
        }

        @Override
        public void run() {
            U next;

            try {
                next = ObjectHelper.requireNonNull(bufferSupplier.call(), "The supplied buffer is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                actual.onError(e);
                return;
            }

            U current;

            synchronized (this) {
                current = buffer;
                if (current == null || producerIndex != consumerIndex) {
                    return;
                }
                buffer = next;
            }

            fastPathOrderedEmitMax(current, false, this);
        }
    }
}
