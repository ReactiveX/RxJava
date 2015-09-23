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

package io.reactivex.internal.operators.nbp;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SetCompositeResource;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.nbp.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.nbp.NbpUnicastSubject;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorWindowBoundarySelector<T, B, V> implements NbpOperator<NbpObservable<T>, T> {
    final NbpObservable<B> open;
    final Function<? super B, ? extends NbpObservable<V>> close;
    final int bufferSize;
    
    public NbpOperatorWindowBoundarySelector(NbpObservable<B> open, Function<? super B, ? extends NbpObservable<V>> close,
            int bufferSize) {
        this.open = open;
        this.close = close;
        this.bufferSize = bufferSize;
    }

    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super NbpObservable<T>> t) {
        return new WindowBoundaryMainSubscriber<>(
                new NbpSerializedSubscriber<>(t), 
                open, close, bufferSize);
    }
    
    static final class WindowBoundaryMainSubscriber<T, B, V>
    extends NbpQueueDrainSubscriber<T, Object, NbpObservable<T>>
    implements Disposable {
        final NbpObservable<B> open;
        final Function<? super B, ? extends NbpObservable<V>> close;
        final int bufferSize;
        final SetCompositeResource<Disposable> resources;

        Disposable s;
        
        volatile Disposable boundary;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<WindowBoundaryMainSubscriber, Disposable> BOUNDARY =
                AtomicReferenceFieldUpdater.newUpdater(WindowBoundaryMainSubscriber.class, Disposable.class, "boundary");
        
        static final Disposable CANCELLED = () -> { };
        
        final List<NbpUnicastSubject<T>> ws;
        
        volatile long windows;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<WindowBoundaryMainSubscriber> WINDOWS =
                AtomicLongFieldUpdater.newUpdater(WindowBoundaryMainSubscriber.class, "windows");

        public WindowBoundaryMainSubscriber(NbpSubscriber<? super NbpObservable<T>> actual,
                NbpObservable<B> open, Function<? super B, ? extends NbpObservable<V>> close, int bufferSize) {
            super(actual, new MpscLinkedQueue<>());
            this.open = open;
            this.close = close;
            this.bufferSize = bufferSize;
            this.resources = new SetCompositeResource<>(Disposable::dispose);
            this.ws = new ArrayList<>();
            WINDOWS.lazySet(this, 1);
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
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
                open.subscribe(os);
            }
            
        }
        
        @Override
        public void onNext(T t) {
            if (fastEnter()) {
                for (NbpUnicastSubject<T> w : ws) {
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
                s.dispose();
                resources.dispose();
            }
            
            actual.onComplete();
        }
        
        void error(Throwable t) {
            if (WINDOWS.decrementAndGet(this) == 0) {
                s.dispose();
                resources.dispose();
            }
            
            actual.onError(t);
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
            }
        }
        
        void disposeBoundary() {
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
            final NbpSubscriber<? super NbpObservable<T>> a = actual;
            final List<NbpUnicastSubject<T>> ws = this.ws;
            int missed = 1;
            
            for (;;) {
                
                for (;;) {
                    boolean d = done;
                    Object o = q.poll();
                    
                    boolean empty = o == null;
                    
                    if (d && empty) {
                        disposeBoundary();
                        Throwable e = error;
                        if (e != null) {
                            for (NbpUnicastSubject<T> w : ws) {
                                w.onError(e);
                            }
                        } else {
                            for (NbpUnicastSubject<T> w : ws) {
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
                        
                        NbpUnicastSubject<T> w = wo.w;
                        if (w != null) {
                            if (ws.remove(wo.w)) {
                                wo.w.onComplete();
                                
                                if (WINDOWS.decrementAndGet(this) == 0) {
                                    disposeBoundary();
                                    return;
                                }
                            }
                            continue;
                        }
                        
                        if (cancelled) {
                            continue;
                        }
                        

                        w = NbpUnicastSubject.create(bufferSize);
                        
                        ws.add(w);
                        a.onNext(w);
                        
                        NbpObservable<V> p;
                        
                        try {
                            p = close.apply(wo.open);
                        } catch (Throwable e) {
                            cancelled = true;
                            a.onError(e);
                            continue;
                        }
                        
                        if (p == null) {
                            cancelled = true;
                            a.onError(new NullPointerException("The NbpObservable supplied is null"));
                            continue;
                        }
                        
                        OperatorWindowBoundaryCloseSubscriber<T, V> cl = new OperatorWindowBoundaryCloseSubscriber<>(this, w);
                        
                        if (resources.add(cl)) {
                            WINDOWS.getAndIncrement(this);
                            
                            p.subscribe(cl);
                        }
                        
                        continue;
                    }
                    
                    for (NbpUnicastSubject<T> w : ws) {
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
        public void accept(NbpSubscriber<? super NbpObservable<T>> a, Object v) {
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
        final NbpUnicastSubject<T> w;
        final B open;
        public WindowOperation(NbpUnicastSubject<T> w, B open) {
            this.w = w;
            this.open = open;
        }
    }
    
    static final class OperatorWindowBoundaryOpenSubscriber<T, B> extends NbpDisposableSubscriber<B> {
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
    
    static final class OperatorWindowBoundaryCloseSubscriber<T, V> extends NbpDisposableSubscriber<V> {
        final WindowBoundaryMainSubscriber<T, ?, V> parent;
        final NbpUnicastSubject<T> w;
        
        boolean done;

        public OperatorWindowBoundaryCloseSubscriber(WindowBoundaryMainSubscriber<T, ?, V> parent, NbpUnicastSubject<T> w) {
            this.parent = parent;
            this.w = w;
        }
        
        @Override
        public void onNext(V t) {
            if (done) {
                return;
            }
            done = true;
            dispose();
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
