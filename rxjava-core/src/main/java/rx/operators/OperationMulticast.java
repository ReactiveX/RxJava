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

import static org.mockito.Mockito.*;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;
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

    public static class UnitTest {

        @Test
        public void testMulticast() {
            Subject<String, String> source = PublishSubject.create();

            ConnectableObservable<String> multicasted = OperationMulticast.multicast(source,
                    PublishSubject.<String>create());

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            multicasted.subscribe(observer);

            source.onNext("one");
            source.onNext("two");

            multicasted.connect();

            source.onNext("three");
            source.onNext("four");
            source.onCompleted();

            verify(observer, never()).onNext("one");
            verify(observer, never()).onNext("two");
            verify(observer, times(1)).onNext("three");
            verify(observer, times(1)).onNext("four");
            verify(observer, times(1)).onCompleted();

        }

        @Test
        public void testMulticastConnectTwice() {
            Subject<String, String> source = PublishSubject.create();

            ConnectableObservable<String> multicasted = OperationMulticast.multicast(source,
                    PublishSubject.<String>create());

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            multicasted.subscribe(observer);

            source.onNext("one");

            multicasted.connect();
            multicasted.connect();

            source.onNext("two");
            source.onCompleted();

            verify(observer, never()).onNext("one");
            verify(observer, times(1)).onNext("two");
            verify(observer, times(1)).onCompleted();

        }

        @Test
        public void testMulticastDisconnect() {
            Subject<String, String> source = PublishSubject.create();

            ConnectableObservable<String> multicasted = OperationMulticast.multicast(source,
                    PublishSubject.<String>create());

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            multicasted.subscribe(observer);

            source.onNext("one");

            Subscription connection = multicasted.connect();
            source.onNext("two");

            connection.unsubscribe();
            source.onNext("three");

            multicasted.connect();
            source.onNext("four");
            source.onCompleted();

            verify(observer, never()).onNext("one");
            verify(observer, times(1)).onNext("two");
            verify(observer, never()).onNext("three");
            verify(observer, times(1)).onNext("four");
            verify(observer, times(1)).onCompleted();

        }

    }
}
