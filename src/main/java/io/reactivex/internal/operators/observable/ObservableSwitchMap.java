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

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableSwitchMap<T, R> extends AbstractObservableWithUpstream<T, R> {
    final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
    final int bufferSize;
    
    final boolean delayErrors;

    public ObservableSwitchMap(ObservableSource<T> source,
                               Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize,
                                       boolean delayErrors) {
        super(source);
        this.mapper = mapper;
        this.bufferSize = bufferSize;
        this.delayErrors = delayErrors;
    }
    
    @Override
    public void subscribeActual(Observer<? super R> t) {
        
        if (ObservableScalarXMap.tryScalarXMapSubscribe(source, t, mapper)) {
            return;
        }
        
        source.subscribe(new SwitchMapSubscriber<T, R>(t, mapper, bufferSize, delayErrors));
    }
    
    static final class SwitchMapSubscriber<T, R> extends AtomicInteger implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = -3491074160481096299L;
        final Observer<? super R> actual;
        final Function<? super T, ? extends ObservableSource<? extends R>> mapper;
        final int bufferSize;
        
        final boolean delayErrors;
        
        final AtomicThrowable errors;
        
        volatile boolean done;
        
        volatile boolean cancelled;
        
        Disposable s;
        
        final AtomicReference<SwitchMapInnerSubscriber<T, R>> active = new AtomicReference<SwitchMapInnerSubscriber<T, R>>();
        
        static final SwitchMapInnerSubscriber<Object, Object> CANCELLED;
        static {
            CANCELLED = new SwitchMapInnerSubscriber<Object, Object>(null, -1L, 1);
            CANCELLED.cancel();
        }
        
        volatile long unique;
        
        public SwitchMapSubscriber(Observer<? super R> actual, 
                Function<? super T, ? extends ObservableSource<? extends R>> mapper, int bufferSize,
                        boolean delayErrors) {
            this.actual = actual;
            this.mapper = mapper;
            this.bufferSize = bufferSize;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
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
            long c = unique + 1;
            unique = c;
            
            SwitchMapInnerSubscriber<T, R> inner = active.get();
            if (inner != null) {
                inner.cancel();
            }
            
            ObservableSource<? extends R> p;
            try {
                p = mapper.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.dispose();
                onError(e);
                return;
            }

            if (p == null) {
                s.dispose();
                onError(new NullPointerException("The publisher returned is null"));
                return;
            }
            
            SwitchMapInnerSubscriber<T, R> nextInner = new SwitchMapInnerSubscriber<T, R>(this, c, bufferSize);
            
            for (;;) {
                inner = active.get();
                if (inner == CANCELLED) {
                    break;
                }
                if (active.compareAndSet(inner, nextInner)) {
                    p.subscribe(nextInner);
                    break;
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done || !errors.addThrowable(t)) {
                if (!delayErrors) {
                    disposeInner();
                }
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            drain();
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            drain();
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                
                disposeInner();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @SuppressWarnings("unchecked")
        void disposeInner() {
            SwitchMapInnerSubscriber<T, R> a = active.get();
            if (a != CANCELLED) {
                a = active.getAndSet((SwitchMapInnerSubscriber<T, R>)CANCELLED);
                if (a != CANCELLED && a != null) {
                    s.dispose();
                }
            }
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final Observer<? super R> a = actual;
            
            int missing = 1;

            for (;;) {

                if (cancelled) {
                    return;
                }
                
                if (done) {
                    boolean empty = active.get() == null;
                    if (delayErrors) {
                        if (empty) {
                            Throwable ex = errors.get();
                            if (ex != null) {
                                a.onError(ex);
                            } else {
                                a.onComplete();
                            }
                            return;
                        }
                    } else {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            a.onError(errors.terminate());
                            return;
                        }
                        if (empty) {
                            a.onComplete();
                            return;
                        }
                    }
                }
                
                SwitchMapInnerSubscriber<T, R> inner = active.get();

                if (inner != null) {
                    SpscArrayQueue<R> q = inner.queue;

                    if (inner.done) {
                        boolean empty = q.isEmpty();
                        if (delayErrors) {
                            if (empty) {
                                active.compareAndSet(inner, null);
                                continue;
                            }
                        } else {
                            Throwable ex = errors.get();
                            if (ex != null) {
                                a.onError(errors.terminate());
                                return;
                            }
                            if (empty) {
                                active.compareAndSet(inner, null);
                                continue;
                            }
                        }
                    }
                    
                    boolean retry = false;
                    
                    for (;;) {
                        if (cancelled) {
                            return;
                        }
                        if (inner != active.get()) {
                            retry = true;
                            break;
                        }

                        boolean d = inner.done;
                        R v = q.poll();
                        boolean empty = v == null;

                        if (d) {
                            if (delayErrors || empty) {
                                active.compareAndSet(inner, null);
                                retry = true;
                                break;
                            }
                            
                            Throwable ex = errors.get();
                            if (ex != null) {
                                a.onError(errors.terminate());
                                return;
                            }
                        }
                        
                        if (empty) {
                            break;
                        }
                        
                        a.onNext(v);
                    }
                    
                    if (retry) {
                        continue;
                    }
                }
                
                missing = addAndGet(-missing);
                if (missing == 0) {
                    break;
                }
            }
        }
        
        void innerError(SwitchMapInnerSubscriber<T, R> inner, Throwable ex) {
            if (inner.index == unique && errors.addThrowable(ex)) {
                if (!delayErrors) {
                    s.dispose();
                }
                inner.done = true;
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }
    }
    
    static final class SwitchMapInnerSubscriber<T, R> extends AtomicReference<Disposable> implements Observer<R> {
        /** */
        private static final long serialVersionUID = 3837284832786408377L;
        final SwitchMapSubscriber<T, R> parent;
        final long index;
        final int bufferSize;
        final SpscArrayQueue<R> queue;
        
        volatile boolean done;

        public SwitchMapInnerSubscriber(SwitchMapSubscriber<T, R> parent, long index, int bufferSize) {
            this.parent = parent;
            this.index = index;
            this.bufferSize = bufferSize;
            this.queue = new SpscArrayQueue<R>(bufferSize);
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (index == parent.unique) {
                DisposableHelper.setOnce(this, s);
            } else {
                s.dispose();
            }
        }
        
        @Override
        public void onNext(R t) {
            if (index == parent.unique) {
                if (!queue.offer(t)) {
                    onError(new IllegalStateException("Queue full?!"));
                    return;
                }
                parent.drain();
            }
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(this, t);
        }
        
        @Override
        public void onComplete() {
            if (index == parent.unique) {
                done = true;
                parent.drain();
            }
        }
        
        public void cancel() {
            DisposableHelper.dispose(this);
        }
    }
}
