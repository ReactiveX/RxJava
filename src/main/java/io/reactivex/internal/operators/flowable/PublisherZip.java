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

import java.util.Queue;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.functions.Function;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class PublisherZip<T, R> implements Publisher<R> {
    
    final Publisher<? extends T>[] sources;
    final Iterable<? extends Publisher<? extends T>> sourcesIterable;
    final Function<? super Object[], ? extends R> zipper;
    final int bufferSize;
    final boolean delayError;
    
    public PublisherZip(Publisher<? extends T>[] sources,
            Iterable<? extends Publisher<? extends T>> sourcesIterable,
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
    public void subscribe(Subscriber<? super R> s) {
        Publisher<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Publisher[8];
            for (Publisher<? extends T> p : sourcesIterable) {
                if (count == sources.length) {
                    Publisher<? extends T>[] b = new Publisher[count + (count >> 2)];
                    System.arraycopy(sources, 0, b, 0, count);
                    sources = b;
                }
                sources[count++] = p;
            }
        } else {
            count = sources.length;
        }
        
        if (count == 0) {
            EmptySubscription.complete(s);
            return;
        }
        
        ZipCoordinator<T, R> zc = new ZipCoordinator<T, R>(s, zipper, count, delayError);
        zc.subscribe(sources, bufferSize);
    }
    
    static final class ZipCoordinator<T, R> extends AtomicInteger implements Subscription {
        /** */
        private static final long serialVersionUID = 2983708048395377667L;
        final Subscriber<? super R> actual;
        final Function<? super Object[], ? extends R> zipper;
        final ZipSubscriber<T, R>[] subscribers;
        final Object[] row;
        final boolean delayError;
        
        final AtomicLong requested = new AtomicLong();

        volatile boolean cancelled;
        
        @SuppressWarnings("unchecked")
        public ZipCoordinator(Subscriber<? super R> actual, 
                Function<? super Object[], ? extends R> zipper, 
                int count, boolean delayError) {
            this.actual = actual;
            this.zipper = zipper;
            this.subscribers = new ZipSubscriber[count];
            this.row = new Object[count];
            this.delayError = delayError;
        }
        
        public void subscribe(Publisher<? extends T>[] sources, int bufferSize) {
            ZipSubscriber<T, R>[] s = subscribers;
            int len = s.length;
            for (int i = 0; i < len; i++) {
                s[i] = new ZipSubscriber<T, R>(this, bufferSize);
            }
            // this makes sure the contents of the subscribers array is visible
            requested.lazySet(0);
            actual.onSubscribe(this);
            for (int i = 0; i < len; i++) {
                if (cancelled) {
                    return;
                }
                sources[i].subscribe(s[i]);
            }
        }
        
        @Override
        public void request(long n) {
            if (n <= 0) {
                RxJavaPlugins.onError(new IllegalArgumentException("n > required but it was " + n));
                return;
            }
            BackpressureHelper.add(requested, n);
            drain();
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                if (getAndIncrement() == 0) {
                    clear();
                }
            }
        }
        
        void clear() {
            for (ZipSubscriber<?, ?> zs : subscribers) {
                zs.cancel();
                zs.queue.clear();
            }
        }
        
        public void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            int missing = 1;
            
            final ZipSubscriber<T, R>[] zs = subscribers;
            final Subscriber<? super R> a = actual;
            final Object[] os = row;
            final boolean delayError = this.delayError;
            
            for (;;) {

                long r = requested.get();
                boolean unbounded = r == Long.MAX_VALUE;
                long e = 0;
                
                while (r != 0) {
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
                        a.onError(new NullPointerException("The zipper returned null"));
                        return;
                    }
                    
                    a.onNext(v);
                    
                    r--;
                    e++;
                }
                
                if (e != 0) {
                    if (!unbounded) {
                        requested.addAndGet(-e);
                    }
                    for (ZipSubscriber<T, R> z : zs) {
                        z.request(e);
                    }
                }
                
                missing = addAndGet(-missing);
                if (missing == 0) {
                    return;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super R> a, boolean delayError, ZipSubscriber<?, ?> source) {
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
    
    static final class ZipSubscriber<T, R> extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -4627193790118206028L;
        
        final ZipCoordinator<T, R> parent;
        final int bufferSize;
        final Queue<T> queue;
        
        volatile boolean done;
        Throwable error;
        
        final AtomicReference<Subscription> s = new AtomicReference<Subscription>();
        
        Subscription cachedS;
        
        static final Subscription CANCELLED = new Subscription() {
            @Override
            public void request(long n) {
                
            }
            
            @Override
            public void cancel() {
                
            }
        };
        
        public ZipSubscriber(ZipCoordinator<T, R> parent, int bufferSize) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            Queue<T> q;
            if (Pow2.isPowerOfTwo(bufferSize)) {
                q = new SpscArrayQueue<T>(bufferSize);
            } else {
                q = new SpscExactArrayQueue<T>(bufferSize);
            }
            this.queue = q;
        }
        @Override
        public void onSubscribe(Subscription s) {
            
            for (;;) {
                Subscription current = this.s.get();
                if (current == CANCELLED) {
                    s.cancel();
                    return;
                }
                if (current != null) {
                    s.cancel();
                    RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                    return;
                }
                if (this.s.compareAndSet(null, s)) {
                    lazySet(bufferSize);
                    s.request(bufferSize);
                    return;
                } else {
                    s.cancel();
                }
            }
            
        }
        
        @Override
        public void onNext(T t) {
            if (t == null) {
                s.get().cancel();
                onError(new NullPointerException());
                return;
            }
            if (!queue.offer(t)) {
                s.get().cancel();
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
        public void request(long n) {
            lazySet(BackpressureHelper.addCap(get(), n));
            // this method is only called if s is no longer null;
            if (cachedS == null) {
                cachedS = s.get();
            }
            cachedS.request(n);
        }
        
        @Override
        public void cancel() {
            Subscription s = this.s.get();
            if (s != CANCELLED) {
                s = this.s.getAndSet(CANCELLED);
                if (s != CANCELLED && s != null) {
                    s.cancel();
                }
            }
        }
        
        public void produced(long n) {
            long v = get() - n;
            if (v < 0L) {
                RxJavaPlugins.onError(new IllegalArgumentException("More produced than requested: " + v));
                v = 0;
            }
            lazySet(v);
        }
    }
}