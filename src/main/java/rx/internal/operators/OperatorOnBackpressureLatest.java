/**
 * Copyright 2014 Netflix, Inc.
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

import java.util.concurrent.atomic.*;

import rx.Observable.Operator;
import rx.*;

/**
 * An operator which drops all but the last received value in case the downstream
 * doesn't request more.
 * @param <T> the value type
 */
public final class OperatorOnBackpressureLatest<T> implements Operator<T, T> {
    /** Holds a singleton instance initialized on class-loading. */
    static final class Holder {
        static final OperatorOnBackpressureLatest<Object> INSTANCE = new OperatorOnBackpressureLatest<Object>();
    }
    
    /**
     * Returns a singleton instance of the OnBackpressureLatest operator since it is stateless.
     * @param <T> the value type
     * @return the single instanceof OperatorOnBackpressureLatest
     */
    @SuppressWarnings("unchecked")
    public static <T> OperatorOnBackpressureLatest<T> instance() {
        return (OperatorOnBackpressureLatest<T>)Holder.INSTANCE;
    }
    
    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final LatestEmitter<T> producer = new LatestEmitter<T>(child);
        LatestSubscriber<T> parent = new LatestSubscriber<T>(producer);
        producer.parent = parent;
        child.add(parent);
        child.add(producer);
        child.setProducer(producer);
        return parent;
    }
    /**
     * A terminable producer which emits the latest items on request.
     * @param <T>
     */
    static final class LatestEmitter<T> extends AtomicLong implements Producer, Subscription, Observer<T> {
        /** */
        private static final long serialVersionUID = -1364393685005146274L;
        final Subscriber<? super T> child;
        LatestSubscriber<? super T> parent;
        final AtomicReference<Object> value;
        /** Written before done, read after done. */
        Throwable terminal;
        volatile boolean done;
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        boolean missed;
        static final Object EMPTY = new Object();
        static final long NOT_REQUESTED = Long.MIN_VALUE / 2;
        public LatestEmitter(Subscriber<? super T> child) {
            this.child = child;
            this.value = new AtomicReference<Object>(EMPTY);
            this.lazySet(NOT_REQUESTED); // not 
        }
        @Override
        public void request(long n) {
            if (n >= 0) {
                for (;;) {
                    long r = get();
                    if (r == Long.MIN_VALUE) {
                        return;
                    }
                    long u;
                    if (r == NOT_REQUESTED) {
                        u = n;
                    } else {
                        u = r + n;
                        if (u < 0) {
                            u = Long.MAX_VALUE;
                        }
                    }
                    if (compareAndSet(r, u)) {
                        if (r == NOT_REQUESTED) {
                            parent.requestMore(Long.MAX_VALUE);
                        }
                        emit();
                        return;
                    }
                }
            }
        }
        long produced(long n) {
            for (;;) {
                long r = get();
                if (r < 0) {
                    return r;
                }
                long u = r - n;
                if (compareAndSet(r, u)) {
                    return u;
                }
            }
        }
        @Override
        public boolean isUnsubscribed() {
            return get() == Long.MIN_VALUE;
        }
        @Override
        public void unsubscribe() {
            if (get() >= 0) {
                getAndSet(Long.MIN_VALUE);
            }
        }
        
        @Override
        public void onNext(T t) {
            value.lazySet(t); // emit's synchronized block does a full release
            emit();
        }
        @Override
        public void onError(Throwable e) {
            terminal = e;
            done = true;
            emit();
        }
        @Override
        public void onCompleted() {
            done = true;
            emit();
        }
        void emit() {
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
                    if (r == Long.MIN_VALUE) {
                        skipFinal = true;
                        break;
                    }
                    Object v = value.get();
                    if (r > 0 && v != EMPTY) {
                        @SuppressWarnings("unchecked")
                        T v2 = (T)v;
                        child.onNext(v2);
                        value.compareAndSet(v, EMPTY);
                        produced(1);
                        v = EMPTY;
                    }
                    if (v == EMPTY && done) {
                        Throwable e = terminal;
                        if (e != null) {
                            child.onError(e);
                        } else {
                            child.onCompleted();
                        }
                    }
                    synchronized (this) {
                        if (!missed) {
                            emitting = false;
                            skipFinal = true;
                            break;
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
    }
    static final class LatestSubscriber<T> extends Subscriber<T> {
        private final LatestEmitter<T> producer;

        LatestSubscriber(LatestEmitter<T> producer) {
            this.producer = producer;
        }

        @Override
        public void onStart() {
            // don't run until the child actually requested to avoid synchronous problems
            request(0); 
        }

        @Override
        public void onNext(T t) {
            producer.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            producer.onError(e);
        }

        @Override
        public void onCompleted() {
            producer.onCompleted();
        }
        void requestMore(long n) {
            request(n);
        }
    }
}
