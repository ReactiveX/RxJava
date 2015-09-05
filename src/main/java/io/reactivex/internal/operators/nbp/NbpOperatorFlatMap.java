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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorFlatMap<T, R> implements NbpOperator<R, T> {
    final Function<? super T, ? extends NbpObservable<? extends R>> mapper;
    final boolean delayError;
    public NbpOperatorFlatMap(Function<? super T, ? extends NbpObservable<? extends R>> mapper, boolean delayError) {
        this.mapper = mapper;
        this.delayError = delayError;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super R> t) {
        return new NbpFlatMapSubscriber<>(t, mapper, delayError);
    }
    
    static final class NbpFlatMapSubscriber<T, R> extends AtomicInteger implements NbpSubscriber<T>, Disposable, BiPredicate<NbpSubscriber<? super R>, R> {
        /** */
        
        private static final long serialVersionUID = -5429656673391159123L;
        
        final NbpSubscriber<? super R> actual;
        final Function<? super T, ? extends NbpObservable<? extends R>> mapper;
        final boolean delayError;
        
        boolean emitting;
        AppendOnlyLinkedArrayList<R> queue;
        
        volatile Queue<Throwable> errors;
        
        OpenHashSet<Disposable> innerSubscribers;
        
        volatile boolean done;
        
        volatile boolean cancelled;
        
        Disposable d;

        public NbpFlatMapSubscriber(NbpSubscriber<? super R> actual,
                Function<? super T, ? extends NbpObservable<? extends R>> mapper, boolean delayError) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayError = delayError;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            this.d = d;
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T value) {
            if (done || cancelled) {
                return;
            }
            NbpObservable<? extends R> o;
            
            try {
                o = mapper.apply(value);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            
            if (o == null) {
                onError(new NullPointerException("The observable supplied is null"));
                return;
            }
            
            // TODO Scalar optimization
            
            getAndIncrement();
            
            NbpFlatMapInnerSubscriber<R> inner = new NbpFlatMapInnerSubscriber<>(this);
            add(inner);
            if (!cancelled) {
                o.subscribe(inner);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            if (done || cancelled) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            
            tryError(e);
        }
        
        @Override
        public void onComplete() {
            if (done || cancelled) {
                return;
            }
            done = true;
            
            tryTerminate();
        }
        
        void innerDone() {
            decrementAndGet();
            
            tryTerminate();
        }
        
        void innerError(Throwable e) {
            decrementAndGet();
            
            tryError(e);
        }
        
        void tryError(Throwable e) {
            Queue<Throwable> q = errors;
            if (q == null) {
                synchronized (this) {
                    if (cancelled) {
                        return;
                    }
                    q = errors;
                    if (q == null) {
                        q = new ConcurrentLinkedQueue<>();
                        errors = q;
                    }
                }
            }
            q.offer(e);
            
            synchronized (this) {
                if (emitting) {
                    return;
                }
                emitting = true;
            }
            if (delayError) {
                emitLoop(actual);
            } else {
                dispose();
                actual.onError(getError(q));
            }
        }
        
        void tryTerminate() {
            synchronized (this) {
                if (emitting) {
                    return;
                }
                emitting = true;
            }
            if (done && get() == 0) {
                this.d = null;
                Queue<Throwable> e  = errors;
                if (e != null && !e.isEmpty()) {
                    actual.onError(getError(e));
                } else {
                    actual.onComplete();
                }
                return;
            }
            
            emitLoop(actual);
        }
        
        
        @Override
        public void dispose() {
            if (!cancelled) {
                OpenHashSet<Disposable> ds;
                synchronized (this) {
                    if (cancelled) {
                        return;
                    }
                    ds = innerSubscribers;
                    innerSubscribers = null;
                    cancelled = true;
                }
                Disposable d = this.d;
                this.d = null;
                d.dispose();
                if (ds != null) {
                    ds.forEach(Disposable::dispose);
                }
            }
        }
        
        void add(NbpFlatMapInnerSubscriber<R> inner) {
            if (!cancelled) {
                synchronized (this) {
                    if (!cancelled) {
                        OpenHashSet<Disposable> ds = innerSubscribers;
                        if (ds == null) {
                            ds = new OpenHashSet<>();
                            innerSubscribers = ds;
                        }
                        ds.add(inner);
                    }
                }
            }
        }
        void remove(NbpFlatMapInnerSubscriber<R> inner) {
            if (!cancelled) {
                synchronized (this) {
                    if (!cancelled) {
                        OpenHashSet<Disposable> ds = innerSubscribers;
                        if (ds != null) {
                            ds.remove(inner);
                        }
                    }
                }
            }
        }
        
        void emit(R value) {
            synchronized (this) {
                if (emitting) {
                    AppendOnlyLinkedArrayList<R> q = queue;
                    if (q == null) {
                        q = new AppendOnlyLinkedArrayList<>(16);
                        queue = q;
                    }
                    q.add(value);
                    return;
                }
                emitting = true;
            }
            
            NbpSubscriber<? super R> a = actual;
            
            a.onNext(value);
            
            emitLoop(a);
        }
        
        void emitLoop(NbpSubscriber<? super R> a) {
            for (;;) {
                if (cancelled) {
                    return;
                }
                
                boolean d;
                
                AppendOnlyLinkedArrayList<R> q;
                synchronized (this) {
                    q = queue;
                    d = done && get() == 0;
                    if (q == null && !d) {
                        emitting = false;
                        return;
                    }
                    queue = null;
                }
                
                if (cancelled) {
                    return;
                }
                
                if (d) {
                    if (delayError) {
                        if (q == null) {
                            this.d = null;
                            Queue<Throwable> e = errors;
                            if (e == null || e.isEmpty()) {
                                a.onError(getError(e));
                            } else {
                                a.onComplete();
                            }
                            return;
                        }
                    } else {
                        Queue<Throwable> e = errors;
                        if (e == null || e.isEmpty()) {
                            dispose();
                            a.onError(getError(e));
                            return;
                        } else
                        if (q == null) {
                            this.d = null;
                            a.onComplete();
                            return;
                        }
                    }
                }
                
                if (q != null) {
                    q.forEachWhile(a, this);
                }
            }
        }
        
        Throwable getError(Queue<Throwable> e) {
            Throwable ex = null;
            while (!e.isEmpty()) {
                if (ex == null) {
                    ex = e.poll();
                } else {
                    ex.addSuppressed(e.poll());
                }
            }
            return ex;
        }
        
        @Override
        public boolean test(NbpSubscriber<? super R> t, R u) {
            t.onNext(u);
            return cancelled;
        }
    }
    
    static final Disposable DISPOSED = () -> { };
    
    static final class NbpFlatMapInnerSubscriber<R> extends AtomicReference<Disposable> implements NbpSubscriber<R>, Disposable {
        /** */
        private static final long serialVersionUID = 1911556127363824975L;
        
        final NbpFlatMapSubscriber<?, R> parent;
        
        boolean done;

        public NbpFlatMapInnerSubscriber(NbpFlatMapSubscriber<?, R> parent) {
            this.parent = parent;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            if (!compareAndSet(null, d)) {
                d.dispose();
                if (d != DISPOSED) {
                    SubscriptionHelper.reportSubscriptionSet();
                }
            }
        }
        
        @Override
        public void onNext(R value) {
            if (done) {
                return;
            }
            parent.emit(value);
        }
        
        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            parent.remove(this);
            parent.innerError(e);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.remove(this);
            parent.innerDone();
        }
        
        @Override
        public void dispose() {
            Disposable d = get();
            if (d != DISPOSED) {
                d = getAndSet(DISPOSED);
                if (d != DISPOSED && d != null) {
                    d.dispose();
                }
            }
        }
    }
}
