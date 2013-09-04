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
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * Turns all of the notifications from an Observable into <code>onNext</code> emissions, and marks
 * them with their original notification types within {@link Notification} objects.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/materialize.png">
 * <p>
 * See <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">here</a> for the
 * Microsoft Rx equivalent.
 */
public final class OperationMaterialize {

    /**
     * Materializes the implicit notifications of an observable sequence as explicit notification values.
     * 
     * @param sequence
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">Observable.Materialize(TSource) Method </a>
     */
    public static <T> OnSubscribeFunc<Notification<T>> materialize(final Observable<? extends T> sequence) {
        return new MaterializeObservable<T>(sequence);
    }

    private static class MaterializeObservable<T> implements OnSubscribeFunc<Notification<T>> {

        private final Observable<? extends T> sequence;

        public MaterializeObservable(Observable<? extends T> sequence) {
            this.sequence = sequence;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Notification<T>> observer) {
            return sequence.subscribe(new Observer<T>() {

                @Override
                public void onCompleted() {
                    observer.onNext(new Notification<T>());
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
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
            Observable<Notification<String>> m = Observable.create(materialize(Observable.create(o1)));
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
            assertEquals(NullPointerException.class, Observer.notifications.get(2).getThrowable().getClass());
            assertTrue(Observer.notifications.get(2).isOnError());
        }

        @Test
        public void testMaterialize2() {
            final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", "three");

            TestObserver Observer = new TestObserver();
            Observable<Notification<String>> m = Observable.create(materialize(Observable.create(o1)));
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
        public void testMultipleSubscribes() throws InterruptedException, ExecutionException {
            final TestAsyncErrorObservable o = new TestAsyncErrorObservable("one", "two", null, "three");

            Observable<Notification<String>> m = Observable.create(materialize(Observable.create(o)));

            assertEquals(3, m.toList().toBlockingObservable().toFuture().get().size());
            assertEquals(3, m.toList().toBlockingObservable().toFuture().get().size());
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
        public void onError(Throwable e) {
            this.onError = true;
        }

        @Override
        public void onNext(Notification<String> value) {
            this.notifications.add(value);
        }

    }

    private static class TestAsyncErrorObservable implements OnSubscribeFunc<String> {

        String[] valuesToReturn;

        TestAsyncErrorObservable(String... values) {
            valuesToReturn = values;
        }

        volatile Thread t;

        @Override
        public Subscription onSubscribe(final Observer<? super String> observer) {
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    for (String s : valuesToReturn) {
                        if (s == null) {
                            System.out.println("throwing exception");
                            try {
                                Thread.sleep(100);
                            } catch (Throwable e) {

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
