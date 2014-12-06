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
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;

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
        o.setProducer(new IterableProducer<T>(o, it));
    }

    private static final class IterableProducer<T> implements Producer {
        private final Subscriber<? super T> o;
        private final Iterator<? extends T> it;

        private volatile long requested = 0;
        @SuppressWarnings("rawtypes")
        private static final AtomicLongFieldUpdater<IterableProducer> REQUESTED_UPDATER = AtomicLongFieldUpdater.newUpdater(IterableProducer.class, "requested");

        private IterableProducer(Subscriber<? super T> o, Iterator<? extends T> it) {
            this.o = o;
            this.it = it;
        }

        @Override
        public void request(long n) {
            if (REQUESTED_UPDATER.get(this) == Long.MAX_VALUE) {
                // already started with fast-path
                return;
            }
            if (n == Long.MAX_VALUE) {
                REQUESTED_UPDATER.set(this, n);
                // fast-path without backpressure
                while (it.hasNext()) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    o.onNext(it.next());
                }
                if (!o.isUnsubscribed()) {
                    o.onCompleted();
                }
            } else if(n > 0) {
                // backpressure is requested
                long _c = REQUESTED_UPDATER.getAndAdd(this, n);
                if (_c == 0) {
                    while (true) {
                        /*
                         * This complicated logic is done to avoid touching the volatile `requested` value
                         * during the loop itself. If it is touched during the loop the performance is impacted significantly.
                         */
                        long r = requested;
                        long numToEmit = r;
                        while (it.hasNext() && --numToEmit >= 0) {
                            if (o.isUnsubscribed()) {
                                return;
                            }
                            o.onNext(it.next());

                        }

                        if (!it.hasNext()) {
                            if (!o.isUnsubscribed()) {
                                o.onCompleted();
                            }
                            return;
                        }
                        if (REQUESTED_UPDATER.addAndGet(this, -r) == 0) {
                            // we're done emitting the number requested so return
                            return;
                        }

                    }
                }
            }

        }
    }

}
