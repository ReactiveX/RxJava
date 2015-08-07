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

/**
 * Converts an {@code Iterable} sequence into an {@code Observable}.
 * <p>
 * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toObservable.png" alt="" />
 * <p>
 * You can convert any object that supports the Iterable interface into an Observable that emits each item in
 * the object, with the {@code toObservable} operation.
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
        final Iterator<? extends T> it = is.iterator();
        if (!it.hasNext() && !o.isUnsubscribed())
            o.onCompleted();
        else 
            o.setProducer(new IterableProducer<T>(o, it));
    }

    private static final class IterableProducer<T> extends AtomicLong implements Producer {
        /** */
        private static final long serialVersionUID = -8730475647105475802L;
        private final Subscriber<? super T> o;
        private final Iterator<? extends T> it;

        private IterableProducer(Subscriber<? super T> o, Iterator<? extends T> it) {
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
            while (true) {
                /*
                 * This complicated logic is done to avoid touching the
                 * volatile `requested` value during the loop itself. If
                 * it is touched during the loop the performance is
                 * impacted significantly.
                 */
                long numToEmit = r;
                while (true) {
                    if (o.isUnsubscribed()) {
                        return;
                    } else if (it.hasNext()) {
                        if (--numToEmit >= 0) {
                            o.onNext(it.next());
                        } else
                            break;
                    } else if (!o.isUnsubscribed()) {
                        o.onCompleted();
                        return;
                    } else {
                        // is unsubscribed
                        return;
                    }
                }
                r = addAndGet(-r);
                if (r == 0L) {
                    // we're done emitting the number requested so
                    // return
                    return;
                }

            }
        }

        void fastpath() {
            // fast-path without backpressure
            final Subscriber<? super T> o = this.o;
            final Iterator<? extends T> it = this.it;

            while (true) {
                if (o.isUnsubscribed()) {
                    return;
                } else if (it.hasNext()) {
                    o.onNext(it.next());
                } else if (!o.isUnsubscribed()) {
                    o.onCompleted();
                    return;
                } else {
                    // is unsubscribed
                    return;
                }
            }
        }
    }

}
