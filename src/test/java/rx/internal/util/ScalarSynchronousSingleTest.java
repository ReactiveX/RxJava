/**
 * Copyright 2014 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.util;

import org.junit.Test;

import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ScalarSynchronousSingleTest {
    @Test
    public void backPressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Single.just(1).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);

        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();

        ts.requestMore(1);

        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test(timeout = 1000)
    public void backPressureSubscribeOn() throws Exception {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Single.just(1).subscribeOn(Schedulers.computation()).subscribe(ts);

        Thread.sleep(200);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);

        ts.awaitTerminalEvent();

        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test(timeout = 1000)
    public void backPressureObserveOn() throws Exception {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Single.just(1).observeOn(Schedulers.computation()).subscribe(ts);

        Thread.sleep(200);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);

        ts.awaitTerminalEvent();

        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test(timeout = 1000)
    public void backPressureSubscribeOn2() throws Exception {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Single.just(1).subscribeOn(Schedulers.newThread()).subscribe(ts);

        Thread.sleep(200);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);

        ts.awaitTerminalEvent();

        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test(timeout = 1000)
    public void backPressureObserveOn2() throws Exception {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Single.just(1).observeOn(Schedulers.newThread()).subscribe(ts);

        Thread.sleep(200);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);

        ts.awaitTerminalEvent();

        ts.assertValue(1);
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void backPressureFlatMapJust() {
        TestSubscriber<String> ts = TestSubscriber.create(0);

        Single.just(1).flatMap(new Func1<Integer, Single<String>>() {
            @Override
            public Single<String> call(Integer v) {
                return Single.just(String.valueOf(v));
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);

        ts.assertValue("1");
        ts.assertCompleted();
        ts.assertNoErrors();

        ts.requestMore(1);

        ts.assertValue("1");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void syncObserverNextThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };

        Single.just(1).unsafeSubscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void syncFlatMapJustObserverNextThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };

        Single.just(1)
                .flatMap(new Func1<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> call(Integer v) {
                        return Single.just(v);
                    }
                })
                .unsafeSubscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test(timeout = 1000)
    public void asyncObserverNextThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };

        Single.just(1).subscribeOn(Schedulers.computation()).unsafeSubscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void scalarFlatMap() {
        final Action0 unSubscribe = mock(Action0.class);
        Single<Integer> s = Single.create(new Single.OnSubscribe<Integer>() {
            @Override
            public void call(SingleSubscriber<? super Integer> subscriber) {
                subscriber.add(Subscriptions.create(unSubscribe));
            }
        });
        Subscription subscription = Single.merge(Single.just(s)).subscribe();
        subscription.unsubscribe();
        verify(unSubscribe).call();
    }

    @Test
    public void scalarFlatMapError() {
        final Throwable error = new IllegalStateException();
        Single<Integer> s = Single.just(1);
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        s.flatMap(new Func1<Integer, Single<String>>() {
            @Override
            public Single<String> call(Integer integer) {
                return Single.create(new Single.OnSubscribe<String>() {
                    @Override
                    public void call(SingleSubscriber<? super String> singleSubscriber) {
                        singleSubscriber.onError(error);
                    }
                });
            }
        }).subscribe(testSubscriber);
        testSubscriber.assertNoValues();
        testSubscriber.assertError(error);
    }

    @Test
    public void scalarFlatMapSuccess() {
        Single<Integer> s = Single.just(1);
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        s.flatMap(new Func1<Integer, Single<String>>() {
            @Override
            public Single<String> call(final Integer integer) {
                return Single.create(new Single.OnSubscribe<String>() {
                    @Override
                    public void call(SingleSubscriber<? super String> singleSubscriber) {
                        singleSubscriber.onSuccess(String.valueOf(integer));
                    }
                });
            }
        }).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue("1");
    }

    @Test
    public void getValue() {
        Single<Integer> s = Single.just(1);
        assertEquals(1, ((ScalarSynchronousSingle<?>) s).get());
    }
}