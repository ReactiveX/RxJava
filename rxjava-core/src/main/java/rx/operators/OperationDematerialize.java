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

import static org.junit.Assert.*;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

import java.util.List;
import java.util.Vector;

/**
 * Dematerializes the explicit notification values of an observable sequence as implicit notifications.
 * See http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx for the Microsoft Rx equivalent.
 */
public final class OperationDematerialize {

    /**
     * Dematerializes the explicit notification values of an observable sequence as implicit notifications.
     *
     * @param sequence
     *            An observable sequence containing explicit notification values which have to be turned into implicit notifications.
     * @return An observable sequence exhibiting the behavior corresponding to the source sequence's notification values.
     * @see http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx
     */
    public static <T> Func1<Observer<T>, Subscription> dematerialize(final Observable<Notification<T>> sequence) {
        return new DematerializeObservable<T>(sequence);
    }

    private static class DematerializeObservable<T> implements Func1<Observer<T>, Subscription> {

        private final Observable<Notification<T>> sequence;

        public DematerializeObservable(Observable<Notification<T>> sequence) {
            this.sequence = sequence;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            return sequence.subscribe(new Observer<Notification<T>>() {
                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Exception e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(Notification<T> value) {
                    switch (value.getKind()) {
                        case OnNext:
                            observer.onNext(value.getValue());
                            break;
                        case OnError:
                            observer.onError(value.getException());
                            break;
                        case OnCompleted:
                            observer.onCompleted();
                            break;
                    }
                }
            });
        }
    }

    public static class UnitTest {

        @Test
        public void testDematerialize1() {

            Observable<String> observable = Observable.just("test");
            Observable<String> dematerializedObservable = Observable.create(dematerialize(observable.materialize()));

            TestObserver testObserver = new TestObserver();
            dematerializedObservable.subscribe(testObserver);

            assertFalse(testObserver.onError);
            assertTrue(testObserver.onCompleted);
            assertTrue(testObserver.notifications.size() == 1);
            assertTrue(testObserver.notifications.get(0) == "test");
        }

        @Test
        public void testDematerialize2() {
            Exception exception = new Exception("test");
            Observable<String> observable = Observable.error(exception);
            Observable<String> dematerializedObservable = Observable.create(dematerialize(observable.materialize()));

            TestObserver testObserver = new TestObserver();
            dematerializedObservable.subscribe(testObserver);

            assertTrue(testObserver.onError);
            assertTrue(exception == testObserver.exception);
            assertFalse(testObserver.onCompleted);
            assertTrue(testObserver.notifications.isEmpty());
        }

        private static class TestObserver implements Observer<String> {

            boolean onCompleted = false;
            boolean onError = false;
            Exception exception = null;
            List<String> notifications = new Vector<String>();

            @Override
            public void onCompleted() {
                this.onCompleted = true;
            }

            @Override
            public void onError(Exception e) {
                this.onError = true;
                exception = e;
            }

            @Override
            public void onNext(String value) {
                this.notifications.add(value);
            }

        }
    }
}
