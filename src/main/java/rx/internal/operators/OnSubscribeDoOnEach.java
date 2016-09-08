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

import java.util.Arrays;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.*;
import rx.plugins.RxJavaHooks;

/**
 * Calls specified actions for each notification.
 *
 * @param <T> the value type
 */
public class OnSubscribeDoOnEach<T> implements OnSubscribe<T> {
    private final Observer<? super T> doOnEachObserver;
    private final Observable<T> source;

    public OnSubscribeDoOnEach(Observable<T> source, Observer<? super T> doOnEachObserver) {
        this.source = source;
        this.doOnEachObserver = doOnEachObserver;
    }

    @Override
    public void call(final Subscriber<? super T> subscriber) {
        source.unsafeSubscribe(new DoOnEachSubscriber<T>(subscriber, doOnEachObserver));
    }

    private static final class DoOnEachSubscriber<T> extends Subscriber<T> {

        private final Subscriber<? super T> subscriber;
        private final Observer<? super T> doOnEachObserver;

        private boolean done;

        DoOnEachSubscriber(Subscriber<? super T> subscriber, Observer<? super T> doOnEachObserver) {
            super(subscriber);
            this.subscriber = subscriber;
            this.doOnEachObserver = doOnEachObserver;
        }

        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            try {
                doOnEachObserver.onCompleted();
            } catch (Throwable e) {
                Exceptions.throwOrReport(e, this);
                return;
            }
            // Set `done` here so that the error in `doOnEachObserver.onCompleted()` can be noticed by observer
            done = true;
            subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaHooks.onError(e);
                return;
            }
            done = true;
            try {
                doOnEachObserver.onError(e);
            } catch (Throwable e2) {
                Exceptions.throwIfFatal(e2);
                subscriber.onError(new CompositeException(Arrays.asList(e, e2)));
                return;
            }
            subscriber.onError(e);
        }

        @Override
        public void onNext(T value) {
            if (done) {
                return;
            }
            try {
                doOnEachObserver.onNext(value);
            } catch (Throwable e) {
                Exceptions.throwOrReport(e, this, value);
                return;
            }
            subscriber.onNext(value);
        }
    }
}