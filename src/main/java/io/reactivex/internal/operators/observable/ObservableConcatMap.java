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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.observers.SerializedObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableConcatMap<T, U> extends ObservableSource<T, U> {
    final Function<? super T, ? extends ObservableConsumable<? extends U>> mapper;
    final int bufferSize;
    public ObservableConcatMap(ObservableConsumable<T> source, Function<? super T, ? extends ObservableConsumable<? extends U>> mapper, int bufferSize) {
        super(source);
        this.mapper = mapper;
        this.bufferSize = Math.max(8, bufferSize);
    }
    @Override
    public void subscribeActual(Observer<? super U> s) {
        SerializedObserver<U> ssub = new SerializedObserver<U>(s);
        source.subscribe(new SourceSubscriber<T, U>(ssub, mapper, bufferSize));
    }
    
    static final class SourceSubscriber<T, U> extends AtomicInteger implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = 8828587559905699186L;
        final Observer<? super U> actual;
        final SerialDisposable sa;
        final Function<? super T, ? extends ObservableConsumable<? extends U>> mapper;
        final Observer<U> inner;
        final int bufferSize;

        SimpleQueue<T> queue;

        Disposable s;
        
        volatile boolean active;
        
        volatile boolean disposed;
        
        volatile boolean done;
        
        int fusionMode;
        
        public SourceSubscriber(Observer<? super U> actual,
                Function<? super T, ? extends ObservableConsumable<? extends U>> mapper, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.bufferSize = bufferSize;
            this.inner = new InnerSubscriber<U>(actual, this);
            this.sa = new SerialDisposable();
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                if (s instanceof QueueDisposable) {
                    @SuppressWarnings("unchecked")
                    QueueDisposable<T> qd = (QueueDisposable<T>) s;
                    
                    int m = qd.requestFusion(QueueDisposable.ANY);
                    if (m == QueueDisposable.SYNC) {
                        fusionMode = m;
                        queue = qd;
                        done = true;
                        
                        actual.onSubscribe(this);
                        
                        drain();
                        return;
                    }
                    
                    if (m == QueueDisposable.ASYNC) {
                        fusionMode = m;
                        queue = qd;

                        actual.onSubscribe(this);

                        return;
                    }
                }
                
                queue = new SpscLinkedArrayQueue<T>(bufferSize);
                
                actual.onSubscribe(this);
            }
        }
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (fusionMode == QueueDisposable.NONE && !queue.offer(t)) {
                dispose();
                actual.onError(new IllegalStateException("More values received than requested!"));
                return;
            }
            drain();
        }
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            dispose();
            actual.onError(t);
        }
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            drain();
        }
        
        void innerComplete() {
            active = false;
            drain();
        }
        
        @Override
        public boolean isDisposed() {
            return disposed;
        }
        
        @Override
        public void dispose() {
            disposed = true;
            sa.dispose();
            s.dispose();
            
            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }
        
        void innerSubscribe(Disposable s) {
            sa.set(s);
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            for (;;) {
                if (disposed) {
                    queue.clear();
                    return;
                }
                if (!active) {
                    
                    boolean d = done;
                    
                    T t;
                    
                    try {
                        t = queue.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        dispose();
                        queue.clear();
                        actual.onError(ex);
                        return;
                    }
                    
                    boolean empty = t == null;
                    
                    if (d && empty) {
                        actual.onComplete();
                        return;
                    }
                    
                    if (!empty) {
                        ObservableConsumable<? extends U> o;
                        
                        try {
                            o = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null ObservableConsumable");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            dispose();
                            queue.clear();
                            actual.onError(ex);
                            return;
                        }
                        
                        active = true;
                        o.subscribe(inner);
                    }
                }
                
                if (decrementAndGet() == 0) {
                    break;
                }
            };
        }
    }
    
    static final class InnerSubscriber<U> implements Observer<U> {
        final Observer<? super U> actual;
        final SourceSubscriber<?, ?> parent;

        public InnerSubscriber(Observer<? super U> actual, SourceSubscriber<?, ?> parent) {
            this.actual = actual;
            this.parent = parent;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            parent.innerSubscribe(s);
        }
        
        @Override
        public void onNext(U t) {
            actual.onNext(t);
        }
        @Override
        public void onError(Throwable t) {
            parent.dispose();
            actual.onError(t);
        }
        @Override
        public void onComplete() {
            parent.innerComplete();
        }
    }
}