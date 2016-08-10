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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;

public final class ObservableSkipLastTimed<T> extends AbstractObservableWithUpstream<T, T> {
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;
    final int bufferSize;
    final boolean delayError;

    public ObservableSkipLastTimed(ObservableSource<T> source,
            long time, TimeUnit unit, Scheduler scheduler, int bufferSize, boolean delayError) {
        super(source);
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }
    
    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new SkipLastTimedSubscriber<T>(t, time, unit, scheduler, bufferSize, delayError));
    }
    
    static final class SkipLastTimedSubscriber<T> extends AtomicInteger implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = -5677354903406201275L;
        final Observer<? super T> actual;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;
        final SpscLinkedArrayQueue<Object> queue;
        final boolean delayError;
        
        Disposable s;
        
        volatile boolean cancelled;
        
        volatile boolean done;
        Throwable error;

        public SkipLastTimedSubscriber(Observer<? super T> actual, long time, TimeUnit unit, Scheduler scheduler, int bufferSize, boolean delayError) {
            this.actual = actual;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
            this.queue = new SpscLinkedArrayQueue<Object>(bufferSize);
            this.delayError = delayError;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onNext(T t) {
            final SpscLinkedArrayQueue<Object> q = queue;

            long now = scheduler.now(unit);
            
            q.offer(now, t);

            drain();
        }
        
        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            drain();
        }
        
        @Override
        public void onComplete() {
            done = true;
            drain();
        }
        
        @Override
        public void dispose() {
            if (cancelled) {
                cancelled = true;
                
                if (getAndIncrement() == 0) {
                    queue.clear();
                    s.dispose();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            
            final Observer<? super T> a = actual;
            final SpscLinkedArrayQueue<Object> q = queue;
            final boolean delayError = this.delayError;
            final TimeUnit unit = this.unit;
            final Scheduler scheduler = this.scheduler;
            final long time = this.time;
            
            for (;;) {
                
                if (checkTerminated(done, q.isEmpty(), a, delayError)) {
                    return;
                }
                
                for (;;) {
                    boolean d = done;
                    
                    Long ts = (Long)q.peek();
                    
                    boolean empty = ts == null;

                    long now = scheduler.now(unit);
                    
                    if (!empty && ts > now - time) {
                        empty = true;
                    }
                    
                    if (checkTerminated(d, empty, a, delayError)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    if (ts > now - time) {
                        // not old enough
                        break;
                    }

                    q.poll();
                    @SuppressWarnings("unchecked")
                    T v = (T)q.poll();
                    
                    a.onNext(v);
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Observer<? super T> a, boolean delayError) {
            if (cancelled) {
                queue.clear();
                s.dispose();
                return true;
            }
            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = error;
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = error;
                    if (e != null) {
                        queue.clear();
                        a.onError(e);
                        return true;
                    } else
                    if (empty) {
                        a.onComplete();
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
