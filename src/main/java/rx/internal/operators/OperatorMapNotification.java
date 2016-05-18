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

import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.Operator;
import rx.exceptions.Exceptions;
import rx.functions.*;

/**
 * Applies a function of your choosing to every item emitted by an {@code Observable}, and emits the results of
 * this transformation as a new {@code Observable}.
 * <p>
 * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/map.png" alt="">
 * @param <T> the input value type
 * @param <R> the output value type
 */
public final class OperatorMapNotification<T, R> implements Operator<R, T> {

    final Func1<? super T, ? extends R> onNext;
    final Func1<? super Throwable, ? extends R> onError;
    final Func0<? extends R> onCompleted;

    public OperatorMapNotification(Func1<? super T, ? extends R> onNext, Func1<? super Throwable, ? extends R> onError, Func0<? extends R> onCompleted) {
        this.onNext = onNext;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> child) {
        final MapNotificationSubscriber<T, R> parent = new MapNotificationSubscriber<T, R>(child, onNext, onError, onCompleted);
        child.add(parent);
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.requestInner(n);
            }
        });
        return parent;
    }
    
    static final class MapNotificationSubscriber<T, R> extends Subscriber<T> {
        
        final Subscriber<? super R> actual;
        
        final Func1<? super T, ? extends R> onNext;
        
        final Func1<? super Throwable, ? extends R> onError;
        
        final Func0<? extends R> onCompleted;
        
        final AtomicLong requested;

        final AtomicLong missedRequested;

        final AtomicReference<Producer> producer;

        long produced;
        
        R value;
        
        static final long COMPLETED_FLAG = Long.MIN_VALUE;
        static final long REQUESTED_MASK = Long.MAX_VALUE;
        
        public MapNotificationSubscriber(Subscriber<? super R> actual, Func1<? super T, ? extends R> onNext,
                Func1<? super Throwable, ? extends R> onError, Func0<? extends R> onCompleted) {
            this.actual = actual;
            this.onNext = onNext;
            this.onError = onError;
            this.onCompleted = onCompleted;
            this.requested = new AtomicLong();
            this.missedRequested = new AtomicLong();
            this.producer = new AtomicReference<Producer>();
        }

        @Override
        public void onNext(T t) {
            try {
                produced++;
                actual.onNext(onNext.call(t));
            } catch (Throwable ex) {
                Exceptions.throwOrReport(ex, actual, t);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            accountProduced();
            try {
                value = onError.call(e);
            } catch (Throwable ex) {
                Exceptions.throwOrReport(ex, actual, e);
            }
            tryEmit();
        }
        
        @Override
        public void onCompleted() {
            accountProduced();
            try {
                value = onCompleted.call();
            } catch (Throwable ex) {
                Exceptions.throwOrReport(ex, actual);
            }
            tryEmit();
        }
        
        void accountProduced() {
            long p = produced;
            if (p != 0L && producer.get() != null) {
                BackpressureUtils.produced(requested, p);
            }
        }
        
        @Override
        public void setProducer(Producer p) {
            if (producer.compareAndSet(null, p)) {
                long r = missedRequested.getAndSet(0L);
                if (r != 0L) {
                    p.request(r);
                }
            } else {
                throw new IllegalStateException("Producer already set!");
            }
        }
        
        void tryEmit() {
            for (;;) {
                long r = requested.get();
                if ((r & COMPLETED_FLAG) != 0) {
                    break;
                }
                if (requested.compareAndSet(r, r | COMPLETED_FLAG)) {
                    if (r != 0 || producer.get() == null) {
                        if (!actual.isUnsubscribed()) {
                            actual.onNext(value);
                        }
                        if (!actual.isUnsubscribed()) {
                            actual.onCompleted();
                        }
                    }
                    return;
                }
            }
        }
        
        void requestInner(long n) {
            if (n < 0L) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            }
            if (n == 0L) {
                return;
            }
            for (;;) {
                long r = requested.get();
                
                if ((r & COMPLETED_FLAG) != 0L) {
                    long v = r & REQUESTED_MASK;
                    long u = BackpressureUtils.addCap(v, n) | COMPLETED_FLAG;
                    if (requested.compareAndSet(r, u)) {
                        if (v == 0L) {
                            if (!actual.isUnsubscribed()) {
                                actual.onNext(value);
                            }
                            if (!actual.isUnsubscribed()) {
                                actual.onCompleted();
                            }
                        }
                        return;
                    }
                } else {
                    long u = BackpressureUtils.addCap(r, n);
                    if (requested.compareAndSet(r, u)) {
                        break;
                    }
                }
            }
            
            AtomicReference<Producer> localProducer = producer;
            Producer actualProducer = localProducer.get();
            if (actualProducer != null) {
                actualProducer.request(n);
            } else {
                BackpressureUtils.getAndAddRequest(missedRequested, n);
                actualProducer = localProducer.get();
                if (actualProducer != null) {
                    long r = missedRequested.getAndSet(0L);
                    if (r != 0L) {
                        actualProducer.request(r);
                    }
                }
            }
        }
    }
}