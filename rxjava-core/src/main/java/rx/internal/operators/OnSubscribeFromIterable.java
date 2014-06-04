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
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * Converts an Iterable sequence into an Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/toObservable.png">
 * <p>
 * You can convert any object that supports the Iterable interface into an Observable that emits
 * each item in the object, with the toObservable operation.
 */
public final class OnSubscribeFromIterable<T> implements OnSubscribe<T> {

    final Iterable<? extends T> is;

    public OnSubscribeFromIterable(Iterable<? extends T> iterable) {
        this.is = iterable;
    }

    @Override
    public void call(final Subscriber<? super T> o) {
        final Iterator<? extends T> it = is.iterator();
        // TODO make a fast-path if site of Iterable is less than min buffer size so we avoid trampoline and producer in that case
        final Scheduler.Worker trampoline = Schedulers.trampoline().createWorker();
        o.add(trampoline);
        o.setProducer(new Producer() {
            // TODO migrate to AFU
            final AtomicInteger requested = new AtomicInteger();

            @Override
            public void request(int n) {
                int _c = requested.getAndAdd(n);
                if (_c == 0) {
                    // it was 0 (not running) so start it
                    trampoline.schedule(new Action0() {

                        @Override
                        public void call() {
                            while (it.hasNext()) {
                                if (o.isUnsubscribed()) {
                                    return;
                                }
                                int c = requested.decrementAndGet();
                                T t = it.next();
                                o.onNext(t);
                                if (c == 0) {
                                    // we're done emitting the number requested so return
                                    return;
                                }
                            }

                            if (o.isUnsubscribed()) {
                                return;
                            }
                            o.onCompleted();
                        }

                    });
                }
            }

        });
    }

}
