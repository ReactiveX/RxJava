/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subjects.Subject;

public class OperationMulticast {
    public static <T, R> ConnectableObservable<R> multicast(Observable<? extends T> source, final Subject<? super T, ? extends R> subject) {
        return new MulticastConnectableObservable<T, R>(source, subject);
    }

    private static class MulticastConnectableObservable<T, R> extends ConnectableObservable<R> {
        private final Object lock = new Object();

        private final Observable<? extends T> source;
        private final Subject<? super T, ? extends R> subject;

        private Subscription subscription;

        public MulticastConnectableObservable(Observable<? extends T> source, final Subject<? super T, ? extends R> subject) {
            super(new OnSubscribeFunc<R>() {
                @Override
                public Subscription onSubscribe(Observer<? super R> observer) {
                    return subject.subscribe(observer);
                }
            });
            this.source = source;
            this.subject = subject;
        }

        public Subscription connect() {
            synchronized (lock) {
                if (subscription == null) {
                    subscription = source.subscribe(new Observer<T>() {
                        @Override
                        public void onCompleted() {
                            subject.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            subject.onError(e);
                        }

                        @Override
                        public void onNext(T args) {
                            subject.onNext(args);
                        }
                    });
                }
            }

            return new Subscription() {
                @Override
                public void unsubscribe() {
                    synchronized (lock) {
                        if (subscription != null) {
                            subscription.unsubscribe();
                            subscription = null;
                        }
                    }
                }
            };
        }

    }
}
