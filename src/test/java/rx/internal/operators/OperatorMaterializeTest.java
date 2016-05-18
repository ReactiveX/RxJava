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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorMaterializeTest {

    @Test
    public void testMaterialize1() {
        // null will cause onError to be triggered before "three" can be
        // returned
        final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", null,
                "three");

        TestObserver Observer = new TestObserver();
        Observable<Notification<String>> m = Observable.create(o1).materialize();
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
        assertEquals(NullPointerException.class, Observer.notifications.get(2).getThrowable()
                .getClass());
        assertTrue(Observer.notifications.get(2).isOnError());
    }

    @Test
    public void testMaterialize2() {
        final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", "three");

        TestObserver Observer = new TestObserver();
        Observable<Notification<String>> m = Observable.create(o1).materialize();
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

        Observable<Notification<String>> m = Observable.create(o).materialize();

        assertEquals(3, m.toList().toBlocking().toFuture().get().size());
        assertEquals(3, m.toList().toBlocking().toFuture().get().size());
    }

    @Test
    public void testBackpressureOnEmptyStream() {
        TestSubscriber<Notification<Integer>> ts = TestSubscriber.create(0);
        Observable.<Integer> empty().materialize().subscribe(ts);
        ts.assertNoValues();
        ts.requestMore(1);
        ts.assertValueCount(1);
        assertTrue(ts.getOnNextEvents().get(0).isOnCompleted());
        ts.assertCompleted();
    }

    @Test
    public void testBackpressureNoError() {
        TestSubscriber<Notification<Integer>> ts = TestSubscriber.create(0);
        Observable.just(1, 2, 3).materialize().subscribe(ts);
        ts.assertNoValues();
        ts.requestMore(1);
        ts.assertValueCount(1);
        ts.requestMore(2);
        ts.assertValueCount(3);
        ts.requestMore(1);
        ts.assertValueCount(4);
        ts.assertCompleted();
    }
    
    @Test
    public void testBackpressureNoErrorAsync() throws InterruptedException {
        TestSubscriber<Notification<Integer>> ts = TestSubscriber.create(0);
        Observable.just(1, 2, 3)
            .materialize()
            .subscribeOn(Schedulers.computation())
            .subscribe(ts);
        Thread.sleep(100);
        ts.assertNoValues();
        ts.requestMore(1);
        Thread.sleep(100);
        ts.assertValueCount(1);
        ts.requestMore(2);
        Thread.sleep(100);
        ts.assertValueCount(3);
        ts.requestMore(1);
        Thread.sleep(100);
        ts.assertValueCount(4);
        ts.assertCompleted();
    }

    @Test
    public void testBackpressureWithError() {
        TestSubscriber<Notification<Integer>> ts = TestSubscriber.create(0);
        Observable.<Integer> error(new IllegalArgumentException()).materialize().subscribe(ts);
        ts.assertNoValues();
        ts.requestMore(1);
        ts.assertValueCount(1);
        ts.assertCompleted();
    }

    @Test
    public void testBackpressureWithEmissionThenError() {
        TestSubscriber<Notification<Integer>> ts = TestSubscriber.create(0);
        IllegalArgumentException ex = new IllegalArgumentException();
        Observable.from(Arrays.asList(1)).concatWith(Observable.<Integer> error(ex)).materialize()
                .subscribe(ts);
        ts.assertNoValues();
        ts.requestMore(1);
        ts.assertValueCount(1);
        assertTrue(ts.getOnNextEvents().get(0).hasValue());
        ts.requestMore(1);
        ts.assertValueCount(2);
        assertTrue(ts.getOnNextEvents().get(1).isOnError());
        assertTrue(ex == ts.getOnNextEvents().get(1).getThrowable());
        ts.assertCompleted();
    }

    @Test
    public void testWithCompletionCausingError() {
        TestSubscriber<Notification<Integer>> ts = TestSubscriber.create();
        final RuntimeException ex = new RuntimeException("boo");
        Observable.<Integer>empty().materialize().doOnNext(new Action1<Object>() {
            @Override
            public void call(Object t) {
                throw ex;
            }
        }).subscribe(ts);
        ts.assertError(ex);
        ts.assertNoValues();
        ts.assertTerminalEvent();
    }
    
    @Test
    public void testUnsubscribeJustBeforeCompletionNotificationShouldPreventThatNotificationArriving() {
        TestSubscriber<Notification<Integer>> ts = TestSubscriber.create(0);

        Observable.<Integer>empty().materialize()
                .subscribe(ts);
        ts.assertNoValues();
        ts.unsubscribe();
        ts.requestMore(1);
        ts.assertNoValues();
        ts.assertUnsubscribed();
    }

    private static class TestObserver extends Subscriber<Notification<String>> {

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

    private static class TestAsyncErrorObservable implements Observable.OnSubscribe<String> {

        String[] valuesToReturn;

        TestAsyncErrorObservable(String... values) {
            valuesToReturn = values;
        }

        volatile Thread t;

        @Override
        public void call(final Subscriber<? super String> observer) {
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
        }
    }
}
