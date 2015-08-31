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
import io.reactivex.internal.queue.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorOnBackpressureBuffer<T> implements Operator<T, T> {
    final int bufferSize;
    final boolean unbounded;
    final boolean delayError;
    final Runnable onOverflow;
    
    public OperatorOnBackpressureBuffer(int bufferSize, boolean unbounded, boolean delayError, Runnable onOverflow) {
        this.bufferSize = bufferSize;
        this.unbounded = unbounded;
        this.delayError = delayError;
        this.onOverflow = onOverflow;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new BackpressureBufferSubscriber<>(t, bufferSize, unbounded, delayError, onOverflow);
    }
    
    static final class BackpressureBufferSubscriber<T> extends AtomicInteger implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -2514538129242366402L;
        final Subscriber<? super T> actual;
        final Queue<T> queue;
        final boolean delayError;
        final Runnable onOverflow;
        
        Subscription s;
        
        volatile boolean cancelled;
        
        volatile boolean done;
        Throwable error;
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<BackpressureBufferSubscriber> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(BackpressureBufferSubscriber.class, "requested");
        
        public BackpressureBufferSubscriber(Subscriber<? super T> actual, int bufferSize, 
                boolean unbounded, boolean delayError, Runnable onOverflow) {
            this.actual = actual;
            this.onOverflow = onOverflow;
            this.delayError = delayError;
            
            Queue<T> q;
            
            if (unbounded) {
                q = new SpscLinkedArrayQueue<>(bufferSize);
            } else {
                if (Pow2.isPowerOfTwo(bufferSize)) {
                    q = new SpscArrayQueue<>(bufferSize);
                } else {
                    q = new SpscExactArrayQueue<>(bufferSize);
                }
            }
            
            this.queue = q;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (this.s != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                s.cancel();
                IllegalStateException ex = new IllegalStateException("Buffer is full?!");
                try {
                    onOverflow.run();
                } catch (Throwable e) {
                    ex.addSuppressed(e);
                }
                onError(ex);
            }
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
        public void request(long n) {
            if (n <= 0) {
                RxJavaPlugins.onError(new IllegalArgumentException("n > required but it was " + n));
                return;
            }
            BackpressureHelper.add(REQUESTED, this, n);
            drain();
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                
                if (getAndIncrement() == 0) {
                    queue.clear();
                    s.cancel();
                }
            }
        }
        
        void drain() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                final Queue<T> q = queue;
                final Subscriber<? super T> a = actual;
                for (;;) {
                    
                    if (checkTerminated(done, q.isEmpty(), a)) {
                        return;
                    }
                    
                    long r = requested;
                    boolean unbounded = r == Long.MAX_VALUE;
                    
                    long e = 0;
                    
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
                        e--;
                    }
                    
                    if (e != 0L) {
                        if (!unbounded) {
                            REQUESTED.addAndGet(this, e);
                        }
                    }
                    
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super T> a) {
            if (cancelled) {
                s.cancel();
                queue.clear();
                return true;
            }
            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = error;
                        if (e != null) {
                            actual.onError(e);
                        } else {
                            actual.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = error;
                    if (e != null) {
                        queue.clear();
                        actual.onError(e);
                        return true;
                    } else
                    if (empty) {
                        actual.onComplete();
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
