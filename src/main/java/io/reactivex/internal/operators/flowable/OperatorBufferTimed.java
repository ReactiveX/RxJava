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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.Scheduler;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.flowable.QueueDrainSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.QueueDrainHelper;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorBufferTimed<T, U extends Collection<? super T>> implements Operator<U, T> {

    final long timespan;
    final long timeskip;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Supplier<U> bufferSupplier;
    final int maxSize;
    final boolean restartTimerOnMaxSize;
    
    public OperatorBufferTimed(long timespan, long timeskip, TimeUnit unit, Scheduler scheduler, Supplier<U> bufferSupplier, int maxSize,
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
    public Subscriber<? super T> apply(Subscriber<? super U> t) {
        if (timespan == timeskip && maxSize == Integer.MAX_VALUE) {
            return new BufferExactUnboundedSubscriber<T, U>(
                    new SerializedSubscriber<U>(t), 
                    bufferSupplier, timespan, unit, scheduler);
        }
        Scheduler.Worker w = scheduler.createWorker();

        if (timespan == timeskip) {
            return new BufferExactBoundedSubscriber<T, U>(
                    new SerializedSubscriber<U>(t),
                    bufferSupplier,
                    timespan, unit, maxSize, restartTimerOnMaxSize, w
            );
        }
        // Can't use maxSize because what to do if a buffer is full but its
        // timespan hasn't been elapsed?
        return new BufferSkipBoundedSubscriber<T, U>(
                new SerializedSubscriber<U>(t),
                bufferSupplier, timespan, timeskip, unit, w);
    }
    
    static final class BufferExactUnboundedSubscriber<T, U extends Collection<? super T>>
    extends QueueDrainSubscriber<T, U, U> implements Subscription, Runnable, Disposable {
        final Supplier<U> bufferSupplier;
        final long timespan;
        final TimeUnit unit;
        final Scheduler scheduler;
        
        Subscription s;
        
        U buffer;
        
        boolean selfCancel;
        
        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();
        
        static final Disposable CANCELLED = new Disposable() {
            @Override
            public void dispose() { }
        };

        public BufferExactUnboundedSubscriber(
                Subscriber<? super U> actual, Supplier<U> bufferSupplier,
                long timespan, TimeUnit unit, Scheduler scheduler) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.timespan = timespan;
            this.unit = unit;
            this.scheduler = scheduler;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            U b;
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                cancel();
                EmptySubscription.error(e, actual);
                return;
            }
            
            if (b == null) {
                cancel();
                EmptySubscription.error(new NullPointerException("buffer supplied is null"), actual);
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
                QueueDrainHelper.drainMaxLoop(queue, actual, false, this, this);
            }
        }
        
        @Override
        public void request(long n) {
            requested(n);
        }
        
        @Override
        public void cancel() {
            disposeTimer();
            
            s.cancel();
        }
        
        void disposeTimer() {
            Disposable d = timer.get();
            if (d != CANCELLED) {
                d = timer.getAndSet(CANCELLED);
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
                cancel();
                actual.onError(e);
                return;
            }
            
            if (next == null) {
                selfCancel = true;
                cancel();
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

            fastpathEmitMax(current, false, this);
        }
        
        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            actual.onNext(v);
            return true;
        }
        
        @Override
        public void dispose() {
            selfCancel = true;
            cancel();
        }
    }
    
    static final class BufferSkipBoundedSubscriber<T, U extends Collection<? super T>>
    extends QueueDrainSubscriber<T, U, U> implements Subscription, Runnable {
        final Supplier<U> bufferSupplier;
        final long timespan;
        final long timeskip;
        final TimeUnit unit;
        final Worker w;
        
        Subscription s;
        
        List<U> buffers;
        
        public BufferSkipBoundedSubscriber(Subscriber<? super U> actual, 
                Supplier<U> bufferSupplier, long timespan,
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
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            final U b;

            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                w.dispose();
                s.cancel();
                EmptySubscription.error(e, actual);
                return;
            }
            
            if (b == null) {
                w.dispose();
                s.cancel();
                EmptySubscription.error(new NullPointerException("The supplied buffer is null"), actual);
                return;
            }
            
            buffers.add(b);

            actual.onSubscribe(this);
            
            s.request(Long.MAX_VALUE);

            w.schedulePeriodically(this, timeskip, timeskip, unit);
            
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    synchronized (BufferSkipBoundedSubscriber.this) {
                        buffers.remove(b);
                    }
                    
                    fastpathOrderedEmitMax(b, false, w);
                }
            }, timespan, unit);
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
                queue.add(b);
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
            w.dispose();
            clear();
            s.cancel();
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
            final U b;
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                cancel();
                actual.onError(e);
                return;
            }
            
            if (b == null) {
                cancel();
                actual.onError(new NullPointerException("The supplied buffer is null"));
                return;
            }
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                buffers.add(b);
            }
            
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    synchronized (BufferSkipBoundedSubscriber.this) {
                        buffers.remove(b);
                    }
                    
                    fastpathOrderedEmitMax(b, false, w);
                }
            }, timespan, unit);
        }
        
        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            a.onNext(v);
            return true;
        }
    }
    
    static final class BufferExactBoundedSubscriber<T, U extends Collection<? super T>>
    extends QueueDrainSubscriber<T, U, U> implements Subscription, Runnable, Disposable {
        final Supplier<U> bufferSupplier;
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

        public BufferExactBoundedSubscriber(
                Subscriber<? super U> actual,
                Supplier<U> bufferSupplier,
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
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            U b;

            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                w.dispose();
                s.cancel();
                EmptySubscription.error(e, actual);
                return;
            }
            
            if (b == null) {
                w.dispose();
                s.cancel();
                EmptySubscription.error(new NullPointerException("The supplied buffer is null"), actual);
                return;
            }
            
            buffer = b;
            
            actual.onSubscribe(this);

            s.request(Long.MAX_VALUE);

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
            
            fastpathOrderedEmitMax(b, false, this);
            
            try {
                b = bufferSupplier.get();
            } catch (Throwable e) {
                cancel();
                actual.onError(e);
                return;
            }
            
            if (b == null) {
                cancel();
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
                QueueDrainHelper.drainMaxLoop(queue, actual, false, this, this);
            }
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
            w.dispose();
            synchronized (this) {
                buffer = null;
            }
            s.cancel();
        }
        
        
        @Override
        public void run() {
            U next;
            
            try {
                next = bufferSupplier.get();
            } catch (Throwable e) {
                cancel();
                actual.onError(e);
                return;
            }
            
            if (next == null) {
                cancel();
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

            fastpathOrderedEmitMax(current, false, this);
        }
    }
}
