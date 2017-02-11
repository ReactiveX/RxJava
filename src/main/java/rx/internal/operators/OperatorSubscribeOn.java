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

import rx.*;
import rx.Observable.OnSubscribe;
import rx.Scheduler.Worker;
import rx.functions.Action0;

/**
 * Subscribes Observers on the specified {@code Scheduler}.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/subscribeOn.png" alt="">
 *
 * @param <T> the value type of the actual source
 */
public final class OperatorSubscribeOn<T> implements OnSubscribe<T> {

    final Scheduler scheduler;
    final Observable<T> source;
    final boolean requestOn;

    public OperatorSubscribeOn(Observable<T> source, Scheduler scheduler, boolean requestOn) {
        this.scheduler = scheduler;
        this.source = source;
        this.requestOn = requestOn;
    }

    @Override
    public void call(final Subscriber<? super T> subscriber) {
        final Worker inner = scheduler.createWorker();

        SubscribeOnSubscriber<T> parent = new SubscribeOnSubscriber<T>(subscriber, requestOn, inner, source);
        subscriber.add(parent);
        subscriber.add(inner);

        inner.schedule(parent);
    }

    static final class SubscribeOnSubscriber<T> extends Subscriber<T> implements Action0 {

        final Subscriber<? super T> actual;

        final boolean requestOn;

        final Worker worker;

        Observable<T> source;

        Thread t;

        SubscribeOnSubscriber(Subscriber<? super T> actual, boolean requestOn, Worker worker, Observable<T> source) {
            this.actual = actual;
            this.requestOn = requestOn;
            this.worker = worker;
            this.source = source;
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            try {
                actual.onError(e);
            } finally {
                worker.unsubscribe();
            }
        }

        @Override
        public void onCompleted() {
            try {
                actual.onCompleted();
            } finally {
                worker.unsubscribe();
            }
        }

        @Override
        public void call() {
            Observable<T> src = source;
            source = null;
            t = Thread.currentThread();
            src.unsafeSubscribe(this);
        }

        @Override
        public void setProducer(final Producer p) {
            actual.setProducer(new Producer() {
                @Override
                public void request(final long n) {
                    if (t == Thread.currentThread() || !requestOn) {
                        p.request(n);
                    } else {
                        worker.schedule(new Action0() {
                            @Override
                            public void call() {
                                p.request(n);
                            }
                        });
                    }
                }
            });
        }
    }
}