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

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.internal.util.RxRingBuffer;

/**
 * Converts an {@code Iterable} sequence into an {@code Observable}.
 * <p>
 * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/Netflix/RxJava/images/rx-operators/toObservable.png" />
 * <p>
 * You can convert any object that supports the Iterable interface into an Observable that emits each item in
 * the object, with the {@code toObservable} operation.
 */
public final class OnSubscribeFromIterable<T> implements OnSubscribe<T> {

    final Iterable<? extends T> is;

    public OnSubscribeFromIterable(Iterable<? extends T> iterable) {
        this.is = iterable;
    }

    @Override
    public void call(final Subscriber<? super T> o) {
        if (is == null) {
            o.onCompleted();
        }
        final Iterator<? extends T> it = is.iterator();
        if (is instanceof Collection) {
            @SuppressWarnings("rawtypes")
            int size = ((Collection) is).size();
            if (size < Producer.BUFFER_SIZE) {
                while (it.hasNext()) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    T t = it.next();
                    o.onNext(t);
                }
                o.onCompleted();
                return;
            }
        }
        // otherwise we do it via the producer to support backpressure
        o.setProducer(new IterableProducer<T>(o, it));
    }

    private static final class IterableProducer<T> implements Producer {
        private final Subscriber<? super T> o;
        private final Iterator<? extends T> it;

        private volatile int requested = 0;
        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<IterableProducer> REQUESTED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(IterableProducer.class, "requested");

        private IterableProducer(Subscriber<? super T> o, Iterator<? extends T> it) {
            this.o = o;
            this.it = it;
        }

        @Override
        public void request(int n) {
            int _c = REQUESTED_UPDATER.getAndAdd(this, n);
            if (_c == 0) {
                while (it.hasNext()) {
                    if (o.isUnsubscribed()) {
                        return;
                    }
                    T t = it.next();
                    o.onNext(t);
                    if (REQUESTED_UPDATER.decrementAndGet(this) == 0) {
                        // we're done emitting the number requested so return
                        return;
                    }
                }

                o.onCompleted();
            }

        }
    }

}
