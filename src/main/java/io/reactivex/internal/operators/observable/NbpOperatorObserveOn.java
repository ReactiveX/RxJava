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

package io.reactivex.internal.operators.observable;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorObserveOn<T> implements NbpOperator<T, T> {
    final Scheduler scheduler;
    final boolean delayError;
    final int bufferSize;
    public NbpOperatorObserveOn(Scheduler scheduler, boolean delayError, int bufferSize) {
        this.scheduler = scheduler;
        this.delayError = delayError;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super T> t) {
        if (scheduler instanceof TrampolineScheduler) {
            return t;
        }
        
        Scheduler.Worker w = scheduler.createWorker();
        
        return new ObserveOnSubscriber<T>(t, w, delayError, bufferSize);
    }
    
    /**
     * Pads the base atomic integer used for wip counting.
     */
    static class Padding0 extends AtomicInteger {
        /** */
        private static final long serialVersionUID = 3172843496016154809L;
        
        volatile long p01, p02, p03, p04, p05, p06, p07;
        volatile long p08, p09, p0A, p0B, p0C, p0D, p0E, p0F;
    }
    
    static final class ObserveOnSubscriber<T> extends Padding0 implements Observer<T>, Disposable, Runnable {
        /** */
        private static final long serialVersionUID = 6576896619930983584L;
        final Observer<? super T> actual;
        final Scheduler.Worker worker;
        final boolean delayError;
        final int bufferSize;
        final Queue<T> queue;
        
        Disposable s;
        
        Throwable error;
        volatile boolean done;
        
        volatile boolean cancelled;
        
        public ObserveOnSubscriber(Observer<? super T> actual, Scheduler.Worker worker, boolean delayError, int bufferSize) {
            this.actual = actual;
            this.worker = worker;
            this.delayError = delayError;
            this.bufferSize = bufferSize;
            this.queue = new SpscLinkedArrayQueue<T>(bufferSize);
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            
            if (!queue.offer(t)) {
                s.dispose();
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
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                s.dispose();
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
            final Observer<? super T> a = actual;
            
            for (;;) {
                if (checkTerminated(done, q.isEmpty(), a)) {
                    return;
                }
                
                for (;;) {
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
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Observer<? super T> a) {
            if (cancelled) {
                s.dispose();
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
