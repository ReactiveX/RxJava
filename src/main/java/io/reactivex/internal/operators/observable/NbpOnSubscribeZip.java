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

import java.util.Queue;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOnSubscribeZip<T, R> implements NbpOnSubscribe<R> {
    
    final Observable<? extends T>[] sources;
    final Iterable<? extends Observable<? extends T>> sourcesIterable;
    final Function<? super Object[], ? extends R> zipper;
    final int bufferSize;
    final boolean delayError;
    
    public NbpOnSubscribeZip(Observable<? extends T>[] sources,
            Iterable<? extends Observable<? extends T>> sourcesIterable,
            Function<? super Object[], ? extends R> zipper,
            int bufferSize,
            boolean delayError) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.zipper = zipper;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void accept(Observer<? super R> s) {
        Observable<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Observable[8];
            for (Observable<? extends T> p : sourcesIterable) {
                if (count == sources.length) {
                    Observable<? extends T>[] b = new Observable[count + (count >> 2)];
                    System.arraycopy(sources, 0, b, 0, count);
                    sources = b;
                }
                sources[count++] = p;
            }
        } else {
            count = sources.length;
        }
        
        if (count == 0) {
            EmptyDisposable.complete(s);
            return;
        }
        
        ZipCoordinator<T, R> zc = new ZipCoordinator<T, R>(s, zipper, count, delayError);
        zc.subscribe(sources, bufferSize);
    }
    
    static final class ZipCoordinator<T, R> extends AtomicInteger implements Disposable {
        /** */
        private static final long serialVersionUID = 2983708048395377667L;
        final Observer<? super R> actual;
        final Function<? super Object[], ? extends R> zipper;
        final ZipSubscriber<T, R>[] subscribers;
        final Object[] row;
        final boolean delayError;
        
        volatile boolean cancelled;
        
        @SuppressWarnings("unchecked")
        public ZipCoordinator(Observer<? super R> actual, 
                Function<? super Object[], ? extends R> zipper, 
                int count, boolean delayError) {
            this.actual = actual;
            this.zipper = zipper;
            this.subscribers = new ZipSubscriber[count];
            this.row = new Object[count];
            this.delayError = delayError;
        }
        
        public void subscribe(Observable<? extends T>[] sources, int bufferSize) {
            ZipSubscriber<T, R>[] s = subscribers;
            int len = s.length;
            for (int i = 0; i < len; i++) {
                s[i] = new ZipSubscriber<T, R>(this, bufferSize);
            }
            // this makes sure the contents of the subscribers array is visible
            this.lazySet(0);
            actual.onSubscribe(this);
            for (int i = 0; i < len; i++) {
                if (cancelled) {
                    return;
                }
                sources[i].subscribe(s[i]);
            }
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                if (getAndIncrement() == 0) {
                    clear();
                }
            }
        }
        
        void clear() {
            for (ZipSubscriber<?, ?> zs : subscribers) {
                zs.dispose();
                zs.queue.clear();
            }
        }
        
        public void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            int missing = 1;
            
            final ZipSubscriber<T, R>[] zs = subscribers;
            final Observer<? super R> a = actual;
            final Object[] os = row;
            final boolean delayError = this.delayError;
            
            for (;;) {

                for (;;) {
                    int i = 0;
                    int emptyCount = 0;
                    for (ZipSubscriber<T, R> z : zs) {
                        boolean d = z.done;
                        T v = z.queue.peek();
                        boolean empty = v == null;
                        
                        if (checkTerminated(d, empty, a, delayError, z)) {
                            return;
                        }
                        
                        if (empty) {
                            emptyCount++;
                            continue;
                        }
                        
                        os[i] = v;
                        i++;
                    }
                    
                    if (emptyCount != 0) {
                        break;
                    }
                    // consume the row
                    for (ZipSubscriber<T, R> z : zs) {
                        z.queue.poll();
                    }

                    R v;
                    try {
                        v = zipper.apply(os.clone());
                    } catch (Throwable ex) {
                        clear();
                        a.onError(ex);
                        return;
                    }
                    
                    if (v == null) {
                        clear();
                        a.onError(new NullPointerException("The zipper returned a null value"));
                        return;
                    }
                    
                    a.onNext(v);
                }
                
                missing = addAndGet(-missing);
                if (missing == 0) {
                    return;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Observer<? super R> a, boolean delayError, ZipSubscriber<?, ?> source) {
            if (cancelled) {
                clear();
                return true;
            }
            
            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = source.error;
                        clear();
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = source.error;
                    if (e != null) {
                        clear();
                        a.onError(e);
                        return true;
                    } else
                    if (empty) {
                        clear();
                        a.onComplete();
                        return true;
                    }
                }
            }
            
            return false;
        }
    }
    
    static final class ZipSubscriber<T, R> implements Observer<T>, Disposable {
        
        final ZipCoordinator<T, R> parent;
        final Queue<T> queue;
        
        volatile boolean done;
        Throwable error;
        
        final AtomicReference<Disposable> s = new AtomicReference<Disposable>();
        
        static final Disposable CANCELLED = new Disposable() {
            @Override
            public void dispose() { }
        };
        
        public ZipSubscriber(ZipCoordinator<T, R> parent, int bufferSize) {
            this.parent = parent;
            this.queue = new SpscLinkedArrayQueue<T>(bufferSize);
        }
        @Override
        public void onSubscribe(Disposable s) {
            
            for (;;) {
                Disposable current = this.s.get();
                if (current == CANCELLED) {
                    s.dispose();
                    return;
                }
                if (current != null) {
                    s.dispose();
                    RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                    return;
                }
                if (this.s.compareAndSet(null, s)) {
                    return;
                } else {
                    s.dispose();
                }
            }
            
        }
        
        @Override
        public void onNext(T t) {
            if (t == null) {
                s.get().dispose();
                onError(new NullPointerException());
                return;
            }
            if (!queue.offer(t)) {
                s.get().dispose();
                onError(new IllegalStateException("Queue full?!"));
                return;
            }
            parent.drain();
        }
        
        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            parent.drain();
        }
        
        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }
        
        @Override
        public void dispose() {
            Disposable s = this.s.get();
            if (s != CANCELLED) {
                s = this.s.getAndSet(CANCELLED);
                if (s != CANCELLED && s != null) {
                    s.dispose();
                }
            }
        }
    }
}