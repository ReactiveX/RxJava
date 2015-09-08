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

package io.reactivex.internal.operators;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.Scheduler;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorObserveOn<T> implements Operator<T, T> {
    final Scheduler scheduler;
    final boolean delayError;
    final int bufferSize;
    public OperatorObserveOn(Scheduler scheduler, boolean delayError, int bufferSize) {
        this.scheduler = scheduler;
        this.delayError = delayError;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        if (scheduler instanceof TrampolineScheduler) {
            return t;
        }
        
        Scheduler.Worker w = scheduler.createWorker();
        
        return new ObserveOnSubscriber<>(t, w, delayError, bufferSize);
    }
    
    /**
     * Pads the base atomic integer used for wip counting.
     */
    static class Padding0 extends AtomicInteger {
        /** */
        private static final long serialVersionUID = 3172843496016154809L;
        
        volatile long p01, p02, p03, p04, p05, p06, p07;
    }
    
    /**
     * Contains the requested amount.
     */
    static class Padding1 extends Padding0 {
        /** */
        private static final long serialVersionUID = 7659422588548271214L;
        
        volatile long requested;
        static final AtomicLongFieldUpdater<Padding1> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(Padding1.class, "requested");
        
    }
    
    /**
     * Pads the requested amount away from the effectively constant fields
     */
    static class Padding2 extends Padding1 {
        /** */
        private static final long serialVersionUID = 227348361328175380L;
        volatile long p11, p12, p13, p14, p15, p16, p17;
    }
    
    static final class ObserveOnSubscriber<T> extends Padding2 implements Subscriber<T>, Subscription, Runnable {
        /** */
        private static final long serialVersionUID = 6576896619930983584L;
        final Subscriber<? super T> actual;
        final Scheduler.Worker worker;
        final boolean delayError;
        final int bufferSize;
        final Queue<T> queue;
        
        Subscription s;
        
        Throwable error;
        volatile boolean done;
        
        volatile boolean cancelled;
        
        public ObserveOnSubscriber(Subscriber<? super T> actual, Scheduler.Worker worker, boolean delayError, int bufferSize) {
            this.actual = actual;
            this.worker = worker;
            this.delayError = delayError;
            this.bufferSize = bufferSize;
            Queue<T> q;
            if (Pow2.isPowerOfTwo(bufferSize)) {
                q = new SpscArrayQueue<>(bufferSize);
            } else {
                q = new SpscExactArrayQueue<>(bufferSize);
            }
            this.queue = q;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            s.request(bufferSize);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            
            if (!queue.offer(t)) {
                s.cancel();
                onError(new MissingBackpressureException("Queue full?!"));
                return;
            }
            schedule();
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            schedule();
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            schedule();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(REQUESTED, this, n);
            schedule();
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                s.cancel();
                worker.dispose();
            }
        }
        
        void schedule() {
            if (getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }
        
        @Override
        public void run() {
            int missed = 1;
            
            final Queue<T> q = queue;
            final Subscriber<? super T> a = actual;
            
            for (;;) {
                if (checkTerminated(done, q.isEmpty(), a)) {
                    return;
                }
                
                long r = requested;
                long e = 0L;
                boolean unbounded = r == Long.MAX_VALUE;
                
                while (r != 0L) {
                    boolean d = done;
                    T v = q.poll();
                    boolean empty = v == null;

                    if (checkTerminated(d, empty, a)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    a.onNext(v);
                    
                    r--;
                    e++;
                }
                
                if (cancelled) {
                    return;
                }
                if (e != 0L) {
                    if (!unbounded) {
                        REQUESTED.addAndGet(this, -e);
                    }
                    s.request(e);
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super T> a) {
            if (cancelled) {
                s.cancel();
                worker.dispose();
                return true;
            }
            if (d) {
                Throwable e = error;
                if (delayError) {
                    if (empty) {
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        worker.dispose();
                        return true;
                    }
                } else {
                    if (e != null) {
                        a.onError(e);
                        worker.dispose();
                        return true;
                    } else
                    if (empty) {
                        a.onComplete();
                        worker.dispose();
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
