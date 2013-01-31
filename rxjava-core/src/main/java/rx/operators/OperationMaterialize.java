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

import java.util.List;
import java.util.Vector;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

/**
 * Materializes the implicit notifications of an observable sequence as explicit notification values.
 * <p>
 * In other words, converts a sequence of OnNext, OnError and OnCompleted events into a sequence of ObservableNotifications containing the OnNext, OnError and OnCompleted values.
 * <p>
 * See http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx for the Microsoft Rx equivalent.
 */
public final class OperationMaterialize {

    /**
     * Materializes the implicit notifications of an observable sequence as explicit notification values.
     * 
     * @param source
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx
     */
    public static <T> Func1<Observer<Notification<T>>, Subscription> materialize(final Observable<T> sequence) {
        return new MaterializeObservable<T>(sequence);
    }

    private static class MaterializeObservable<T> implements Func1<Observer<Notification<T>>, Subscription> {

        private final Observable<T> sequence;

        public MaterializeObservable(Observable<T> sequence) {
            this.sequence = sequence;
        }

        @Override
        public Subscription call(final Observer<Notification<T>> observer) {
            return sequence.subscribe(new Observer<T>() {

                @Override
                public void onCompleted() {
                    observer.onNext(new Notification<T>());
                    observer.onCompleted();
                }

                @Override
                public void onError(Exception e) {
                    observer.onNext(new Notification<T>(e));
                    observer.onCompleted();
                }

                @Override
                public void onNext(T value) {
                    observer.onNext(new Notification<T>(value));
                }

            });
        }

    }

    public static class UnitTest {
        @Test
        public void testMaterialize1() {
            // null will cause onError to be triggered before "three" can be returned
            final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", null, "three");

            TestObserver Observer = new TestObserver();
            Observable<Notification<String>> m = Observable.create(materialize(o1));
            m.subscribe(Observer);

            try {
                o1.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertFalse(Observer.onError);
            assertTrue(Observer.onCompleted);
            assertEquals(3, Observer.notifications.size());
            assertEquals("one", Observer.notifications.get(0).getValue());
            assertTrue(Observer.notifications.get(0).isOnNext());
            assertEquals("two", Observer.notifications.get(1).getValue());
            assertTrue(Observer.notifications.get(1).isOnNext());
            assertEquals(NullPointerException.class, Observer.notifications.get(2).getException().getClass());
            assertTrue(Observer.notifications.get(2).isOnError());
        }

        @Test
        public void testMaterialize2() {
            final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", "three");

            TestObserver Observer = new TestObserver();
            Observable<Notification<String>> m = Observable.create(materialize(o1));
            m.subscribe(Observer);

            try {
                o1.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertFalse(Observer.onError);
            assertTrue(Observer.onCompleted);
            assertEquals(4, Observer.notifications.size());
            assertEquals("one", Observer.notifications.get(0).getValue());
            assertTrue(Observer.notifications.get(0).isOnNext());
            assertEquals("two", Observer.notifications.get(1).getValue());
            assertTrue(Observer.notifications.get(1).isOnNext());
            assertEquals("three", Observer.notifications.get(2).getValue());
            assertTrue(Observer.notifications.get(2).isOnNext());
            assertTrue(Observer.notifications.get(3).isOnCompleted());
        }

        @Test
        public void testMultipleSubscribes() {
            final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", null, "three");

            Observable<Notification<String>> m = Observable.create(materialize(o1));

            TestObserver Observer1 = new TestObserver();
            m.subscribe(Observer1);

            TestObserver Observer2 = new TestObserver();
            m.subscribe(Observer2);

            try {
                o1.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals(3, Observer1.notifications.size());
            assertEquals(3, Observer2.notifications.size());
        }

    }

    private static class TestObserver implements Observer<Notification<String>> {

        boolean onCompleted = false;
        boolean onError = false;
        List<Notification<String>> notifications = new Vector<Notification<String>>();

        @Override
        public void onCompleted() {
            this.onCompleted = true;
        }

        @Override
        public void onError(Exception e) {
            this.onError = true;
        }

        @Override
        public void onNext(Notification<String> value) {
            this.notifications.add(value);
        }

    }

    private static class TestAsyncErrorObservable extends Observable<String> {

        String[] valuesToReturn;

        TestAsyncErrorObservable(String... values) {
            valuesToReturn = values;
        }

        Thread t;

        @Override
        public Subscription subscribe(final Observer<String> observer) {
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    for (String s : valuesToReturn) {
                        if (s == null) {
                            System.out.println("throwing exception");
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {

                            }
                            observer.onError(new NullPointerException());
                            return;
                        } else {
                            observer.onNext(s);
                        }
                    }
                    System.out.println("subscription complete");
                    observer.onCompleted();
                }

            });
            t.start();

            return new Subscription() {

                @Override
                public void unsubscribe() {

                }

            };
        }
    }
}
