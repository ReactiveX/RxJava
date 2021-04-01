/*
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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.observers.QueueDrainObserver;
import io.reactivex.rxjava3.internal.queue.MpscLinkedQueue;
import io.reactivex.rxjava3.internal.util.QueueDrainHelper;
import io.reactivex.rxjava3.observers.SerializedObserver;

public final class ObservableBufferTimed<T, U extends Collection<? super T>>
extends AbstractObservableWithUpstream<T, U> {

    final long timespan;
    final long timeskip;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Supplier<U> bufferSupplier;
    final int maxSize;
    final boolean restartTimerOnMaxSize;

    public ObservableBufferTimed(ObservableSource<T> source, long timespan, long timeskip, TimeUnit unit, Scheduler scheduler, Supplier<U> bufferSupplier, int maxSize,
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
    protected void subscribeActual(Observer<? super U> t) {
        if (timespan == timeskip && maxSize == Integer.MAX_VALUE) {
            source.subscribe(new BufferExactUnboundedObserver<>(
                    new SerializedObserver<>(t),
                    bufferSupplier, timespan, unit, scheduler));
            return;
        }
        Scheduler.Worker w = scheduler.createWorker();

        if (timespan == timeskip) {
            source.subscribe(new BufferExactBoundedObserver<>(
                    new SerializedObserver<>(t),
                    bufferSupplier,
                    timespan, unit, maxSize, restartTimerOnMaxSize, w
            ));
            return;
        }
        // Can't use maxSize because what to do if a buffer is full but its
        // timespan hasn't been elapsed?
        source.subscribe(new BufferSkipBoundedObserver<>(
                new SerializedObserver<>(t),
                bufferSupplier, timespan, timeskip, unit, w));

    }

    static final class BufferExactUnboundedObserver<T, U extends Collection<? super T>>
    extends QueueDrainObserver<T, U, U> implements Runnable, Disposable {
        final Supplier<U> bufferSupplier;
        final long timespan;
        final TimeUnit unit;
        final Scheduler scheduler;

        Disposable upstream;

        U buffer;

        final AtomicReference<Disposable> timer = new AtomicReference<>();

        BufferExactUnboundedObserver(
                Observer<? super U> actual, Supplier<U> bufferSupplier,
                long timespan, TimeUnit unit, Scheduler scheduler) {
            super(actual, new MpscLinkedQueue<>());
            this.bufferSupplier = bufferSupplier;
            this.timespan = timespan;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                U b;

                try {
                    b = Objects.requireNonNull(bufferSupplier.get(), "The buffer supplied is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    dispose();
                    EmptyDisposable.error(e, downstream);
                    return;
                }

                buffer = b;

                downstream.onSubscribe(this);

                if (!DisposableHelper.isDisposed(timer.get())) {
                    Disposable task = scheduler.schedulePeriodicallyDirect(this, timespan, timespan, unit);
                    DisposableHelper.set(timer, task);
                }
            }
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                U b = buffer;
                if (b == null) {
                    return;
                }
                b.add(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            synchronized (this) {
                buffer = null;
            }
            downstream.onError(t);
            DisposableHelper.dispose(timer);
        }

        @Override
        public void onComplete() {
            U b;
            synchronized (this) {
                b = buffer;
                buffer = null;
            }
            if (b != null) {
                queue.offer(b);
                done = true;
                if (enter()) {
                    QueueDrainHelper.drainLoop(queue, downstream, false, null, this);
                }
            }
            DisposableHelper.dispose(timer);
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(timer);
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return timer.get() == DisposableHelper.DISPOSED;
        }

        @Override
        public void run() {
            U next;

            try {
                next = Objects.requireNonNull(bufferSupplier.get(), "The bufferSupplier returned a null buffer");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                dispose();
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

            fastPathEmit(current, false, this);
        }

        @Override
        public void accept(Observer<? super U> a, U v) {
            downstream.onNext(v);
        }
    }

    static final class BufferSkipBoundedObserver<T, U extends Collection<? super T>>
    extends QueueDrainObserver<T, U, U> implements Runnable, Disposable {
        final Supplier<U> bufferSupplier;
        final long timespan;
        final long timeskip;
        final TimeUnit unit;
        final Worker w;
        final List<U> buffers;

        Disposable upstream;

        BufferSkipBoundedObserver(Observer<? super U> actual,
                Supplier<U> bufferSupplier, long timespan,
                long timeskip, TimeUnit unit, Worker w) {
            super(actual, new MpscLinkedQueue<>());
            this.bufferSupplier = bufferSupplier;
            this.timespan = timespan;
            this.timeskip = timeskip;
            this.unit = unit;
            this.w = w;
            this.buffers = new LinkedList<>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                final U b; // NOPMD

                try {
                    b = Objects.requireNonNull(bufferSupplier.get(), "The buffer supplied is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    d.dispose();
                    EmptyDisposable.error(e, downstream);
                    w.dispose();
                    return;
                }

                buffers.add(b);

                downstream.onSubscribe(this);

                w.schedulePeriodically(this, timeskip, timeskip, unit);

                w.schedule(new RemoveFromBufferEmit(b), timespan, unit);
            }
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
            clear();
            downstream.onError(t);
            w.dispose();
        }

        @Override
        public void onComplete() {
            List<U> bs;
            synchronized (this) {
                bs = new ArrayList<>(buffers);
                buffers.clear();
            }

            for (U b : bs) {
                queue.offer(b);
            }
            done = true;
            if (enter()) {
                QueueDrainHelper.drainLoop(queue, downstream, false, w, this);
            }
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                clear();
                upstream.dispose();
                w.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
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
                b = Objects.requireNonNull(bufferSupplier.get(), "The bufferSupplier returned a null buffer");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                dispose();
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
        public void accept(Observer<? super U> a, U v) {
            a.onNext(v);
        }

        final class RemoveFromBuffer implements Runnable {
            private final U b;

            RemoveFromBuffer(U b) {
                this.b = b;
            }

            @Override
            public void run() {
                synchronized (BufferSkipBoundedObserver.this) {
                    buffers.remove(b);
                }

                fastPathOrderedEmit(b, false, w);
            }
        }

        final class RemoveFromBufferEmit implements Runnable {
            private final U buffer;

            RemoveFromBufferEmit(U buffer) {
                this.buffer = buffer;
            }

            @Override
            public void run() {
                synchronized (BufferSkipBoundedObserver.this) {
                    buffers.remove(buffer);
                }

                fastPathOrderedEmit(buffer, false, w);
            }
        }
    }

    static final class BufferExactBoundedObserver<T, U extends Collection<? super T>>
    extends QueueDrainObserver<T, U, U> implements Runnable, Disposable {
        final Supplier<U> bufferSupplier;
        final long timespan;
        final TimeUnit unit;
        final int maxSize;
        final boolean restartTimerOnMaxSize;
        final Worker w;

        U buffer;

        Disposable timer;

        Disposable upstream;

        long producerIndex;

        long consumerIndex;

        BufferExactBoundedObserver(
                Observer<? super U> actual,
                Supplier<U> bufferSupplier,
                long timespan, TimeUnit unit, int maxSize,
                boolean restartOnMaxSize, Worker w) {
            super(actual, new MpscLinkedQueue<>());
            this.bufferSupplier = bufferSupplier;
            this.timespan = timespan;
            this.unit = unit;
            this.maxSize = maxSize;
            this.restartTimerOnMaxSize = restartOnMaxSize;
            this.w = w;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                U b;

                try {
                    b = Objects.requireNonNull(bufferSupplier.get(), "The buffer supplied is null");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    d.dispose();
                    EmptyDisposable.error(e, downstream);
                    w.dispose();
                    return;
                }

                buffer = b;

                downstream.onSubscribe(this);

                timer = w.schedulePeriodically(this, timespan, timespan, unit);
            }
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

            fastPathOrderedEmit(b, false, this);

            try {
                b = Objects.requireNonNull(bufferSupplier.get(), "The buffer supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                dispose();
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
            downstream.onError(t);
            w.dispose();
        }

        @Override
        public void onComplete() {
            w.dispose();

            U b;
            synchronized (this) {
                b = buffer;
                buffer = null;
            }

            if (b != null) {
                queue.offer(b);
                done = true;
                if (enter()) {
                    QueueDrainHelper.drainLoop(queue, downstream, false, this, this);
                }
            }
        }

        @Override
        public void accept(Observer<? super U> a, U v) {
            a.onNext(v);
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                upstream.dispose();
                w.dispose();
                synchronized (this) {
                    buffer = null;
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @Override
        public void run() {
            U next;

            try {
                next = Objects.requireNonNull(bufferSupplier.get(), "The bufferSupplier returned a null buffer");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                dispose();
                downstream.onError(e);
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

            fastPathOrderedEmit(current, false, this);
        }
    }
}
