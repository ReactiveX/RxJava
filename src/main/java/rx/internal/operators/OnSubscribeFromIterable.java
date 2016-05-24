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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.Exceptions;

/**
 * Converts an {@code Iterable} sequence into an {@code Observable}.
 * <p>
 * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toObservable.png" alt="" />
 * <p>
 * You can convert any object that supports the Iterable interface into an Observable that emits each item in
 * the object, with the {@code toObservable} operation.
 * @param <T> the value type of the items
 */
public final class OnSubscribeFromIterable<T> implements OnSubscribe<T> {

    final Iterable<? extends T> is;

    public OnSubscribeFromIterable(Iterable<? extends T> iterable) {
        if (iterable == null) {
            throw new NullPointerException("iterable must not be null");
        }
        this.is = iterable;
    }

    @Override
    public void call(final Subscriber<? super T> o) {
        final Iterator<? extends T> it;
        boolean b;
        
        try {
            it = is.iterator();
            
            b = it.hasNext();
        } catch (Throwable ex) {
            Exceptions.throwOrReport(ex, o);
            return;
        }
            
        if (!o.isUnsubscribed()) {
            if (!b) {
                o.onCompleted();
            } else { 
                o.setProducer(new IterableProducer<T>(o, it));
            }
        }
    }

    static final class IterableProducer<T> extends AtomicLong implements Producer {
        /** */
        private static final long serialVersionUID = -8730475647105475802L;
        private final Subscriber<? super T> o;
        private final Iterator<? extends T> it;

        IterableProducer(Subscriber<? super T> o, Iterator<? extends T> it) {
            this.o = o;
            this.it = it;
        }

        @Override
        public void request(long n) {
            if (get() == Long.MAX_VALUE) {
                // already started with fast-path
                return;
            }
            if (n == Long.MAX_VALUE && compareAndSet(0, Long.MAX_VALUE)) {
                fastpath();
            } else 
            if (n > 0 && BackpressureUtils.getAndAddRequest(this, n) == 0L) {
                slowpath(n);
            }

        }

        void slowpath(long n) {
            // backpressure is requested
            final Subscriber<? super T> o = this.o;
            final Iterator<? extends T> it = this.it;

            long r = n;
            long e = 0;
            
            for (;;) {
                while (e != r) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    
                    T value;
                    
                    try {
                        value = it.next();
                    } catch (Throwable ex) {
                        Exceptions.throwOrReport(ex, o);
                        return;
                    }
                    
                    o.onNext(value);

                    if (o.isUnsubscribed()) {
                        return;
                    }

                    boolean b;
                    
                    try {
                        b = it.hasNext();
                    } catch (Throwable ex) {
                        Exceptions.throwOrReport(ex, o);
                        return;
                    }
                    
                    if (!b) {
                        if (!o.isUnsubscribed()) {
                            o.onCompleted();
                        }
                        return;
                    }
                    
                    e++;
                }
                
                r = get();
                if (e == r) {
                    r = BackpressureUtils.produced(this, e);
                    if (r == 0L) {
                        break;
                    }
                    e = 0L;
                }
            }
            
        }

        void fastpath() {
            // fast-path without backpressure
            final Subscriber<? super T> o = this.o;
            final Iterator<? extends T> it = this.it;

            for (;;) {
                if (o.isUnsubscribed()) {
                    return;
                }
                
                T value;

                try {
                    value = it.next();
                } catch (Throwable ex) {
                    Exceptions.throwOrReport(ex, o);
                    return;
                }
                
                o.onNext(value);

                if (o.isUnsubscribed()) {
                    return;
                }

                boolean b;

                try {
                    b  = it.hasNext();
                } catch (Throwable ex) {
                    Exceptions.throwOrReport(ex, o);
                    return;
                }

                if (!b) {
                    if (!o.isUnsubscribed()) {
                        o.onCompleted();
                    }
                    return;
                }
            }
        }
    }

}
