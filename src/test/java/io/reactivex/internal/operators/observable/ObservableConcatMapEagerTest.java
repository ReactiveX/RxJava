/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.*;

public class ObservableConcatMapEagerTest {

    @Test
    public void normal() {
        Observable.range(1, 5)
        .concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer t) {
                return Observable.range(t, 2);
            }
        })
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    @Ignore("Observable doesn't do backpressure")
    public void normalBackpressured() {
//        TestObserver<Integer> ts = Observable.range(1, 5)
//        .concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
//            @Override
//            public ObservableSource<Integer> apply(Integer t) {
//                return Observable.range(t, 2);
//            }
//        })
//        .test(3);
//
//        ts.assertValues(1, 2, 2);
//
//        ts.request(1);
//
//        ts.assertValues(1, 2, 2, 3);
//
//        ts.request(1);
//
//        ts.assertValues(1, 2, 2, 3, 3);
//
//        ts.request(5);
//
//        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    public void normalDelayBoundary() {
        Observable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer t) {
                return Observable.range(t, 2);
            }
        }, false)
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    @Ignore("Observable doesn't do backpressure")
    public void normalDelayBoundaryBackpressured() {
//        TestObserver<Integer> ts = Observable.range(1, 5)
//        .concatMapEagerDelayError(new Function<Integer, ObservableSource<Integer>>() {
//            @Override
//            public ObservableSource<Integer> apply(Integer t) {
//                return Observable.range(t, 2);
//            }
//        }, false)
//        .test(3);
//
//        ts.assertValues(1, 2, 2);
//
//        ts.request(1);
//
//        ts.assertValues(1, 2, 2, 3);
//
//        ts.request(1);
//
//        ts.assertValues(1, 2, 2, 3, 3);
//
//        ts.request(5);
//
//        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    public void normalDelayEnd() {
        Observable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer t) {
                return Observable.range(t, 2);
            }
        }, true)
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    @Ignore("Observable doesn't do backpressure")
    public void normalDelayEndBackpressured() {
//        TestObserver<Integer> ts = Observable.range(1, 5)
//        .concatMapEagerDelayError(new Function<Integer, ObservableSource<Integer>>() {
//            @Override
//            public ObservableSource<Integer> apply(Integer t) {
//                return Observable.range(t, 2);
//            }
//        }, true)
//        .test(3);
//
//        ts.assertValues(1, 2, 2);
//
//        ts.request(1);
//
//        ts.assertValues(1, 2, 2, 3);
//
//        ts.request(1);
//
//        ts.assertValues(1, 2, 2, 3, 3);
//
//        ts.request(5);
//
//        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }

    @Test
    public void mainErrorsDelayBoundary() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserver<Integer> ts = main.concatMapEagerDelayError(
                new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer t) {
                        return inner;
                    }
                }, false).test();

        main.onNext(1);

        inner.onNext(2);

        ts.assertValue(2);

        main.onError(new TestException("Forced failure"));

        ts.assertNoErrors();

        inner.onNext(3);
        inner.onComplete();

        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3);
    }

    @Test
    public void mainErrorsDelayEnd() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserver<Integer> ts = main.concatMapEagerDelayError(
                new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer t) {
                        return inner;
                    }
                }, true).test();

        main.onNext(1);
        main.onNext(2);

        inner.onNext(2);

        ts.assertValue(2);

        main.onError(new TestException("Forced failure"));

        ts.assertNoErrors();

        inner.onNext(3);
        inner.onComplete();

        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3, 2, 3);
    }

    @Test
    public void mainErrorsImmediate() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserver<Integer> ts = main.concatMapEager(
                new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer t) {
                        return inner;
                    }
                }).test();

        main.onNext(1);
        main.onNext(2);

        inner.onNext(2);

        ts.assertValue(2);

        main.onError(new TestException("Forced failure"));

        assertFalse("inner has subscribers?", inner.hasObservers());

        inner.onNext(3);
        inner.onComplete();

        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2);
    }

    @Test
    public void longEager() {

        Observable.range(1, 2 * Observable.bufferSize())
        .concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) {
                return Observable.just(1);
            }
        })
        .test()
        .assertValueCount(2 * Observable.bufferSize())
        .assertNoErrors()
        .assertComplete();
    }

    TestObserver<Object> ts;

    Function<Integer, Observable<Integer>> toJust = new Function<Integer, Observable<Integer>>() {
        @Override
        public Observable<Integer> apply(Integer t) {
            return Observable.just(t);
        }
    };

    Function<Integer, Observable<Integer>> toRange = new Function<Integer, Observable<Integer>>() {
        @Override
        public Observable<Integer> apply(Integer t) {
            return Observable.range(t, 2);
        }
    };

    @Before
    public void before() {
        ts = new TestObserver<Object>();
    }

    @Test
    public void testSimple() {
        Observable.range(1, 100).concatMapEager(toJust).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValueCount(100);
        ts.assertComplete();
    }

    @Test
    public void testSimple2() {
        Observable.range(1, 100).concatMapEager(toRange).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValueCount(200);
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness2() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source).subscribe(ts);

        Assert.assertEquals(2, count.get());

        ts.assertValueCount(count.get());
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness3() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source).subscribe(ts);

        Assert.assertEquals(3, count.get());

        ts.assertValueCount(count.get());
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness4() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source).subscribe(ts);

        Assert.assertEquals(4, count.get());

        ts.assertValueCount(count.get());
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness5() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source).subscribe(ts);

        Assert.assertEquals(5, count.get());

        ts.assertValueCount(count.get());
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness6() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source, source).subscribe(ts);

        Assert.assertEquals(6, count.get());

        ts.assertValueCount(count.get());
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness7() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source).subscribe(ts);

        Assert.assertEquals(7, count.get());

        ts.assertValueCount(count.get());
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness8() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source, source).subscribe(ts);

        Assert.assertEquals(8, count.get());

        ts.assertValueCount(count.get());
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness9() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source, source, source).subscribe(ts);

        Assert.assertEquals(9, count.get());

        ts.assertValueCount(count.get());
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testMainError() {
        Observable.<Integer>error(new TestException()).concatMapEager(toJust).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInnerError() {
        // TODO verify: concatMapEager subscribes first then consumes the sources is okay

        PublishSubject<Integer> ps = PublishSubject.create();

        Observable.concatArrayEager(Observable.just(1), ps)
        .subscribe(ts);

        ps.onError(new TestException());

        ts.assertValue(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInnerEmpty() {
        Observable.concatArrayEager(Observable.empty(), Observable.empty()).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void testMapperThrows() {
        Observable.just(1).concatMapEager(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxConcurrent() {
        Observable.just(1).concatMapEager(toJust, 0, Observable.bufferSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCapacityHint() {
        Observable.just(1).concatMapEager(toJust, Observable.bufferSize(), 0);
    }

    @Test
//    @SuppressWarnings("unchecked")
    @Ignore("Observable doesn't do backpressure")
    public void testBackpressure() {
//        Observable.concatArrayEager(Observable.just(1), Observable.just(1)).subscribe(ts);
//
//        ts.assertNoErrors();
//        ts.assertNoValues();
//        ts.assertNotComplete();
//
//        ts.request(1);
//        ts.assertValue(1);
//        ts.assertNoErrors();
//        ts.assertNotComplete();
//
//        ts.request(1);
//        ts.assertValues(1, 1);
//        ts.assertNoErrors();
//        ts.assertComplete();
    }

    @Test
    public void testAsynchronousRun() {
        Observable.range(1, 2).concatMapEager(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                return Observable.range(1, 1000).subscribeOn(Schedulers.computation());
            }
        }).observeOn(Schedulers.newThread()).subscribe(ts);

        ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValueCount(2000);
    }

    @Test
    public void testReentrantWork() {
        final PublishSubject<Integer> subject = PublishSubject.create();

        final AtomicBoolean once = new AtomicBoolean();

        subject.concatMapEager(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                return Observable.just(t);
            }
        })
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                if (once.compareAndSet(false, true)) {
                    subject.onNext(2);
                }
            }
        })
        .subscribe(ts);

        subject.onNext(1);

        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValues(1, 2);
    }

    @Test
    @Ignore("Observable doesn't do backpressure so it can't bound its input count")
    public void testPrefetchIsBounded() {
        final AtomicInteger count = new AtomicInteger();

        TestObserver<Object> ts = TestObserver.create();

        Observable.just(1).concatMapEager(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                return Observable.range(1, Observable.bufferSize() * 2)
                        .doOnNext(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer t) {
                                count.getAndIncrement();
                            }
                        }).hide();
            }
        }).subscribe(ts);

        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotComplete();
        Assert.assertEquals(Observable.bufferSize(), count.get());
    }

    @Test
    @Ignore("Null values are not allowed in RS")
    public void testInnerNull() {
        Observable.just(1).concatMapEager(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                return Observable.just(null);
            }
        }).subscribe(ts);

        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValue(null);
    }


    @Test
    @Ignore("Observable doesn't do backpressure")
    public void testMaxConcurrent5() {
//        final List<Long> requests = new ArrayList<Long>();
//        Observable.range(1, 100).doOnRequest(new LongConsumer() {
//            @Override
//            public void accept(long reqCount) {
//                requests.add(reqCount);
//            }
//        }).concatMapEager(toJust, 5, Observable.bufferSize()).subscribe(ts);
//
//        ts.assertNoErrors();
//        ts.assertValueCount(100);
//        ts.assertComplete();
//
//        Assert.assertEquals(5, (long) requests.get(0));
//        Assert.assertEquals(1, (long) requests.get(1));
//        Assert.assertEquals(1, (long) requests.get(2));
//        Assert.assertEquals(1, (long) requests.get(3));
//        Assert.assertEquals(1, (long) requests.get(4));
//        Assert.assertEquals(1, (long) requests.get(5));
    }

    @SuppressWarnings("unchecked")
    @Test
    @Ignore("Currently there are no 2-9 argument variants, use concatArrayEager()")
    public void many() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Observable.class);

            Observable<Integer>[] obs = new Observable[i];
            Arrays.fill(obs, Observable.just(1));

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Observable.class.getMethod("concatEager", clazz);

            TestObserver<Integer> ts = TestObserver.create();

            ((Observable<Integer>)m.invoke(null, (Object[])obs)).subscribe(ts);

            ts.assertValues(expected);
            ts.assertNoErrors();
            ts.assertComplete();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void capacityHint() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> ts = TestObserver.create();

        Observable.concatEager(Arrays.asList(source, source, source), 1, 1).subscribe(ts);

        ts.assertValues(1, 1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void Observable() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> ts = TestObserver.create();

        Observable.concatEager(Observable.just(source, source, source)).subscribe(ts);

        ts.assertValues(1, 1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void ObservableCapacityHint() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> ts = TestObserver.create();

        Observable.concatEager(Observable.just(source, source, source), 1, 1).subscribe(ts);

        ts.assertValues(1, 1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void badCapacityHint() throws Exception {
        Observable<Integer> source = Observable.just(1);
        try {
            Observable.concatEager(Arrays.asList(source, source, source), 1, -99);
        } catch (IllegalArgumentException ex) {
            assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void mappingBadCapacityHint() throws Exception {
        Observable<Integer> source = Observable.just(1);
        try {
            Observable.just(source, source, source).concatMapEager((Function)Functions.identity(), 10, -99);
        } catch (IllegalArgumentException ex) {
            assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatEagerIterable() {
        Observable.concatEager(Arrays.asList(Observable.just(1), Observable.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).hide().concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(1, 2);
            }
        }));
    }

    @Test
    public void empty() {
        Observable.<Integer>empty().hide().concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(1, 2);
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void innerError() {
        Observable.<Integer>just(1).hide().concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorMaxConcurrency() {
        Observable.<Integer>just(1).hide().concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.error(new TestException());
            }
        }, 1, 128)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerCallableThrows() {
        Observable.<Integer>just(1).hide().concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        throw new TestException();
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerOuterRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestObserver<Integer> to = ps1.concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer v) throws Exception {
                        return ps2;
                    }
                }).test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                ps1.onNext(1);

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());

                to.assertSubscribed().assertNoValues().assertNotComplete();

                Throwable ex = to.errors().get(0);

                if (ex instanceof CompositeException) {
                    List<Throwable> es = TestHelper.errorList(to);
                    TestHelper.assertError(es, 0, TestException.class);
                    TestHelper.assertError(es, 1, TestException.class);
                } else {
                    to.assertError(TestException.class);
                    if (!errors.isEmpty()) {
                        TestHelper.assertUndeliverable(errors, 0, TestException.class);
                    }
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();

            final TestObserver<Integer> to = ps1.concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> apply(Integer v) throws Exception {
                    return Observable.never();
                }
            }).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onNext(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            to.assertEmpty();
        }
    }

    @Test
    public void mapperCancels() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1).hide()
        .concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                to.cancel();
                return Observable.never();
            }
        }, 1, 128)
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void innerErrorFused() {
        Observable.<Integer>just(1).hide().concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(1, 2).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        throw new TestException();
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorAfterPoll() {
        final UnicastSubject<Integer> us = UnicastSubject.create();
        us.onNext(1);

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                us.onError(new TestException());
            }
        };

        Observable.<Integer>just(1).hide()
        .concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return us;
            }
        }, 1, 128)
        .subscribe(to);

        to
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void fuseAndTake() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        us.onNext(1);
        us.onComplete();

        us.concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.just(1);
            }
        })
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.concatMapEager(new Function<Object, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Object v) throws Exception {
                        return Observable.just(v);
                    }
                });
            }
        });
    }

    @Test
    public void oneDelayed() {
        Observable.just(1, 2, 3, 4, 5)
        .concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer i) throws Exception {
                return i == 3 ? Observable.just(i) : Observable
                        .just(i)
                        .delay(1, TimeUnit.MILLISECONDS, Schedulers.io());
            }
        })
        .observeOn(Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5)
        ;
    }
}
