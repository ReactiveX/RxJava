/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.nbp.NbpQueueDrainSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorBufferTimed<T, U extends Collection<? super T>> implements NbpOperator<U, T> {

    final long timespan;
    final long timeskip;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Supplier<U> bufferSupplier;
    final int maxSize;
    final boolean restartTimerOnMaxSize;
    
    public NbpOperatorBufferTimed(long timespan, long timeskip, TimeUnit unit, Scheduler scheduler, Supplier<U> bufferSupplier, int maxSize,
            boolean restartTimerOnMaxSize) {
        this.timespan = timespan;
        this.timeskip = timeskip;
        this.unit = unit;
        this.scheduler = scheduler;
        this.bufferSupplier = bufferSupplier;
        this.maxSize = maxSize;
        this.restartTimerOnMaxSize = restartTimerOnMaxSize;
    }

    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super U> t) {
        if (timespan == timeskip && maxSize == Integer.MAX_VALUE) {
            return new BufferExactUnboundedSubscriber<>(
                    new NbpSerializedSubscriber<>(t), 
                    bufferSupplier, timespan, unit, scheduler);
        }
        Scheduler.Worker w = scheduler.createWorker();

        if (timespan == timeskip) {
            return new BufferExactBoundedSubscriber<>(
                    new NbpSerializedSubscriber<>(t),
                    bufferSupplier,
                    timespan, unit, maxSize, restartTimerOnMaxSize, w
            );
        }
        // Can't use maxSize because what to do if a buffer is full but its
        // timespan hasn't been elapsed?
        return new BufferSkipBoundedSubscriber<>(
                new NbpSerializedSubscriber<>(t),
                bufferSupplier, timespan, timeskip, unit, w);
    }
    
    static final class BufferExactUnboundedSubscriber<T, U extends Collection<? super T>>
    extends NbpQueueDrainSubscriber<T, U, U> implements Runnable, Disposable {
        final Supplier<U> bufferSupplier;
        final long timespan;
        final TimeUnit unit;
        final Scheduler scheduler;
        
        Disposable s;
        
        U buffer;
        
        boolean selfCancel;
        
        volatile Disposable timer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<BufferExactUnboundedSubscriber, Disposable> TIMER =
                AtomicReferenceFieldUpdater.newUpdater(BufferExactUnboundedSubscriber.class, Disposable.class, "timer");
        
        static final Disposable CANCELLED = () -> { };

        public BufferExactUnboundedSubscriber(
                NbpSubscriber<? super U> actual, Supplier<U> bufferSupplier,
                long timespan, TimeUnit unit, Scheduler scheduler) {
            super(actual, new MpscLinkedQueue<>());
            this.bufferSupplier = bufferSupplier;
            this.timespan = timespan;
            this.unit = unit;
            this.scheduler = scheduler;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            U b;
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                dispose();
                EmptyDisposable.error(e, actual);
                return;
            }
            
            if (b == null) {
                dispose();
                EmptyDisposable.error(new NullPointerException("buffer supplied is null"), actual);
                return;
            }
            
            buffer = b;
            
            actual.onSubscribe(this);
            
            if (!cancelled) {
                Disposable d = scheduler.schedulePeriodicallyDirect(this, timespan, timespan, unit);
                if (!TIMER.compareAndSet(this, null, d)) {
                    d.dispose();
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
            disposeTimer();
            synchronized (this) {
                buffer = null;
            }
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            disposeTimer();
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
                drainLoop(queue, actual, false, this);
            }
        }
        
        @Override
        public void dispose() {
            disposeTimer();
            s.dispose();
        }
        
        void disposeTimer() {
            Disposable d = timer;
            if (d != CANCELLED) {
                d = TIMER.getAndSet(this, CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public void run() {
            /*
             * If running on a synchronous scheduler, the timer might never
             * be set so the periodic timer can't be stopped this loopback way.
             * The last resort is to crash the task so it hopefully won't
             * be rescheduled.
             */
            if (selfCancel) {
                throw new CancellationException();
            }
            
            U next;
            
            try {
                next = bufferSupplier.get();
            } catch (Throwable e) {
                selfCancel = true;
                dispose();
                actual.onError(e);
                return;
            }
            
            if (next == null) {
                selfCancel = true;
                dispose();
                actual.onError(new NullPointerException("buffer supplied is null"));
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
                selfCancel = true;
                disposeTimer();
                return;
            }

            fastpathEmit(current, false, this);
        }
        
        @Override
        public void accept(NbpSubscriber<? super U> a, U v) {
            actual.onNext(v);
        }
    }
    
    static final class BufferSkipBoundedSubscriber<T, U extends Collection<? super T>>
    extends NbpQueueDrainSubscriber<T, U, U> implements Runnable, Disposable {
        final Supplier<U> bufferSupplier;
        final long timespan;
        final long timeskip;
        final TimeUnit unit;
        final Worker w;
        
        Disposable s;
        
        List<U> buffers;
        
        public BufferSkipBoundedSubscriber(NbpSubscriber<? super U> actual, 
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
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            U b;

            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                w.dispose();
                s.dispose();
                EmptyDisposable.error(e, actual);
                return;
            }
            
            if (b == null) {
                w.dispose();
                s.dispose();
                EmptyDisposable.error(new NullPointerException("The supplied buffer is null"), actual);
                return;
            }
            
            buffers.add(b);

            actual.onSubscribe(this);
            
            w.schedulePeriodically(this, timeskip, timeskip, unit);
            
            w.schedule(() -> {
                synchronized (this) {
                    buffers.remove(b);
                }
                
                fastpathOrderedEmit(b, false, w);
            }, timespan, unit);
        }
        
        @Override
        public void onNext(T t) {
            synchronized (this) {
                buffers.forEach(b -> b.add(t));
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
                bs = new ArrayList<>(buffers);
                buffers.clear();
            }
            
            bs.forEach(b -> queue.add(b));
            done = true;
            if (enter()) {
                drainLoop(queue, actual, false, w);
            }
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                w.dispose();
                clear();
                s.dispose();
            }
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
            U b;
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                dispose();
                actual.onError(e);
                return;
            }
            
            if (b == null) {
                dispose();
                actual.onError(new NullPointerException("The supplied buffer is null"));
                return;
            }
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                buffers.add(b);
            }
            
            w.schedule(() -> {
                synchronized (this) {
                    buffers.remove(b);
                }
                
                fastpathOrderedEmit(b, false, w);
            }, timespan, unit);
        }
        
        @Override
        public void accept(NbpSubscriber<? super U> a, U v) {
            a.onNext(v);
        }
    }
    
    static final class BufferExactBoundedSubscriber<T, U extends Collection<? super T>>
    extends NbpQueueDrainSubscriber<T, U, U> implements Runnable, Disposable {
        final Supplier<U> bufferSupplier;
        final long timespan;
        final TimeUnit unit;
        final int maxSize;
        final boolean restartTimerOnMaxSize;
        final Worker w;

        U buffer;
        
        Disposable timer;
        
        Disposable s;
        
        long producerIndex;
        
        long consumerIndex;

        public BufferExactBoundedSubscriber(
                NbpSubscriber<? super U> actual,
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
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            U b;

            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                w.dispose();
                s.dispose();
                EmptyDisposable.error(e, actual);
                return;
            }
            
            if (b == null) {
                w.dispose();
                s.dispose();
                EmptyDisposable.error(new NullPointerException("The supplied buffer is null"), actual);
                return;
            }
            
            buffer = b;
            
            actual.onSubscribe(this);

            timer = w.schedulePeriodically(this, timespan, timespan, unit);
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
            }

            if (restartTimerOnMaxSize) {
                buffer = null;
                producerIndex++;
                
                timer.dispose();
            }
            
            fastpathOrderedEmit(b, false, this);
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                dispose();
                actual.onError(e);
                return;
            }
            
            if (b == null) {
                dispose();
                actual.onError(new NullPointerException("The buffer supplied is null"));
                return;
            }


            
            if (restartTimerOnMaxSize) {
                synchronized (this) {
                    buffer = b;
                    consumerIndex++;
                }
                
                timer = w.schedulePeriodically(this, timespan, timespan, unit);
            } else {
                synchronized (this) {
                    buffer = b;
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            w.dispose();
            synchronized (this) {
                buffer = null;
            }
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            w.dispose();
            
            U b;
            synchronized (this) {
                b = buffer;
                buffer = null;
            }
            
            queue.offer(b);
            done = true;
            if (enter()) {
                drainLoop(queue, actual, false, this);
            }
        }
        
        @Override
        public void accept(NbpSubscriber<? super U> a, U v) {
            a.onNext(v);
        }
        
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                w.dispose();
                synchronized (this) {
                    buffer = null;
                }
                s.dispose();
            }
        }
        
        
        @Override
        public void run() {
            U next;
            
            try {
                next = bufferSupplier.get();
            } catch (Throwable e) {
                dispose();
                actual.onError(e);
                return;
            }
            
            if (next == null) {
                dispose();
                actual.onError(new NullPointerException("The buffer supplied is null"));
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

            fastpathOrderedEmit(current, false, this);
        }
    }
}
