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
import rx.Observable.Operator;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.internal.util.atomic.SpscAtomicArrayQueue;
import rx.internal.util.unsafe.*;

/**
 * Buffers values into a continuous, non-overlapping Lists where the boundary is determined
 * by a predicate returning true.
 *
 * @param <T> the source and List element type
 */
public final class OperatorBufferPredicateBoundary<T> implements Operator<List<T>, T> {

    final Func1<? super T, Boolean> predicate;
    
    final int prefetch;
    
    final int capacityHint;
    
    final boolean after;
    
    public OperatorBufferPredicateBoundary(Func1<? super T, Boolean> predicate, int prefetch, int capacityHint, boolean after) {
        if (predicate == null) {
            throw new NullPointerException("predicate");
        }
        if (prefetch <= 0) {
            throw new IllegalArgumentException("prefetch > 0 required but it was " + prefetch);
        }
        if (capacityHint <= 0) {
            throw new IllegalArgumentException("capacityHint > 0 required but it was " + capacityHint);
        }
        this.predicate = predicate;
        this.prefetch = prefetch;
        this.capacityHint = capacityHint;
        this.after = after;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super List<T>> child) {
        final BoundedSubscriber<T> parent = after 
                ? new BoundedAfterSubscriber<T>(child, capacityHint, predicate, prefetch)
                : new BoundedBeforeSubscriber<T>(child, capacityHint, predicate, prefetch);
                
        child.add(parent);
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        
        return parent;
    }
    
    static abstract class BoundedSubscriber<T> extends Subscriber<T> {
        final Subscriber<? super List<T>> actual;
        
        final int capacityHint;
        
        final Func1<? super T, Boolean> predicate;
        
        final Queue<Object> queue;
        
        final AtomicLong requested;

        final AtomicInteger wip;
        
        final NotificationLite<T> nl;
        
        final int limit;

        List<T> buffer;
        
        long upstreamConsumed;
        
        volatile boolean done;
        Throwable error;

        public BoundedSubscriber(Subscriber<? super List<T>> actual, int capacityHint,
                Func1<? super T, Boolean> predicate, int prefetch) {
            this.actual = actual;
            this.capacityHint = capacityHint;
            this.predicate = predicate;
            Queue<Object> q;
            if (UnsafeAccess.isUnsafeAvailable()) {
                q = new SpscArrayQueue<Object>(prefetch);
            } else {
                q = new SpscAtomicArrayQueue<Object>(prefetch);
            }
            queue = q;
            buffer = new ArrayList<T>(capacityHint);
            requested = new AtomicLong();
            wip = new AtomicInteger();
            nl = NotificationLite.instance();
            limit = prefetch - (prefetch >> 2);
            if (prefetch == Integer.MAX_VALUE) {
                request(Long.MAX_VALUE);
            } else {
                request(prefetch);
            }
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(nl.next(t))) {
                unsubscribe();
                onError(new MissingBackpressureException());
            } else {
                drain();
            }
        }
        
        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
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
            } else
            if (n < 0) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            }
        }

        abstract void drain();
    }
    
    static final class BoundedAfterSubscriber<T> extends BoundedSubscriber<T> {

        public BoundedAfterSubscriber(Subscriber<? super List<T>> actual, int capacityHint,
                Func1<? super T, Boolean> predicate, int prefetch) {
            super(actual, capacityHint, predicate, prefetch);
        }
        
        @Override
        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            
            final Subscriber<? super List<T>> localSubscriber = actual;
            final Queue<Object> localQueue = queue;
            int missed = 1;
            
            for (;;) {

                long localRequested = requested.get();
                long localEmission = 0L;
                long localConsumption = 0L;
                List<T> localBuffer = buffer;
                
                while (localEmission != localRequested) {
                    if (localSubscriber.isUnsubscribed()) {
                        return;
                    }
                    
                    boolean mainDone = done;
                    
                    if (mainDone) {
                        Throwable exception = error;
                        if (exception != null) {
                            buffer = null;
                            localSubscriber.onError(exception);
                            return;
                        }
                    }
                    
                    Object notification = localQueue.poll();
                    boolean empty = notification == null;
                    
                    if (mainDone && empty) {
                        buffer = null;
                        if (!localBuffer.isEmpty()) {
                            localSubscriber.onNext(localBuffer);
                        }
                        localSubscriber.onCompleted();
                        return;
                    }
                    if (empty) {
                        break;
                    }
                    
                    T value = nl.getValue(notification);
                    
                    localBuffer.add(value);
                    localConsumption++;
                    
                    boolean emit;
                    
                    try {
                        emit = predicate.call(value);
                    } catch (Throwable ex) {
                        unsubscribe();
                        buffer = null;
                        Exceptions.throwOrReport(ex, localSubscriber, value);
                        return;
                    }
                    
                    if (emit) {
                        localSubscriber.onNext(localBuffer);
                        localBuffer = new ArrayList<T>(capacityHint);
                        buffer = localBuffer;
                        
                        localEmission++;
                    }
                }
                
                if (localEmission == localRequested) {
                    if (localSubscriber.isUnsubscribed()) {
                        return;
                    }
                    
                    boolean mainDone = done;

                    if (mainDone) {
                        Throwable exception = error;
                        if (exception != null) {
                            buffer = null;
                            localSubscriber.onError(exception);
                            return;
                        } else
                        if (localQueue.isEmpty() && localBuffer.isEmpty()) {
                            buffer = null;
                            localSubscriber.onCompleted();
                            return;
                        }
                    }
                }
                
                if (localEmission != 0L) {
                    BackpressureUtils.produced(requested, localEmission);
                }
                if (localConsumption != 0L) {
                    long p = upstreamConsumed + localConsumption;
                    if (p >= limit) {
                        upstreamConsumed = 0L;
                        request(p);
                    } else {
                        upstreamConsumed = p;
                    }
                }
                
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class BoundedBeforeSubscriber<T> extends BoundedSubscriber<T> {
        public BoundedBeforeSubscriber(Subscriber<? super List<T>> actual, int capacityHint,
                Func1<? super T, Boolean> predicate, int prefetch) {
            super(actual, capacityHint, predicate, prefetch);
        }

        @Override
        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            
            final Subscriber<? super List<T>> localSubscriber = actual;
            final Queue<Object> localQueue = queue;
            int missed = 1;
            
            for (;;) {

                long localRequested = requested.get();
                long localEmission = 0L;
                long localConsumption = 0L;
                List<T> localBuffer = buffer;
                
                while (localEmission != localRequested) {
                    if (localSubscriber.isUnsubscribed()) {
                        return;
                    }
                    
                    boolean mainDone = done;
                    
                    if (mainDone) {
                        Throwable exception = error;
                        if (exception != null) {
                            buffer = null;
                            localSubscriber.onError(exception);
                            return;
                        }
                    }
                    
                    Object o = localQueue.poll();
                    boolean empty = o == null;
                    
                    if (mainDone && empty) {
                        buffer = null;
                        if (!localBuffer.isEmpty()) {
                            localSubscriber.onNext(localBuffer);
                        }
                        localSubscriber.onCompleted();
                        return;
                    }
                    if (empty) {
                        break;
                    }
                    
                    T value = nl.getValue(o);
                    
                    boolean emit;
                    
                    try {
                        emit = predicate.call(value);
                    } catch (Throwable ex) {
                        unsubscribe();
                        buffer = null;
                        Exceptions.throwOrReport(ex, localSubscriber, value);
                        return;
                    }
                    
                    if (emit && !localBuffer.isEmpty()) {
                        localSubscriber.onNext(localBuffer);
                        localBuffer = new ArrayList<T>(capacityHint);
                        buffer = localBuffer;
                        
                        localEmission++;
                    }
                    
                    localBuffer.add(value);
                    
                    localConsumption++;
                }
                
                if (localEmission == localRequested) {
                    if (localSubscriber.isUnsubscribed()) {
                        return;
                    }
                    
                    boolean mainDone = done;

                    if (mainDone) {
                        Throwable exception = error;
                        if (exception != null) {
                            buffer = null;
                            localSubscriber.onError(exception);
                            return;
                        } else
                        if (localQueue.isEmpty() && localBuffer.isEmpty()) {
                            buffer = null;
                            localSubscriber.onCompleted();
                            return;
                        }
                    }
                }
                
                if (localEmission != 0L) {
                    BackpressureUtils.produced(requested, localEmission);
                }
                
                if (localConsumption != 0L) {
                    long produced = upstreamConsumed + localConsumption;
                    if (produced >= limit) {
                        upstreamConsumed = 0L;
                        request(produced);
                    } else {
                        upstreamConsumed = produced;
                    }
                }
                
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
