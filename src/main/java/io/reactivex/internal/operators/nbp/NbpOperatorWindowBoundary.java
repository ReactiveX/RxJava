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

import java.util.Queue;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.nbp.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.nbp.NbpUnicastSubject;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorWindowBoundary<T, B> implements NbpOperator<NbpObservable<T>, T> {
    final NbpObservable<B> other;
    final int bufferSize;
    
    public NbpOperatorWindowBoundary(NbpObservable<B> other, int bufferSize) {
        this.other = other;
        this.bufferSize = bufferSize;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super NbpObservable<T>> t) {
        return new WindowBoundaryMainSubscriber<>(new NbpSerializedSubscriber<>(t), other, bufferSize);
    }
    
    static final class WindowBoundaryMainSubscriber<T, B> 
    extends NbpQueueDrainSubscriber<T, Object, NbpObservable<T>> 
    implements Disposable {
        
        final NbpObservable<B> other;
        final int bufferSize;
        
        Disposable s;
        
        volatile Disposable boundary;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<WindowBoundaryMainSubscriber, Disposable> BOUNDARY =
                AtomicReferenceFieldUpdater.newUpdater(WindowBoundaryMainSubscriber.class, Disposable.class, "boundary");
        
        static final Disposable CANCELLED = () -> { };
        
        NbpUnicastSubject<T> window;
        
        static final Object NEXT = new Object();
        
        volatile long windows;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<WindowBoundaryMainSubscriber> WINDOWS =
                AtomicLongFieldUpdater.newUpdater(WindowBoundaryMainSubscriber.class, "windows");
        
        public WindowBoundaryMainSubscriber(NbpSubscriber<? super NbpObservable<T>> actual, NbpObservable<B> other,
                int bufferSize) {
            super(actual, new MpscLinkedQueue<>());
            this.other = other;
            this.bufferSize = bufferSize;
            WINDOWS.lazySet(this, 1);
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            
            NbpSubscriber<? super NbpObservable<T>> a = actual;
            a.onSubscribe(this);
            
            if (cancelled) {
                return;
            }
            
            NbpUnicastSubject<T> w = NbpUnicastSubject.create(bufferSize);
            
            window = w;
            
            a.onNext(w);
            
            WindowBoundaryInnerSubscriber<T, B> inner = new WindowBoundaryInnerSubscriber<>(this);
            
            if (BOUNDARY.compareAndSet(this, null, inner)) {
                WINDOWS.getAndIncrement(this);
                other.subscribe(inner);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (fastEnter()) {
                NbpUnicastSubject<T> w = window;
                
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
                disposeBoundary();
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
                disposeBoundary();
            }

            actual.onComplete();
            
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
            }
        }
        
        void disposeBoundary() {
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
            int missed = 1;
            NbpUnicastSubject<T> w = window;
            for (;;) {
                
                for (;;) {
                    boolean d = done;
                    
                    Object o = q.poll();
                    
                    boolean empty = o == null;
                    
                    if (d && empty) {
                        disposeBoundary();
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
                            disposeBoundary();
                            return;
                        }

                        if (cancelled) {
                            continue;
                        }
                        
                        w = NbpUnicastSubject.create(bufferSize);
                        
                        WINDOWS.getAndIncrement(this);

                        window = w;
                        
                        a.onNext(w);
                        
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
        public void accept(NbpSubscriber<? super NbpObservable<T>> a, Object v) {
            // not used by this operator
        }
    }
    
    static final class WindowBoundaryInnerSubscriber<T, B> extends NbpDisposableSubscriber<B> {
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
