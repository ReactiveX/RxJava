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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public enum OperatorOnBackpressureLatest implements Operator<Object, Object> {
    INSTANCE;
    
    @SuppressWarnings("unchecked")
    public static <T> Operator<T, T> instance() {
        return (Operator<T, T>)INSTANCE;
    }
    
    @Override
    public Subscriber<? super Object> apply(Subscriber<? super Object> t) {
        return new BackpressureLatestSubscriber(t);
    }
    
    static final class BackpressureLatestSubscriber extends AtomicInteger implements Subscriber<Object>, Subscription {
        /** */
        private static final long serialVersionUID = 163080509307634843L;

        final Subscriber<? super Object> actual;
        
        Subscription s;
        
        volatile boolean done;
        Throwable error;
        
        volatile boolean cancelled;
        
        // TODO contended padding?
        volatile long requested;
        static final AtomicLongFieldUpdater<BackpressureLatestSubscriber> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(BackpressureLatestSubscriber.class, "requested");
        
        // TODO contended padding?
        volatile Object current;
        static final AtomicReferenceFieldUpdater<BackpressureLatestSubscriber, Object> CURRENT =
                AtomicReferenceFieldUpdater.newUpdater(BackpressureLatestSubscriber.class, Object.class, "current");

        public BackpressureLatestSubscriber(Subscriber<? super Object> actual) {
            this.actual = actual;
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
        public void onNext(Object t) {
            CURRENT.lazySet(this, t);
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
                    CURRENT.lazySet(this, null);
                    s.cancel();
                }
            }
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            final Subscriber<? super Object> a = actual;
            int missed = 1;
            for (;;) {
                
                if (checkTerminated(done, current == null, a)) {
                    return;
                }
                
                long r = requested;
                
                while (r != 0L) {
                    boolean d = done;
                    Object v = CURRENT.getAndSet(this, null);
                    boolean empty = v == null;
                    
                    if (checkTerminated(d, empty, a)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    a.onNext(v);
                    
                    if (r != Long.MAX_VALUE) {
                        REQUESTED.decrementAndGet(this);
                    }
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super Object> a) {
            if (cancelled) {
                CURRENT.lazySet(this, null);
                s.cancel();
                return true;
            }
            
            if (d) {
                Throwable e = error;
                if (e != null) {
                    CURRENT.lazySet(this, null);
                    a.onError(e);
                    return true;
                } else
                if (empty) {
                    a.onComplete();
                    return true;
                }
            }
            
            return false;
        }
    }
}
