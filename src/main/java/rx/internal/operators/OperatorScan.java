/**
 * Copyright 2014 Netflix, Inc.
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

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.Observable.Operator;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.util.atomic.SpscLinkedAtomicQueue;
import rx.internal.util.unsafe.*;

/**
 * Returns an Observable that applies a function to the first item emitted by a source Observable, then feeds
 * the result of that function along with the second item emitted by an Observable into the same function, and
 * so on until all items have been emitted by the source Observable, emitting the result of each of these
 * iterations.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/scan.png" alt="">
 * <p>
 * This sort of function is sometimes called an accumulator.
 * <p>
 * Note that when you pass a seed to {@code scan} the resulting Observable will emit that seed as its
 * first emitted item.
 * 
 * @param <R> the aggregate and output type
 * @param <T> the input value type
 */
public final class OperatorScan<R, T> implements Operator<R, T> {

    private final Func0<R> initialValueFactory;
    final Func2<R, ? super T, R> accumulator;
    // sentinel if we don't receive an initial value
    private static final Object NO_INITIAL_VALUE = new Object();

    /**
     * Applies an accumulator function over an observable sequence and returns each intermediate result with the
     * specified source and accumulator.
     * 
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each element from the sequence
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212007.aspx">Observable.Scan(TSource, TAccumulate) Method (IObservable(TSource), TAccumulate, Func(TAccumulate, TSource,
     *      TAccumulate))</a>
     */
    public OperatorScan(final R initialValue, Func2<R, ? super T, R> accumulator) {
        this(new Func0<R>() {

            @Override
            public R call() {
                return initialValue;
            }
            
        }, accumulator);
    }
    
    public OperatorScan(Func0<R> initialValueFactory, Func2<R, ? super T, R> accumulator) {
        this.initialValueFactory = initialValueFactory;
        this.accumulator = accumulator;
    }

    /**
     * Applies an accumulator function over an observable sequence and returns each intermediate result with the
     * specified source and accumulator.
     * 
     * @param accumulator
     *            an accumulator function to be invoked on each element from the sequence
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665.aspx">Observable.Scan(TSource) Method (IObservable(TSource), Func(TSource, TSource, TSource))</a>
     */
    @SuppressWarnings("unchecked")
    public OperatorScan(final Func2<R, ? super T, R> accumulator) {
        this((R) NO_INITIAL_VALUE, accumulator);
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> child) {
        final R initialValue = initialValueFactory.call();
        
        if (initialValue == NO_INITIAL_VALUE) {
            return new Subscriber<T>(child) {
                boolean once;
                R value;
                @SuppressWarnings("unchecked")
                @Override
                public void onNext(T t) {
                    R v;
                    if (!once) {
                        once = true;
                        v = (R)t;
                    } else {
                        v = value;
                        try {
                            v = accumulator.call(v, t);
                        } catch (Throwable e) {
                            Exceptions.throwOrReport(e, child, t);
                            return;
                        }
                    }
                    value = v;
                    child.onNext(v);
                }
                @Override
                public void onError(Throwable e) {
                    child.onError(e);
                }
                @Override
                public void onCompleted() {
                    child.onCompleted();
                }
            };
        }
        
        final InitialProducer<R> ip = new InitialProducer<R>(initialValue, child);
        
        Subscriber<T> parent = new Subscriber<T>() {
            private R value = initialValue;

            @Override
            public void onNext(T currentValue) {
                R v = value;
                try {
                    v = accumulator.call(v, currentValue);
                } catch (Throwable e) {
                    Exceptions.throwOrReport(e, this, currentValue);
                    return;
                }
                value = v;
                ip.onNext(v);
            }

            @Override
            public void onError(Throwable e) {
                ip.onError(e);
            }

            @Override
            public void onCompleted() {
                ip.onCompleted();
            }
            
            @Override
            public void setProducer(final Producer producer) {
                ip.setProducer(producer);
            }
        };
        
        child.add(parent);
        child.setProducer(ip);
        return parent;
    }
    
    static final class InitialProducer<R> implements Producer, Observer<R> {
        final Subscriber<? super R> child;
        final Queue<Object> queue;
        
        boolean emitting;
        /** Missed a terminal event. */
        boolean missed;
        /** Missed a request. */
        long missedRequested;
        /** The current requested amount. */
        final AtomicLong requested;
        /** The current producer. */
        volatile Producer producer;
        
        volatile boolean done;
        Throwable error;
        
        public InitialProducer(R initialValue, Subscriber<? super R> child) {
            this.child = child;
            Queue<Object> q;
            // TODO switch to the linked-array based queue once available
            if (UnsafeAccess.isUnsafeAvailable()) {
                q = new SpscLinkedQueue<Object>(); // new SpscUnboundedArrayQueue<R>(8);
            } else {
                q = new SpscLinkedAtomicQueue<Object>();  // new SpscUnboundedAtomicArrayQueue<R>(8);
            }
            this.queue = q;
            q.offer(NotificationLite.instance().next(initialValue));
            this.requested = new AtomicLong();
        }
        
        @Override
        public void onNext(R t) {
            queue.offer(NotificationLite.instance().next(t));
            emit();
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super R> child) {
            if (child.isUnsubscribed()) {
                return true;
            }
            if (d) {
                Throwable err = error;
                if (err != null) {
                    child.onError(err);
                    return true;
                } else
                if (empty) {
                    child.onCompleted();
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            emit();
        }
        
        @Override
        public void onCompleted() {
            done = true;
            emit();
        }
        
        @Override
        public void request(long n) {
            if (n < 0L) {
                throw new IllegalArgumentException("n >= required but it was " + n);
            } else
            if (n != 0L) {
                BackpressureUtils.getAndAddRequest(requested, n);
                Producer p = producer;
                if (p == null) {
                    // not synchronizing on this to avoid clash with emit()
                    synchronized (requested) {
                        p = producer;
                        if (p == null) {
                            long mr = missedRequested;
                            missedRequested = BackpressureUtils.addCap(mr, n);
                        }
                    }
                }
                if (p != null) {
                    p.request(n);
                }
                emit();
            }
        }
        
        public void setProducer(Producer p) {
            if (p == null) {
                throw new NullPointerException();
            }
            long mr;
            // not synchronizing on this to avoid clash with emit()
            synchronized (requested) {
                if (producer != null) {
                    throw new IllegalStateException("Can't set more than one Producer!");
                }
                mr = missedRequested;
                // request one less because of the initial value, this happens once
                // and is performed only if the request is not at MAX_VALUE already
                if (mr != Long.MAX_VALUE) {
                    mr -= 1;
                }
                missedRequested = 0L;
                producer = p;
            }
            
            if (mr > 0L) {
                p.request(mr);
            }
            emit();
        }
        
        void emit() {
            synchronized (this) {
                if (emitting) {
                    missed = true;
                    return;
                }
                emitting = true;
            }
            emitLoop();
        }
        
        void emitLoop() {
            final Subscriber<? super R> child = this.child;
            final Queue<Object> queue = this.queue;
            final NotificationLite<R> nl = NotificationLite.instance();
            AtomicLong requested = this.requested;
            
            long r = requested.get();
            for (;;) {
                boolean max = r == Long.MAX_VALUE;
                boolean d = done;
                boolean empty = queue.isEmpty();
                if (checkTerminated(d, empty, child)) {
                    return;
                }
                long e = 0L;
                while (r != 0L) {
                    d = done;
                    Object o = queue.poll();
                    empty = o == null;
                    if (checkTerminated(d, empty, child)) {
                        return;
                    }
                    if (empty) {
                        break;
                    }
                    R v = nl.getValue(o);
                    try {
                        child.onNext(v);
                    } catch (Throwable ex) {
                        Exceptions.throwOrReport(ex, child, v);
                        return;
                    }
                    r--;
                    e--;
                }
                
                if (e != 0 && !max) {
                    r = requested.addAndGet(e);
                }
                
                synchronized (this) {
                    if (!missed) {
                        emitting = false;
                        return;
                    }
                    missed = false;
                }
            }
        }
    }
}
