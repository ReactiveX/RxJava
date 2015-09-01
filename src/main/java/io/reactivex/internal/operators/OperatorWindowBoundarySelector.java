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

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.Observable;
import io.reactivex.Observable.Operator;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SetCompositeResource;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.UnicastSubject;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorWindowBoundarySelector<T, B, V> implements Operator<Observable<T>, T> {
    final Publisher<B> open;
    final Function<? super B, ? extends Publisher<V>> close;
    final int bufferSize;
    
    public OperatorWindowBoundarySelector(Publisher<B> open, Function<? super B, ? extends Publisher<V>> close,
            int bufferSize) {
        this.open = open;
        this.close = close;
        this.bufferSize = bufferSize;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super Observable<T>> t) {
        return new WindowBoundaryMainSubscriber<>(
                new SerializedSubscriber<>(t), 
                open, close, bufferSize);
    }
    
    static final class WindowBoundaryMainSubscriber<T, B, V>
    extends QueueDrainSubscriber<T, Object, Observable<T>>
    implements Subscription {
        final Publisher<B> open;
        final Function<? super B, ? extends Publisher<V>> close;
        final int bufferSize;
        final SetCompositeResource<Disposable> resources;

        Subscription s;
        
        volatile Disposable boundary;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<WindowBoundaryMainSubscriber, Disposable> BOUNDARY =
                AtomicReferenceFieldUpdater.newUpdater(WindowBoundaryMainSubscriber.class, Disposable.class, "boundary");
        
        static final Disposable CANCELLED = () -> { };
        
        final List<UnicastSubject<T>> ws;
        
        volatile long windows;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<WindowBoundaryMainSubscriber> WINDOWS =
                AtomicLongFieldUpdater.newUpdater(WindowBoundaryMainSubscriber.class, "windows");

        public WindowBoundaryMainSubscriber(Subscriber<? super Observable<T>> actual,
                Publisher<B> open, Function<? super B, ? extends Publisher<V>> close, int bufferSize) {
            super(actual, new MpscLinkedQueue<>());
            this.open = open;
            this.close = close;
            this.bufferSize = bufferSize;
            this.resources = new SetCompositeResource<>(Disposable::dispose);
            this.ws = new ArrayList<>();
            WINDOWS.lazySet(this, 1);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            
            this.s = s;
            
            actual.onSubscribe(this);
            
            if (cancelled) {
                return;
            }
            
            OperatorWindowBoundaryOpenSubscriber<T, B> os = new OperatorWindowBoundaryOpenSubscriber<>(this);
            
            if (BOUNDARY.compareAndSet(this, null, os)) {
                WINDOWS.getAndIncrement(this);
                s.request(Long.MAX_VALUE);
                open.subscribe(os);
            }
            
        }
        
        @Override
        public void onNext(T t) {
            if (fastEnter()) {
                for (UnicastSubject<T> w : ws) {
                    w.onNext(t);
                }
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
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            
            if (enter()) {
                drainLoop();
            }
            
            if (WINDOWS.decrementAndGet(this) == 0) {
                resources.dispose();
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
                resources.dispose();
            }
            
            actual.onComplete();
        }
        
        
        
        void complete() {
            if (WINDOWS.decrementAndGet(this) == 0) {
                s.cancel();
                resources.dispose();
            }
            
            actual.onComplete();
        }
        
        void error(Throwable t) {
            if (WINDOWS.decrementAndGet(this) == 0) {
                s.cancel();
                resources.dispose();
            }
            
            actual.onError(t);
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
            resources.dispose();
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
            final List<UnicastSubject<T>> ws = this.ws;
            int missed = 1;
            
            for (;;) {
                
                for (;;) {
                    boolean d = done;
                    Object o = q.poll();
                    
                    boolean empty = o == null;
                    
                    if (d && empty) {
                        dispose();
                        Throwable e = error;
                        if (e != null) {
                            for (UnicastSubject<T> w : ws) {
                                w.onError(e);
                            }
                        } else {
                            for (UnicastSubject<T> w : ws) {
                                w.onComplete();
                            }
                        }
                        ws.clear();
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    if (o instanceof WindowOperation) {
                        @SuppressWarnings("unchecked")
                        WindowOperation<T, B> wo = (WindowOperation<T, B>) o;
                        
                        UnicastSubject<T> w = wo.w;
                        if (w != null) {
                            if (ws.remove(wo.w)) {
                                wo.w.onComplete();
                                
                                if (WINDOWS.decrementAndGet(this) == 0) {
                                    dispose();
                                    return;
                                }
                            }
                            continue;
                        }
                        
                        if (cancelled) {
                            continue;
                        }
                        

                        w = UnicastSubject.create(bufferSize);
                        
                        long r = requested();
                        if (r != 0L) {
                            ws.add(w);
                            a.onNext(w);
                            if (r != Long.MAX_VALUE) {
                                produced(1);
                            }
                        } else {
                            cancelled = true;
                            a.onError(new IllegalStateException("Could not deliver new window due to lack of requests"));
                            continue;
                        }
                        
                        Publisher<V> p;
                        
                        try {
                            p = close.apply(wo.open);
                        } catch (Throwable e) {
                            cancelled = true;
                            a.onError(e);
                            continue;
                        }
                        
                        if (p == null) {
                            cancelled = true;
                            a.onError(new NullPointerException("The publisher supplied is null"));
                            continue;
                        }
                        
                        OperatorWindowBoundaryCloseSubscriber<T, V> cl = new OperatorWindowBoundaryCloseSubscriber<>(this, w);
                        
                        if (resources.add(cl)) {
                            WINDOWS.getAndIncrement(this);
                            
                            p.subscribe(cl);
                        }
                        
                        continue;
                    }
                    
                    for (UnicastSubject<T> w : ws) {
                        w.onNext(NotificationLite.getValue(o));
                    }
                }
                
                missed = leave(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        @Override
        public boolean accept(Subscriber<? super Observable<T>> a, Object v) {
            // not used by this operator
            return false;
        }
        
        void open(B b) {
            queue.offer(new WindowOperation<>(null, b));
            if (enter()) {
                drainLoop();
            }
        }
        
        void close(OperatorWindowBoundaryCloseSubscriber<T, V> w) {
            resources.delete(w);
            queue.offer(new WindowOperation<>(w.w, null));
            if (enter()) {
                drainLoop();
            }
        }
    }
    
    static final class WindowOperation<T, B> {
        final UnicastSubject<T> w;
        final B open;
        public WindowOperation(UnicastSubject<T> w, B open) {
            this.w = w;
            this.open = open;
        }
    }
    
    static final class OperatorWindowBoundaryOpenSubscriber<T, B> extends DisposableSubscriber<B> {
        final WindowBoundaryMainSubscriber<T, B, ?> parent;
        
        boolean done;

        public OperatorWindowBoundaryOpenSubscriber(WindowBoundaryMainSubscriber<T, B, ?> parent) {
            this.parent = parent;
        }
        
        @Override
        public void onNext(B t) {
            if (done) {
                return;
            }
            parent.open(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            parent.error(t);
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
    
    static final class OperatorWindowBoundaryCloseSubscriber<T, V> extends DisposableSubscriber<V> {
        final WindowBoundaryMainSubscriber<T, ?, V> parent;
        final UnicastSubject<T> w;
        
        boolean done;

        public OperatorWindowBoundaryCloseSubscriber(WindowBoundaryMainSubscriber<T, ?, V> parent, UnicastSubject<T> w) {
            this.parent = parent;
            this.w = w;
        }
        
        @Override
        public void onNext(V t) {
            if (done) {
                return;
            }
            done = true;
            cancel();
            parent.close(this);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            parent.error(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.close(this);
        }
    }
}
