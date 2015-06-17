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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.MissingBackpressureException;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.util.unsafe.SpscArrayQueue;
import rx.internal.util.unsafe.UnsafeAccess;

/**
 * Applies a function of your choosing to every item emitted by an {@code Observable}, and emits the results of
 * this transformation as a new {@code Observable}.
 * <p>
 * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/map.png" alt="">
 */
public final class OperatorMapNotification<T, R> implements Operator<R, T> {

    private final Func1<? super T, ? extends R> onNext;
    private final Func1<? super Throwable, ? extends R> onError;
    private final Func0<? extends R> onCompleted;

    public OperatorMapNotification(Func1<? super T, ? extends R> onNext, Func1<? super Throwable, ? extends R> onError, Func0<? extends R> onCompleted) {
        this.onNext = onNext;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> o) {
        Subscriber<T> subscriber = new Subscriber<T>() {
            SingleEmitter<R> emitter;
            @Override
            public void setProducer(Producer producer) {
                emitter = new SingleEmitter<R>(o, producer, this);
                o.setProducer(emitter);
            }
            
            @Override
            public void onCompleted() {
                try {
                    emitter.offerAndComplete(onCompleted.call());
                } catch (Throwable e) {
                    o.onError(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                try {
                    emitter.offerAndComplete(onError.call(e));
                } catch (Throwable e2) {
                    o.onError(e);
                }
            }

            @Override
            public void onNext(T t) {
                try {
                    emitter.offer(onNext.call(t));
                } catch (Throwable e) {
                    o.onError(OnErrorThrowable.addValueAsLastCause(e, t));
                }
            }

        };
        o.add(subscriber);
        return subscriber;
    }
    static final class SingleEmitter<T> extends AtomicLong implements Producer, Subscription {
        /** */
        private static final long serialVersionUID = -249869671366010660L;
        final NotificationLite<T> nl;
        final Subscriber<? super T> child;
        final Producer producer;
        final Subscription cancel;
        final Queue<Object> queue;
        volatile boolean complete;
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        boolean missed;
        
        public SingleEmitter(Subscriber<? super T> child, Producer producer, Subscription cancel) {
            this.child = child;
            this.producer = producer;
            this.cancel = cancel;
            this.queue = UnsafeAccess.isUnsafeAvailable() 
                    ? new SpscArrayQueue<Object>(2) 
                    : new ConcurrentLinkedQueue<Object>();
                    
            this.nl = NotificationLite.instance();
        }
        @Override
        public void request(long n) {
            for (;;) {
                long r = get();
                if (r < 0) {
                    return;
                }
                long u = r + n;
                if (u < 0) {
                    u = Long.MAX_VALUE;
                }
                if (compareAndSet(r, u)) {
                    producer.request(n);
                    drain();
                    return;
                }
            }
        }
        
        void produced(long n) {
            for (;;) {
                long r = get();
                if (r < 0) {
                    return;
                }
                long u = r - n;
                if (u < 0) {
                    throw new IllegalStateException("More produced (" + n + ") than requested (" + r + ")");
                }
                if (compareAndSet(r, u)) {
                    return;
                }
            }
        }
        
        public void offer(T value) {
            if (!queue.offer(value)) {
                child.onError(new MissingBackpressureException());
                unsubscribe();
            } else {
                drain();
            }
        }
        public void offerAndComplete(T value) {
            if (!this.queue.offer(value)) {
                child.onError(new MissingBackpressureException());
                unsubscribe();
            } else {
                this.complete = true;
                drain();
            }
        }
        
        void drain() {
            synchronized (this) {
                if (emitting) {
                    missed = true;
                    return;
                }
                emitting = true;
                missed = false;
            }
            boolean skipFinal = false;
            try {
                for (;;) {
                    
                    long r = get();
                    boolean c = complete;
                    boolean empty = queue.isEmpty();
                    
                    if (c && empty) {
                        child.onCompleted();
                        skipFinal = true;
                        return;
                    } else
                    if (r > 0) {
                        Object v = queue.poll();
                        if (v != null) {
                            child.onNext(nl.getValue(v));
                            produced(1);
                        } else
                        if (c) {
                            child.onCompleted();
                            skipFinal = true;
                            return;
                        }
                    }
                    
                    synchronized (this) {
                        if (!missed) {
                            skipFinal = true;
                            emitting = false;
                            return;
                        }
                        missed = false;
                    }
                }
            } finally {
                if (!skipFinal) {
                    synchronized (this) {
                        emitting = false;
                    }
                }
            }
        }
        
        @Override
        public boolean isUnsubscribed() {
            return get() < 0;
        }
        @Override
        public void unsubscribe() {
            long r = get();
            if (r != Long.MIN_VALUE) {
                r = getAndSet(Long.MIN_VALUE);
                if (r != Long.MIN_VALUE) {
                    cancel.unsubscribe();
                }
            }
        }
    }
}