/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.internal.producers.ProducerArbiter;
import rx.internal.util.*;
import rx.internal.util.atomic.SpscAtomicArrayQueue;
import rx.internal.util.unsafe.*;
import rx.observers.SerializedSubscriber;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.SerialSubscription;

/**
 * Maps a source sequence into Observables and concatenates them in order, subscribing
 * to one at a time.
 * @param <T> the source value type
 * @param <R> the output value type
 * 
 * @since 1.1.2
 */
public final class OnSubscribeConcatMap<T, R> implements OnSubscribe<R> {
    final Observable<? extends T> source;
    
    final Func1<? super T, ? extends Observable<? extends R>> mapper;
    
    final int prefetch;
    
    /** 
     * How to handle errors from the main and inner Observables.
     * See the constants below.
     */
    final int delayErrorMode;
    
    /** Whenever any Observable fires an error, terminate with that error immediately. */
    public static final int IMMEDIATE = 0;
    
    /** Whenever the main fires an error, wait until the inner terminates. */
    public static final int BOUNDARY = 1;
    
    /** Delay all errors to the very end. */
    public static final int END = 2;

    public OnSubscribeConcatMap(Observable<? extends T> source, Func1<? super T, ? extends Observable<? extends R>> mapper, int prefetch,
            int delayErrorMode) {
        this.source = source;
        this.mapper = mapper;
        this.prefetch = prefetch;
        this.delayErrorMode = delayErrorMode;
    }
    
    @Override
    public void call(Subscriber<? super R> child) {
        Subscriber<? super R> s;
        
        if (delayErrorMode == IMMEDIATE) {
            s = new SerializedSubscriber<R>(child);
        } else {
            s = child;
        }
        
        final ConcatMapSubscriber<T, R> parent = new ConcatMapSubscriber<T, R>(s, mapper, prefetch, delayErrorMode);
        
        child.add(parent);
        child.add(parent.inner);
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        
        if (!child.isUnsubscribed()) {
            source.unsafeSubscribe(parent);
        }
    }
    
    static final class ConcatMapSubscriber<T, R> extends Subscriber<T> {
        final Subscriber<? super R> actual;
        
        final Func1<? super T, ? extends Observable<? extends R>> mapper;
        
        final int delayErrorMode;
        
        final ProducerArbiter arbiter;
        
        final Queue<Object> queue;
        
        final AtomicInteger wip;
        
        final AtomicReference<Throwable> error;
        
        final SerialSubscription inner;
        
        volatile boolean done;
        
        volatile boolean active;

        public ConcatMapSubscriber(Subscriber<? super R> actual,
                Func1<? super T, ? extends Observable<? extends R>> mapper, int prefetch, int delayErrorMode) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrorMode = delayErrorMode;
            this.arbiter = new ProducerArbiter();
            this.wip = new AtomicInteger();
            this.error = new AtomicReference<Throwable>();
            Queue<Object> q;
            if (UnsafeAccess.isUnsafeAvailable()) {
                q = new SpscArrayQueue<Object>(prefetch);
            } else {
                q = new SpscAtomicArrayQueue<Object>(prefetch);
            }
            this.queue = q;
            this.inner = new SerialSubscription();
            this.request(prefetch);
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(NotificationLite.instance().next(t))) {
                unsubscribe();
                onError(new MissingBackpressureException());
            } else {
                drain();
            }
        }
        
        @Override
        public void onError(Throwable mainError) {
            if (ExceptionsUtils.addThrowable(error, mainError)) {
                done = true;
                if (delayErrorMode == IMMEDIATE) {
                    Throwable ex = ExceptionsUtils.terminate(error);
                    if (!ExceptionsUtils.isTerminated(ex)) {
                        actual.onError(ex);
                    }
                    inner.unsubscribe();
                } else {
                    drain();
                }
            } else {
                pluginError(mainError);
            }
        }
        
        @Override
        public void onCompleted() {
            done = true;
            drain();
        }
        
        void requestMore(long n) {
            if (n > 0) {
                arbiter.request(n);
            } else
            if (n < 0) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            }
        }
        
        void innerNext(R value) {
            actual.onNext(value);
        }
        
        void innerError(Throwable innerError, long produced) {
            if (!ExceptionsUtils.addThrowable(error, innerError)) {
                pluginError(innerError);
            } else
            if (delayErrorMode == IMMEDIATE) {
                Throwable ex = ExceptionsUtils.terminate(error);
                if (!ExceptionsUtils.isTerminated(ex)) {
                    actual.onError(ex);
                }
                unsubscribe();
            } else {
                if (produced != 0L) {
                    arbiter.produced(produced);
                }
                active = false;
                drain();
            }
        }
        
        void innerCompleted(long produced) {
            if (produced != 0L) {
                arbiter.produced(produced);
            }
            active = false;
            drain();
        }
        
        void pluginError(Throwable e) {
            RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
        }
        
        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            final int delayErrorMode = this.delayErrorMode;
            
            for (;;) {
                if (actual.isUnsubscribed()) {
                    return;
                }
                
                if (!active) {
                    if (delayErrorMode == BOUNDARY) {
                        if (error.get() != null) {
                            Throwable ex = ExceptionsUtils.terminate(error);
                            if (!ExceptionsUtils.isTerminated(ex)) {
                                actual.onError(ex);
                            }
                            return;
                        }
                    }
                    
                    boolean mainDone = done;
                    Object v = queue.poll();
                    boolean empty = v == null;
                    
                    if (mainDone && empty) {
                        Throwable ex = ExceptionsUtils.terminate(error);
                        if (ex == null) {
                            actual.onCompleted();
                        } else
                        if (!ExceptionsUtils.isTerminated(ex)) {
                            actual.onError(ex);
                        }
                        return;
                    }
                    
                    if (!empty) {
                        
                        Observable<? extends R> source;
                        
                        try {
                            source = mapper.call(NotificationLite.<T>instance().getValue(v));
                        } catch (Throwable mapperError) {
                            Exceptions.throwIfFatal(mapperError);
                            drainError(mapperError);
                            return;
                        }
                        
                        if (source == null) {
                            drainError(new NullPointerException("The source returned by the mapper was null"));
                            return;
                        }
                        
                        if (source != Observable.empty()) {

                            if (source instanceof ScalarSynchronousObservable) {
                                ScalarSynchronousObservable<? extends R> scalarSource = (ScalarSynchronousObservable<? extends R>) source;
                                
                                active = true;
                                
                                arbiter.setProducer(new ConcatMapInnerScalarProducer<T, R>(scalarSource.get(), this));
                            } else {
                                ConcatMapInnerSubscriber<T, R> innerSubscriber = new ConcatMapInnerSubscriber<T, R>(this);
                                inner.set(innerSubscriber);
        
                                if (!innerSubscriber.isUnsubscribed()) {
                                    active = true;
    
                                    source.unsafeSubscribe(innerSubscriber);
                                } else {
                                    return;
                                }
                            }
                            request(1);
                        } else {
                            request(1);
                            continue;
                        }
                    }
                }
                if (wip.decrementAndGet() == 0) {
                    break;
                }
            }
        }
        
        void drainError(Throwable mapperError) {
            unsubscribe();
            
            if (ExceptionsUtils.addThrowable(error, mapperError)) {
                Throwable ex = ExceptionsUtils.terminate(error);
                if (!ExceptionsUtils.isTerminated(ex)) {
                    actual.onError(ex);
                }
            } else {
                pluginError(mapperError);
            }
        }
    }
    
    static final class ConcatMapInnerSubscriber<T, R> extends Subscriber<R> {
        final ConcatMapSubscriber<T, R> parent;

        long produced;
        
        public ConcatMapInnerSubscriber(ConcatMapSubscriber<T, R> parent) {
            this.parent = parent;
        }
        
        @Override
        public void setProducer(Producer p) {
            parent.arbiter.setProducer(p);
        }
        
        @Override
        public void onNext(R t) {
            produced++;
            parent.innerNext(t);
        }
        
        @Override
        public void onError(Throwable e) {
            parent.innerError(e, produced);
        }
        
        @Override
        public void onCompleted() {
            parent.innerCompleted(produced);
        }
    }
    
    static final class ConcatMapInnerScalarProducer<T, R> implements Producer {
        final R value;
        
        final ConcatMapSubscriber<T, R> parent;
        
        boolean once;

        public ConcatMapInnerScalarProducer(R value, ConcatMapSubscriber<T, R> parent) {
            this.value = value;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            if (!once && n > 0L) {
                once = true;
                ConcatMapSubscriber<T, R> p = parent;
                p.innerNext(value);
                p.innerCompleted(1);
            }
        }
    }
}
