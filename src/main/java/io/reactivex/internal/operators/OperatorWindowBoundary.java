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

import io.reactivex.Observable;
import io.reactivex.Observable.Operator;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.UnicastSubject;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorWindowBoundary<T, B> implements Operator<Observable<T>, T> {
    final Publisher<B> other;
    final int bufferSize;
    
    public OperatorWindowBoundary(Publisher<B> other, int bufferSize) {
        this.other = other;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super Observable<T>> t) {
        return new WindowBoundaryMainSubscriber<>(new SerializedSubscriber<>(t), other, bufferSize);
    }
    
    static final class WindowBoundaryMainSubscriber<T, B> 
    extends QueueDrainSubscriber<T, Object, Observable<T>> 
    implements Subscription {
        
        final Publisher<B> other;
        final int bufferSize;
        
        Subscription s;
        
        volatile Disposable boundary;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<WindowBoundaryMainSubscriber, Disposable> BOUNDARY =
                AtomicReferenceFieldUpdater.newUpdater(WindowBoundaryMainSubscriber.class, Disposable.class, "boundary");
        
        static final Disposable CANCELLED = () -> { };
        
        UnicastSubject<T> window;
        
        static final Object NEXT = new Object();
        
        volatile long windows;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<WindowBoundaryMainSubscriber> WINDOWS =
                AtomicLongFieldUpdater.newUpdater(WindowBoundaryMainSubscriber.class, "windows");
        
        public WindowBoundaryMainSubscriber(Subscriber<? super Observable<T>> actual, Publisher<B> other,
                int bufferSize) {
            super(actual, new MpscLinkedQueue<>());
            this.other = other;
            this.bufferSize = bufferSize;
            WINDOWS.lazySet(this, 1);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            Subscriber<? super Observable<T>> a = actual;
            a.onSubscribe(this);
            
            if (cancelled) {
                return;
            }
            
            UnicastSubject<T> w = UnicastSubject.create(bufferSize);
            
            long r = requested();
            if (r != 0L) {
                a.onNext(w);
                if (r != Long.MAX_VALUE) {
                    produced(1);
                }
            } else {
                a.onError(new IllegalStateException("Could not deliver first window due to lack of requests"));
                return;
            }
            
            window = w;
            
            WindowBoundaryInnerSubscriber<T, B> inner = new WindowBoundaryInnerSubscriber<>(this);
            
            if (BOUNDARY.compareAndSet(this, null, inner)) {
                WINDOWS.getAndIncrement(this);
                s.request(Long.MAX_VALUE);
                other.subscribe(inner);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (fastEnter()) {
                UnicastSubject<T> w = window;
                
                w.onNext(t);
                
                if (leave(-1) == 0) {
                    return;
                }
            } else {
                queue.offer(NotificationLite.next(t));
                if (!enter()) {
                    return;
                }
            }
            drainLoop();
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(error);
                return;
            }
            error = t;
            done = true;
            if (enter()) {
                drainLoop();
            }
            
            if (WINDOWS.decrementAndGet(this) == 0) {
                dispose();
            }
            
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            if (enter()) {
                drainLoop();
            }
            
            if (WINDOWS.decrementAndGet(this) == 0) {
                dispose();
            }

            actual.onComplete();
            
        }
        
        @Override
        public void request(long n) {
            requested(n);
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
            }
        }
        
        void dispose() {
            Disposable d = boundary;
            if (d != CANCELLED) {
                d = BOUNDARY.getAndSet(this, CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
        
        void drainLoop() {
            final Queue<Object> q = queue;
            final Subscriber<? super Observable<T>> a = actual;
            int missed = 1;
            UnicastSubject<T> w = window;
            for (;;) {
                
                for (;;) {
                    boolean d = done;
                    
                    Object o = q.poll();
                    
                    boolean empty = o == null;
                    
                    if (d && empty) {
                        dispose();
                        Throwable e = error;
                        if (e != null) {
                            w.onError(e);
                        } else {
                            w.onComplete();
                        }
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    if (o == NEXT) {
                        w.onComplete();

                        if (WINDOWS.decrementAndGet(this) == 0) {
                            dispose();
                            return;
                        }

                        if (cancelled) {
                            continue;
                        }
                        
                        w = UnicastSubject.create(bufferSize);
                        
                        long r = requested();
                        if (r != 0L) {
                            WINDOWS.getAndIncrement(this);
                            
                            a.onNext(w);
                            if (r != Long.MAX_VALUE) {
                                produced(1);
                            }
                        } else {
                            // don't emit new windows 
                            cancelled = true;
                            a.onError(new IllegalStateException("Could not deliver new window due to lack of requests"));
                            continue;
                        }
                        
                        window = w;
                        continue;
                    }
                    
                    w.onNext(NotificationLite.getValue(o));
                }
                
                missed = leave(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }
        
        void next() {
            queue.offer(NEXT);
            if (enter()) {
                drainLoop();
            }
        }
        
        @Override
        public boolean accept(Subscriber<? super Observable<T>> a, Object v) {
            // not used by this operator
            return false;
        }
    }
    
    static final class WindowBoundaryInnerSubscriber<T, B> extends DisposableSubscriber<B> {
        final WindowBoundaryMainSubscriber<T, B> parent;
        
        boolean done;
        
        public WindowBoundaryInnerSubscriber(WindowBoundaryMainSubscriber<T, B> parent) {
            this.parent = parent;
        }
        
        @Override
        public void onNext(B t) {
            if (done) {
                return;
            }
            parent.next();
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            parent.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.onComplete();
        }
    }
}
