/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.Operator;
import rx.*;
import rx.Scheduler.Worker;
import rx.exceptions.*;
import rx.functions.Action0;
import rx.subscriptions.*;

/**
 * Buffers the source into Lists with maximum size or emission duration, respecting backpressure.
 */
public final class OperatorBufferWithTimeAndSize<T> implements Operator<List<T>, T> {
    final int size;
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;
    public OperatorBufferWithTimeAndSize(long time, TimeUnit unit, int size, Scheduler scheduler) {
        this.size = size;
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    @Override
    public Subscriber<? super T> call(Subscriber<? super List<T>> child) {
        Scheduler.Worker worker = scheduler.createWorker();
        child.add(worker);
        
        final BufferSubscriber<T> bs = new BufferSubscriber<T>(child, size, time, unit, worker);
        
        child.add(bs);
        
        child.setProducer(new BufferProducer<T>(bs));
        
        return bs;
    }
    
    /** The buffering subscriber for the upstream. */
    static final class BufferSubscriber<T> extends Subscriber<T> {

        final Subscriber<? super List<T>> child;
        final int size;
        final long time;
        final TimeUnit unit;
        final Worker worker;
        
        /** The producer of the upstream. */
        Producer producer;

        /** Tracks the downstream requested amounts. */
        final AtomicLong requested;
        
        /** Tracks the upstream requested amounts. */
        final AtomicLong upstreamRequested;
        
        /** Holds onto the current timer. */
        final SerialSubscription timer;
        
        /** The buffer holding the elements or null for a replaced buffer. Guarded by this. */
        List<T> buffer;
        
        /** The current buffer identifier so timer doesn't emit an old buffer. Guarded by this. */
        long bufferId;
        
        /** Captures how much time was remaining in the last timeout, in milliseconds. Guarded by this. */
        long timeRemaining;
        /** Stores the Worker.now()-relative value where the timer should fire, in milliseconds. Guarded by this. */
        long timeScheduled;
        
        public BufferSubscriber(Subscriber<? super List<T>> child, int size,
                long time, TimeUnit unit, Worker worker) {
            this.child = child;
            this.size = size;
            this.time = time;
            this.unit = unit;
            this.worker = worker;
            this.timeRemaining = unit.toMillis(time);
            this.requested = new AtomicLong();
            this.upstreamRequested = new AtomicLong();
            this.timer = new SerialSubscription();
            this.add(timer);
        }
        
        @Override
        public void setProducer(Producer producer) {
            this.producer = producer;
        }
        
        @Override
        public void onNext(T t) {
            long ur = upstreamRequested.get();
            if (ur == 0) {
                onError(new MissingBackpressureException());
                return;
            } else
            if (ur != Long.MAX_VALUE) {
                upstreamRequested.decrementAndGet();
            }
            
            List<T> list;
            long r;
            long id;
            long delay;
            synchronized (this) {
                List<T> b = buffer;
                if (b == null) {
                    b = new ArrayList<T>();
                    buffer = b;
                }
                b.add(t);
                if (b.size() == size) {
                    id = ++bufferId;
                    
                    list = buffer;
                    buffer = null;
                    
                    r = requested.get();
                    if (r != Long.MAX_VALUE) {
                        delay = calculateNextDelay();
                        r = requested.decrementAndGet();
                    } else {
                        delay = -1; // irrelevant in unbounded mode
                    }
                } else {
                    return;
                }
            }
            scheduleTimer(r, id, delay);
            child.onNext(list);
        }
        
        /** Timeout when run in backpressure mode. */
        void timeout(long id) {
            List<T> list;
            long r;
            long delay;
            synchronized (this) {
                if (id == bufferId) {
                    list = buffer;
                    buffer = null;
                    
                    id = ++bufferId;
                    
                    if (list == null) {
                        list = new ArrayList<T>();
                    }
                    r = requested.get();
                    if (r != Long.MAX_VALUE) {
                        delay = calculateNextDelay();
                        r = requested.decrementAndGet();
                    } else {
                        delay = -1; // irrelevant in unbounded mode
                    }
                } else {
                    return;
                }
            }
            scheduleTimer(r, id, delay);
            child.onNext(list);
        }
        /** Timeout in unbounded mode. */
        void timeout() {
            List<T> list;
            synchronized (this) {
                list = buffer;
                buffer = null;
                
                ++bufferId;
                
                if (list == null) {
                    list = new ArrayList<T>();
                }
            }
            child.onNext(list);
        }
        
        void scheduleTimer(long r, long id, long delay) {
            if (r > 0 && r < Long.MAX_VALUE) {
                timer.set(worker.schedule(new TimerAction(id), delay, unit));
            }
        }
        
        /** Calculates the next delay in the unit accounting how much time was left from the previous timer. */
        long calculateNextDelay() {
            long delay = timeScheduled - worker.now();
            if (delay <= 0) {
                delay = time;
                timeScheduled = worker.now() + unit.toMillis(time);
            } else {
                timeScheduled = worker.now() + delay;
                delay = unit.convert(delay, TimeUnit.MILLISECONDS);
            }
            return delay;
        }
        
        @Override
        public void onError(Throwable e) {
            timer.unsubscribe();
            try {
                synchronized (this) {
                    buffer = null;
                    bufferId++;
                }
                requested.getAndSet(-1); // indicate to the downstream requester there won't be anything to request
    
                child.onError(e);
            } finally {
                unsubscribe();
            }
        }
        
        @Override
        public void onCompleted() {
            timer.unsubscribe();
            try {
                // either we win and emit the current buffer or the timer in which case
                // there is no point in emitting an empty buffer
                List<T> list;
                synchronized (this) {
                    list = buffer;
                    bufferId++;
                }
                requested.getAndSet(-1); // indicate to the downstream requester there won't be anything to request
                if (list != null) {
                    try {
                        child.onNext(list);
                    } catch (Throwable t) {
                        Exceptions.throwIfFatal(t);
                        child.onError(t);
                        return;
                    }
                }
                child.onCompleted();
            } finally {
                unsubscribe();
            }
        }
        public void downstreamRequest(long n) {
            if (n < 0) {
                throw new IllegalArgumentException("Request is negative");
            }
            if (n == 0) {
                return;
            }
            for (;;) {
                long r = requested.get();
                if (r < 0) {
                    return;
                }
                long u = r + n;
                if (u < 0) {
                    u = Long.MAX_VALUE;
                }
                if (requested.compareAndSet(r, u)) {
                    handleRequested(r, n);
                    return;
                }
            }
        }
        /**
         * Handles the change in the request amount.
         * @param before the value before the request
         * @param request the requested amount
         */
        void handleRequested(long before, long request) {
            long s = size;
            long elements = request * s;
            // s != 0 and request != 0
            if ((request >>> 31) != 0 && (elements / request != s)) {
                elements = Long.MAX_VALUE;
            }
            if (before == 0) {
                if (request != Long.MAX_VALUE) {
                    long id;
                    long delay;
                    
                    synchronized (this) {
                        id = bufferId;
                        delay = calculateNextDelay();
                    }
                    
                    timer.set(worker.schedule(new TimerAction(id), delay, unit));
                } else {
                    timer.set(worker.schedulePeriodically(new PeriodicAction(), time, time, unit));
                }
            }
            for (;;) {
                long r2 = upstreamRequested.get();
                long u2 = r2 + elements;
                if (u2 < 0) {
                    u2 = Long.MAX_VALUE;
                }
                if (upstreamRequested.compareAndSet(r2, u2)) {
                    break;
                }
            }
            
            Producer p = producer;
            if (p != null) {
                p.request(elements);
            }
        }
        /**
         * The timer action trying to emit the buffer contents.
         */
        class TimerAction implements Action0 {
            final long id;
            private TimerAction(long id) {
                this.id = id;
            }
            @Override
            public void call() {
                timeout(id);
            }
        }
        /**
         * The timer action trying to emit the buffer contents.
         */
        class PeriodicAction implements Action0 {
            @Override
            public void call() {
                timeout();
            }
        }
    }
    
    /**
     * The producer forwarding request calls to a BufferSubscriber.
     *
     * @param <T> the emitted value type
     */
    static final class BufferProducer<T> implements Producer {
        final BufferSubscriber<T> bs;
        public BufferProducer(BufferSubscriber<T> bs) {
            this.bs = bs;
        }
        @Override
        public void request(long n) {
            bs.downstreamRequest(n);
        }
    }
}
