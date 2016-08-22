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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.Scheduler;
import io.reactivex.Scheduler.Worker;
import io.reactivex.exceptions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableObserveOn<T> extends AbstractFlowableWithUpstream<T, T> {
final Scheduler scheduler;
    
    final boolean delayError;
    
    final int prefetch;
    
    public FlowableObserveOn(
            Publisher<T> source, 
            Scheduler scheduler, 
            boolean delayError,
            int prefetch) {
        super(source);
        if (prefetch <= 0) {
            throw new IllegalArgumentException("prefetch > 0 required but it was " + prefetch);
        }
        this.scheduler = ObjectHelper.requireNonNull(scheduler, "scheduler");
        this.delayError = delayError;
        this.prefetch = prefetch;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {

// FIXME add macro-optimization
//        if (PublisherSubscribeOnValue.scalarScheduleOn(source, s, scheduler)) {
//            return;
//        }

        Worker worker;
        
        try {
            worker = scheduler.createWorker();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }
        
        if (worker == null) {
            EmptySubscription.error(new NullPointerException("The scheduler returned a null Function"), s);
            return;
        }
        
        if (s instanceof ConditionalSubscriber) {
            ConditionalSubscriber<? super T> cs = (ConditionalSubscriber<? super T>) s;
            source.subscribe(new PublisherObserveOnConditionalSubscriber<T>(cs, worker, delayError, prefetch));
            return;
        }
        source.subscribe(new PublisherObserveOnSubscriber<T>(s, worker, delayError, prefetch));
    }

    static abstract class BaseObserveOnSubscriber<T>
    extends BasicIntQueueSubscription<T>
    implements Runnable, Subscriber<T> {
        /** */
        private static final long serialVersionUID = -8241002408341274697L;

        final Worker worker;
        
        final boolean delayError;
        
        final int prefetch;
        
        final int limit;
        
        final AtomicLong requested;
        
        Subscription s;
        
        SimpleQueue<T> queue;
        
        volatile boolean cancelled;
        
        volatile boolean done;
        
        Throwable error;

        int sourceMode;
        
        long produced;
        
        boolean outputFused;
        
        public BaseObserveOnSubscriber(
                Worker worker,
                boolean delayError,
                int prefetch) {
            this.worker = worker;
            this.delayError = delayError;
            this.prefetch = prefetch;
            this.requested = new AtomicLong();
            
            if (prefetch != Integer.MAX_VALUE) {
                this.limit = prefetch - (prefetch >> 2);
            } else {
                this.limit = Integer.MAX_VALUE;
            }
        }
        
        final void initialRequest() {
            if (prefetch == Integer.MAX_VALUE) {
                s.request(Long.MAX_VALUE);
            } else {
                s.request(prefetch);
            }
        }
        
        @Override
        public final void onNext(T t) {
            if (sourceMode == ASYNC) {
                trySchedule();
                return;
            }
            if (!queue.offer(t)) {
                s.cancel();
                
                error = new MissingBackpressureException("Queue is full?!");
                done = true;
            }
            trySchedule();
        }
        
        @Override
        public final void onError(Throwable t) {
            error = t;
            done = true;
            trySchedule();
        }
        
        @Override
        public final void onComplete() {
            done = true;
            trySchedule();
        }
        
        @Override
        public final void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                trySchedule();
            }
        }
        
        @Override
        public final void cancel() {
            if (cancelled) {
                return;
            }
            
            cancelled = true;
            s.cancel();
            worker.dispose();
            
            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }

        final void trySchedule() {
            if (getAndIncrement() != 0) {
                return;
            }
            worker.schedule(this);
        }
        
        @Override
        public final void run() {
            if (outputFused) {
                runBackfused();
            } else if (sourceMode == SYNC) {
                runSync();
            } else {
                runAsync();
            }
        }
        
        abstract void runBackfused();
        
        abstract void runSync();
        
        abstract void runAsync();
        
        final boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a) {
            if (cancelled) {
                queue.clear();
                return true;
            }
            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = error;
                        if (e != null) {
                            doError(a, e);
                        } else {
                            doComplete(a);
                        }
                        return true;
                    }
                } else {
                    Throwable e = error;
                    if (e != null) {
                        queue.clear();
                        doError(a, e);
                        return true;
                    } else
                    if (empty) {
                        doComplete(a);
                        return true;
                    }
                }
            }

            return false;
        }

        final void doComplete(Subscriber<?> a) {
            try {
                a.onComplete();
            } finally {
                worker.dispose();
            }
        }
        
        final void doError(Subscriber<?> a, Throwable e) {
            try {
                a.onError(e);
            } finally {
                worker.dispose();
            }
        }

        @Override
        public final int requestFusion(int requestedMode) {
            if ((requestedMode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Override
        public final void clear() {
            queue.clear();
        }
        
        @Override
        public final boolean isEmpty() {
            return queue.isEmpty();
        }
    }
    
    static final class PublisherObserveOnSubscriber<T> extends BaseObserveOnSubscriber<T>
    implements Subscriber<T> {
        /** */
        private static final long serialVersionUID = -4547113800637756442L;

        final Subscriber<? super T> actual;
        
        public PublisherObserveOnSubscriber(
                Subscriber<? super T> actual,
                Worker worker,
                boolean delayError,
                int prefetch) {
            super(worker, delayError, prefetch);
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> f = (QueueSubscription<T>) s;
                    
                    int m = f.requestFusion(ANY | BOUNDARY);
                    
                    if (m == SYNC) {
                        sourceMode = SYNC;
                        queue = f;
                        done = true;
                        
                        actual.onSubscribe(this);
                        return;
                    } else
                    if (m == ASYNC) {
                        sourceMode = ASYNC;
                        queue = f;
                        
                        actual.onSubscribe(this);
                        
                        initialRequest();
                        
                        return;
                    }
                }
                
                queue = new SpscArrayQueue<T>(prefetch);

                actual.onSubscribe(this);

                initialRequest();
            }
        }
        
        @Override
        void runSync() {
            int missed = 1;

            final Subscriber<? super T> a = actual;
            final SimpleQueue<T> q = queue;

            long e = produced;

            for (;;) {

                long r = requested.get();

                while (e != r) {
                    T v;

                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        doError(a, ex);
                        return;
                    }

                    if (cancelled) {
                        return;
                    }
                    if (v == null) {
                        doComplete(a);
                        return;
                    }

                    a.onNext(v);

                    e++;
                }

                if (e == r) {
                    if (cancelled) {
                        return;
                    }

                    boolean empty;

                    try {
                        empty = q.isEmpty();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        doError(a, ex);
                        return;
                    }

                    if (empty) {
                        doComplete(a);
                        return;
                    }
                }

                int w = get();
                if (missed == w) {
                    produced = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        @Override
        void runAsync() {
            int missed = 1;

            final Subscriber<? super T> a = actual;
            final SimpleQueue<T> q = queue;

            long e = produced;

            for (;;) {

                long r = requested.get();

                while (e != r) {
                    boolean d = done;
                    T v;

                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);

                        s.cancel();
                        q.clear();

                        doError(a, ex);
                        return;
                    }

                    boolean empty = v == null;

                    if (checkTerminated(d, empty, a)) {
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);

                    e++;
                    if (e == limit) {
                        if (r != Long.MAX_VALUE) {
                            r = requested.addAndGet(-e);
                        }
                        s.request(e);
                        e = 0L;
                    }
                }

                if (e == r) {
                    boolean d = done;
                    boolean empty;
                    try {
                        empty = q.isEmpty();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);

                        s.cancel();
                        q.clear();

                        doError(a, ex);
                        return;
                    }

                    if (checkTerminated(d, empty, a)) {
                        return;
                    }
                }

                int w = get();
                if (missed == w) {
                    produced = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        @Override
        void runBackfused() {
            int missed = 1;
            
            for (;;) {
                
                if (cancelled) {
                    return;
                }
                
                boolean d = done;
                
                actual.onNext(null);
                
                if (d) {
                    Throwable e = error;
                    if (e != null) {
                        doError(actual, e);
                    } else {
                        doComplete(actual);
                    }
                    return;
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public T poll() throws Exception {
            T v = queue.poll();
            if (v != null && sourceMode != SYNC) {
                long p = produced + 1;
                if (p == limit) {
                    produced = 0;
                    s.request(p);
                } else {
                    produced = p;
                }
            }
            return v;
        }
        
    }

    static final class PublisherObserveOnConditionalSubscriber<T>
    extends BaseObserveOnSubscriber<T> {
        /** */
        private static final long serialVersionUID = 644624475404284533L;

        final ConditionalSubscriber<? super T> actual;
        
        long consumed;

        public PublisherObserveOnConditionalSubscriber(
                ConditionalSubscriber<? super T> actual,
                Worker worker,
                boolean delayError,
                int prefetch) {
            super(worker, delayError, prefetch);
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> f = (QueueSubscription<T>) s;
                    
                    int m = f.requestFusion(ANY | BOUNDARY);
                    
                    if (m == SYNC) {
                        sourceMode = SYNC;
                        queue = f;
                        done = true;
                        
                        actual.onSubscribe(this);
                        return;
                    } else
                    if (m == ASYNC) {
                        sourceMode = ASYNC;
                        queue = f;
                        
                        actual.onSubscribe(this);
                        
                        initialRequest();
                        
                        return;
                    }
                }

                queue = new SpscArrayQueue<T>(prefetch);
                
                actual.onSubscribe(this);

                initialRequest();
            }
        }

        @Override
        void runSync() {
            int missed = 1;
            
            final ConditionalSubscriber<? super T> a = actual;
            final SimpleQueue<T> q = queue;

            long e = produced;

            for (;;) {
                
                long r = requested.get();
                
                while (e != r) {
                    T v;
                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        doError(a, ex);
                        return;
                    }

                    if (cancelled) {
                        return;
                    }
                    if (v == null) {
                        doComplete(a);
                        return;
                    }
                    
                    if (a.tryOnNext(v)) {
                        e++;
                    }
                }
                
                if (e == r) {
                    if (cancelled) {
                        return;
                    }
                    
                    boolean empty;
                    
                    try {
                        empty = q.isEmpty();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        doError(a, ex);
                        return;
                    }
                    
                    if (empty) {
                        doComplete(a);
                        return;
                    }
                }

                int w = get();
                if (missed == w) {
                    produced = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }
        
        @Override
        void runAsync() {
            int missed = 1;
            
            final ConditionalSubscriber<? super T> a = actual;
            final SimpleQueue<T> q = queue;
            
            long emitted = produced;
            long polled = consumed;
            
            for (;;) {
                
                long r = requested.get();
                
                while (emitted != r) {
                    boolean d = done;
                    T v;
                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);

                        s.cancel();
                        q.clear();
                        
                        doError(a, ex);
                        return;
                    }
                    boolean empty = v == null;
                    
                    if (checkTerminated(d, empty, a)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }

                    if (a.tryOnNext(v)) {
                        emitted++;
                    }
                    
                    polled++;
                    
                    if (polled == limit) {
                        s.request(polled);
                        polled = 0L;
                    }
                }
                
                if (emitted == r) {
                    boolean d = done;
                    boolean empty;
                    try {
                        empty = q.isEmpty();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);

                        s.cancel();
                        q.clear();
                        
                        doError(a, ex);
                        return;
                    }

                    if (checkTerminated(d, empty, a)) {
                        return;
                    }
                }
                
                int w = get();
                if (missed == w) {
                    produced = emitted;
                    consumed = polled;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }

        }
        
        @Override
        void runBackfused() {
            int missed = 1;
            
            for (;;) {
                
                if (cancelled) {
                    return;
                }
                
                boolean d = done;
                
                actual.onNext(null);
                
                if (d) {
                    Throwable e = error;
                    if (e != null) {
                        doError(actual, e);
                    } else {
                        doComplete(actual);
                    }
                    return;
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        @Override
        public T poll() throws Exception {
            T v = queue.poll();
            if (v != null && sourceMode != SYNC) {
                long p = consumed + 1;
                if (p == limit) {
                    consumed = 0;
                    s.request(p);
                } else {
                    consumed = p;
                }
            }
            return v;
        }
    }
}
