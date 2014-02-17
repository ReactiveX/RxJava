package rx.operators;

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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class OperationRetry {

    private static final int INFINITE_RETRY = -1;

    public static <T> OnSubscribeFunc<T> retry(final Observable<T> observable, final int maxRetries) {
        return new Retry<T>(observable, maxRetries);
    }

    public static <T> OnSubscribeFunc<T> retry(final Observable<T> observable) {
        return new Retry<T>(observable, INFINITE_RETRY);
    }

    private static class Retry<T> implements OnSubscribeFunc<T> {

        private final Observable<T> source;
        private final int maxRetries;

        Retry(Observable<T> source, int maxRetries) {
            this.source = source;
            this.maxRetries = maxRetries;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            return new RetrySubscription<T>(source, observer, maxRetries);
        }
    }

    private static class RetrySubscription<T> implements Subscription {

        private final Observable<T> source;
        private final Observer<? super T> observer;
        private final AtomicReference<Subscription> sourceSubscription = new AtomicReference<Subscription>(
                Subscriptions.empty());

        private final AtomicInteger attempts;
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        private final int maxRetries;
        /**
         * This lock guards the suscribe/unsubscribe to source actions so that
         * an unsubscribe+subscribe pair is unaffected by a concurrent
         * unsubscribe and vic-versa.
         */
        private final Object lock = new Object();

        RetrySubscription(Observable<T> source, Observer<? super T> observer, int maxRetries) {
            this.source = source;
            this.observer = observer;
            this.attempts = new AtomicInteger(0);
            this.maxRetries = maxRetries;
            subscribeToSource();
        }

        private void subscribeToSource() {
            sourceSubscription.set(source.subscribe(createObserver()));
            subscribed.set(true);
        }

        private void unsubscribeFromSource() {
            // only unsubscribe once
            if (subscribed.get()) {
                sourceSubscription.get().unsubscribe();
                subscribed.set(false);
            }
        }

        @Override
        public void unsubscribe() {
            synchronized (lock) {
                unsubscribeFromSource();
            }
        }

        private Observer<? super T> createObserver() {
            return new Observer<T>() {

                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    if (attempts.incrementAndGet() <= maxRetries || maxRetries == INFINITE_RETRY) {
                        synchronized (lock) {
                            unsubscribeFromSource();
                            subscribeToSource();
                        }
                    } else
                        observer.onError(e);
                }

                @Override
                public void onNext(T t) {
                    observer.onNext(t);
                }
            };
        }
    }
}