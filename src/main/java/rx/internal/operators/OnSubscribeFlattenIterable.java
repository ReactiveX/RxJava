/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.operators;

import java.util.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.internal.util.*;
import rx.internal.util.atomic.*;
import rx.internal.util.unsafe.*;

/**
 * Flattens a sequence if Iterable sources, generated via a function, into a single sequence.
 *
 * @param <T> the input value type
 * @param <R> the output value type
 */
public final class OnSubscribeFlattenIterable<T, R> implements OnSubscribe<R> {
    
    final Observable<? extends T> source;
    
    final Func1<? super T, ? extends Iterable<? extends R>> mapper;
    
    final int prefetch;
    
    /** Protected: use createFrom to handle source-dependent optimizations. */
    protected OnSubscribeFlattenIterable(Observable<? extends T> source,
            Func1<? super T, ? extends Iterable<? extends R>> mapper, int prefetch) {
        this.source = source;
        this.mapper = mapper;
        this.prefetch = prefetch;
    }
    
    @Override
    public void call(Subscriber<? super R> t) {
        final FlattenIterableSubscriber<T, R> parent = new FlattenIterableSubscriber<T, R>(t, mapper, prefetch);
        
        t.add(parent);
        t.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        
        source.unsafeSubscribe(parent);
    }
    
    public static <T, R> Observable<R> createFrom(Observable<? extends T> source,
            Func1<? super T, ? extends Iterable<? extends R>> mapper, int prefetch) {
        if (source instanceof ScalarSynchronousObservable) {
            T scalar = ((ScalarSynchronousObservable<? extends T>) source).get();
            return Observable.create(new OnSubscribeScalarFlattenIterable<T, R>(scalar, mapper));
        }
        return Observable.create(new OnSubscribeFlattenIterable<T, R>(source, mapper, prefetch));
    }
    
    static final class FlattenIterableSubscriber<T, R> extends Subscriber<T> {
        final Subscriber<? super R> actual;
        
        final Func1<? super T, ? extends Iterable<? extends R>> mapper;
        
        final long limit;
        
        final Queue<Object> queue;

        final AtomicReference<Throwable> error;
        
        final AtomicLong requested;
        
        final AtomicInteger wip;
        
        final NotificationLite<T> nl;
        
        volatile boolean done;
        
        long produced;
        
        Iterator<? extends R> active;
        
        public FlattenIterableSubscriber(Subscriber<? super R> actual,
                Func1<? super T, ? extends Iterable<? extends R>> mapper, int prefetch) {
            this.actual = actual;
            this.mapper = mapper;
            this.error = new AtomicReference<Throwable>();
            this.wip = new AtomicInteger();
            this.requested = new AtomicLong();
            this.nl = NotificationLite.instance();
            if (prefetch == Integer.MAX_VALUE) {
                this.limit = Long.MAX_VALUE;
                this.queue = new SpscLinkedArrayQueue<Object>(RxRingBuffer.SIZE);
            } else {
                // limit = prefetch * 75% rounded up
                this.limit = prefetch - (prefetch >> 2);
                if (UnsafeAccess.isUnsafeAvailable()) {
                    this.queue = new SpscArrayQueue<Object>(prefetch);
                } else {
                    this.queue = new SpscAtomicArrayQueue<Object>(prefetch);
                }
            }
            request(prefetch);
        }
        
        @Override
        public void onNext(T t) {
            if (!queue.offer(nl.next(t))) {
                unsubscribe();
                onError(new MissingBackpressureException());
                return;
            }
            drain();
        }
        
        @Override
        public void onError(Throwable e) {
            if (ExceptionsUtils.addThrowable(error, e)) {
                done = true;
                drain();
            } else {
                RxJavaPluginUtils.handleException(e);
            }
        }
        
        @Override
        public void onCompleted() {
            done = true;
            drain();
        }
        
        void requestMore(long n) {
            if (n > 0) {
                BackpressureUtils.getAndAddRequest(requested, n);
                drain();
            } else if (n < 0) {
                throw new IllegalStateException("n >= 0 required but it was " + n);
            }
        }
        
        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            
            final Subscriber<? super R> actual = this.actual;
            final Queue<Object> queue = this.queue;
            
            int missed = 1;
            
            for (;;) {
                
                Iterator<? extends R> it = active;
                
                if (it == null) {
                    boolean d = done;
                    
                    Object v = queue.poll();
                    
                    boolean empty = v == null;

                    if (checkTerminated(d, empty, actual, queue)) {
                        return;
                    }
                    
                    if (!empty) {
                    
                        long p = produced + 1;
                        if (p == limit) {
                            produced = 0L;
                            request(p);
                        } else {
                            produced = p;
                        }
                        
                        boolean b;
                        
                        try {
                            Iterable<? extends R> iter = mapper.call(nl.getValue(v));
                            
                            it = iter.iterator();
                            
                            b = it.hasNext();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            
                            it = null;
                            onError(ex);
                            
                            continue;
                        }
                        
                        if (!b) {
                            continue;
                        }
                        
                        active = it;
                    }
                }
                
                if (it != null) {
                    long r = requested.get();
                    long e = 0L;
                    
                    while (e != r) {
                        if (checkTerminated(done, false, actual, queue)) {
                            return;
                        }
                        
                        R v;
                        
                        try {
                            v = it.next();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            it = null;
                            active = null;
                            onError(ex);
                            break;
                        }
                        
                        actual.onNext(v);

                        if (checkTerminated(done, false, actual, queue)) {
                            return;
                        }

                        e++;

                        boolean b;
                        
                        try {
                            b = it.hasNext();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            it = null;
                            active = null;
                            onError(ex);
                            break;
                        }
                        
                        if (!b) {
                            it = null;
                            active = null;
                            break;
                        }
                    }
                    
                    if (e == r) {
                        if (checkTerminated(done, queue.isEmpty() && it == null, actual, queue)) {
                            return;
                        }
                    }
                    
                    if (e != 0L) {
                        BackpressureUtils.produced(requested, e);
                    }
                    
                    if (it == null) {
                        continue;
                    }
                }
                
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a, Queue<?> q) {
            if (a.isUnsubscribed()) {
                q.clear();
                active = null;
                return true;
            }
            
            if (d) {
                Throwable ex = error.get();
                if (ex != null) {
                    ex = ExceptionsUtils.terminate(error);
                    unsubscribe();
                    q.clear();
                    active = null;
                    
                    a.onError(ex);
                    return true;
                } else
                if (empty) {
                    
                    a.onCompleted();
                    return true;
                }
            }
            
            return false;
        }
    }
    
    /**
     * A custom flattener that works from a scalar value and computes the iterable
     * during subscription time.
     *
     * @param <T> the scalar's value type
     * @param <R> the result value type
     */
    static final class OnSubscribeScalarFlattenIterable<T, R> implements OnSubscribe<R> {
        final T value;
        
        final Func1<? super T, ? extends Iterable<? extends R>> mapper;

        public OnSubscribeScalarFlattenIterable(T value, Func1<? super T, ? extends Iterable<? extends R>> mapper) {
            this.value = value;
            this.mapper = mapper;
        }

        @Override
        public void call(Subscriber<? super R> t) {
            Iterator<? extends R> itor;
            boolean b;
            try {
                Iterable<? extends R> it = mapper.call(value);
                
                itor = it.iterator();
                
                b = itor.hasNext();
            } catch (Throwable ex) {
                Exceptions.throwOrReport(ex, t, value);
                return;
            }
            
            if (!b) {
                t.onCompleted();
                return;
            }
            
            t.setProducer(new OnSubscribeFromIterable.IterableProducer<R>(t, itor));
        }
    }
}
