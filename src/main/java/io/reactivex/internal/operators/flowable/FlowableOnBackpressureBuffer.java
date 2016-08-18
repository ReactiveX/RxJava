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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.Action;
import io.reactivex.internal.fuseable.SimpleQueue;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableOnBackpressureBuffer<T> extends AbstractFlowableWithUpstream<T, T> {
    final int bufferSize;
    final boolean unbounded;
    final boolean delayError;
    final Action onOverflow;
    
    public FlowableOnBackpressureBuffer(Publisher<T> source, int bufferSize, boolean unbounded, 
            boolean delayError, Action onOverflow) {
        super(source);
        this.bufferSize = bufferSize;
        this.unbounded = unbounded;
        this.delayError = delayError;
        this.onOverflow = onOverflow;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new BackpressureBufferSubscriber<T>(s, bufferSize, unbounded, delayError, onOverflow));
    }
    
    static final class BackpressureBufferSubscriber<T> extends AtomicInteger implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -2514538129242366402L;
        final Subscriber<? super T> actual;
        final SimpleQueue<T> queue;
        final boolean delayError;
        final Action onOverflow;
        
        Subscription s;
        
        volatile boolean cancelled;
        
        volatile boolean done;
        Throwable error;
        
        final AtomicLong requested = new AtomicLong();
        
        public BackpressureBufferSubscriber(Subscriber<? super T> actual, int bufferSize, 
                boolean unbounded, boolean delayError, Action onOverflow) {
            this.actual = actual;
            this.onOverflow = onOverflow;
            this.delayError = delayError;
            
            SimpleQueue<T> q;
            
            if (unbounded) {
                q = new SpscLinkedArrayQueue<T>(bufferSize);
            } else {
                q = new SpscArrayQueue<T>(bufferSize);
            }
            
            this.queue = q;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                s.cancel();
                MissingBackpressureException ex = new MissingBackpressureException("Buffer is full");
                try {
                    onOverflow.run();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    ex.initCause(e);
                }
                onError(ex);
                return;
            }
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
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
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
                final SimpleQueue<T> q = queue;
                final Subscriber<? super T> a = actual;
                for (;;) {
                    
                    if (checkTerminated(done, q.isEmpty(), a)) {
                        return;
                    }
                    
                    long r = requested.get();
                    
                    long e = 0L;
                    
                    while (e != r) {
                        boolean d = done;
                        T v;
                        
                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            s.cancel();
                            a.onError(ex);
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
                    }
                    
                    if (e != 0L) {
                        if (r != Long.MAX_VALUE) {
                            requested.addAndGet(-e);
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
