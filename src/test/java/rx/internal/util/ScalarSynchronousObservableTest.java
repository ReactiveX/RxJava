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

package rx.internal.util;

import java.util.concurrent.atomic.*;

import org.junit.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.TestException;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.plugins.*;
import rx.schedulers.Schedulers;

public class ScalarSynchronousObservableTest {
    @Test
    public void testBackpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Observable.just(1).subscribe(ts);

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
    public void testBackpressureSubscribeOn() throws Exception {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Observable.just(1).subscribeOn(Schedulers.computation()).subscribe(ts);

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
    public void testBackpressureObserveOn() throws Exception {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Observable.just(1).observeOn(Schedulers.computation()).subscribe(ts);

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
    public void testBackpressureFlatMapJust() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Observable.just(1).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.just(v);
            }
        }).subscribe(ts);

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

    @Test
    public void testBackpressureFlatMapRange() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Observable.just(1).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);
        ts.assertValues(1, 2);
        ts.assertCompleted();
        ts.assertNoErrors();

        ts.requestMore(1);

        ts.assertValues(1, 2);
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void emptiesAndJust() {
        TestSubscriber<Object> ts = TestSubscriber.create();

        Observable.just(1)
        .flatMap(new Func1<Integer, Observable<String>>() {
            @Override
            public Observable<String> call(Integer n) {
                return Observable.<String>just(null, null)
                        .filter(new Func1<Object, Boolean>() {
                            @Override
                            public Boolean call(Object o) {
                                return o != null;
                            }
                        })
                        .switchIfEmpty(Observable.<String>empty().switchIfEmpty(Observable.just("Hello")));
            }
        }).subscribe(ts);

        ts.assertValue("Hello");
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void syncObserverNextThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };

        Observable.just(1).unsafeSubscribe(ts);

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

        Observable.just(1)
        .flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.just(v);
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

        Observable.just(1).subscribeOn(Schedulers.computation()).unsafeSubscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test
    public void hookCalled() {
        RxJavaObservableExecutionHook save = ScalarSynchronousObservable.hook;
        try {
            final AtomicInteger c = new AtomicInteger();
            
            ScalarSynchronousObservable.hook = new RxJavaObservableExecutionHook() {
                @Override
                public <T> OnSubscribe<T> onCreate(OnSubscribe<T> f) {
                    c.getAndIncrement();
                    return f;
                }
            };
            
            int n = 10;
            
            for (int i = 0; i < n; i++) {
                Observable.just(1).subscribe();
            }
            
            Assert.assertEquals(n, c.get());
        } finally {
            ScalarSynchronousObservable.hook = save;
        }
    }

    @Test
    public void hookChangesBehavior() {
        RxJavaObservableExecutionHook save = ScalarSynchronousObservable.hook;
        try {
            ScalarSynchronousObservable.hook = new RxJavaObservableExecutionHook() {
                @Override
                public <T> OnSubscribe<T> onCreate(OnSubscribe<T> f) {
                    if (f instanceof ScalarSynchronousObservable.JustOnSubscribe) {
                        final T v = ((ScalarSynchronousObservable.JustOnSubscribe<T>) f).value;
                        return new OnSubscribe<T>() {
                            @Override
                            public void call(Subscriber<? super T> t) {
                                t.onNext(v);
                                t.onNext(v);
                                t.onCompleted();
                            }
                        };
                    }
                    return f;
                }
            };
            
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            
            Observable.just(1).subscribe(ts);
            
            ts.assertValues(1, 1);
            ts.assertNoErrors();
            ts.assertCompleted();
            
        } finally {
            ScalarSynchronousObservable.hook = save;
        }
    }

}