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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableConcatMapEagerTest extends RxJavaTest {

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
    public void mainErrorsDelayBoundary() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserverEx<Integer> to = main.concatMapEagerDelayError(
                new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer t) {
                        return inner;
                    }
                }, false).to(TestHelper.<Integer>testConsumer());

        main.onNext(1);

        inner.onNext(2);

        to.assertValue(2);

        main.onError(new TestException("Forced failure"));

        to.assertNoErrors();

        inner.onNext(3);
        inner.onComplete();

        to.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3);
    }

    @Test
    public void mainErrorsDelayEnd() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserverEx<Integer> to = main.concatMapEagerDelayError(
                new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer t) {
                        return inner;
                    }
                }, true).to(TestHelper.<Integer>testConsumer());

        main.onNext(1);
        main.onNext(2);

        inner.onNext(2);

        to.assertValue(2);

        main.onError(new TestException("Forced failure"));

        to.assertNoErrors();

        inner.onNext(3);
        inner.onComplete();

        to.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3, 2, 3);
    }

    @Test
    public void mainErrorsImmediate() {
        PublishSubject<Integer> main = PublishSubject.create();
        final PublishSubject<Integer> inner = PublishSubject.create();

        TestObserverEx<Integer> to = main.concatMapEager(
                new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer t) {
                        return inner;
                    }
                }).to(TestHelper.<Integer>testConsumer());

        main.onNext(1);
        main.onNext(2);

        inner.onNext(2);

        to.assertValue(2);

        main.onError(new TestException("Forced failure"));

        assertFalse("inner has subscribers?", inner.hasObservers());

        inner.onNext(3);
        inner.onComplete();

        to.assertFailureAndMessage(TestException.class, "Forced failure", 2);
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

    TestObserver<Object> to;

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
        to = new TestObserver<>();
    }

    @Test
    public void simple() {
        Observable.range(1, 100).concatMapEager(toJust).subscribe(to);

        to.assertNoErrors();
        to.assertValueCount(100);
        to.assertComplete();
    }

    @Test
    public void simple2() {
        Observable.range(1, 100).concatMapEager(toRange).subscribe(to);

        to.assertNoErrors();
        to.assertValueCount(200);
        to.assertComplete();
    }

    @Test
    public void eagerness2() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source).subscribe(to);

        Assert.assertEquals(2, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness3() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source).subscribe(to);

        Assert.assertEquals(3, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness4() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source).subscribe(to);

        Assert.assertEquals(4, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness5() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source).subscribe(to);

        Assert.assertEquals(5, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness6() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source, source).subscribe(to);

        Assert.assertEquals(6, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness7() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source).subscribe(to);

        Assert.assertEquals(7, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness8() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source, source).subscribe(to);

        Assert.assertEquals(8, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void eagerness9() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();

        Observable.concatArrayEager(source, source, source, source, source, source, source, source, source).subscribe(to);

        Assert.assertEquals(9, count.get());

        to.assertValueCount(count.get());
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void mainError() {
        Observable.<Integer>error(new TestException()).concatMapEager(toJust).subscribe(to);

        to.assertNoValues();
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerError() {
        // TODO verify: concatMapEager subscribes first then consumes the sources is okay

        PublishSubject<Integer> ps = PublishSubject.create();

        Observable.concatArrayEager(Observable.just(1), ps)
        .subscribe(to);

        ps.onError(new TestException());

        to.assertValue(1);
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerEmpty() {
        Observable.concatArrayEager(Observable.empty(), Observable.empty()).subscribe(to);

        to.assertNoValues();
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void mapperThrows() {
        Observable.just(1).concatMapEager(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                throw new TestException();
            }
        }).subscribe(to);

        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(TestException.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMaxConcurrent() {
        Observable.just(1).concatMapEager(toJust, 0, Observable.bufferSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCapacityHint() {
        Observable.just(1).concatMapEager(toJust, Observable.bufferSize(), 0);
    }

    @Test
    public void asynchronousRun() {
        Observable.range(1, 2).concatMapEager(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                return Observable.range(1, 1000).subscribeOn(Schedulers.computation());
            }
        }).observeOn(Schedulers.newThread()).subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        to.assertValueCount(2000);
    }

    @Test
    public void reentrantWork() {
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
        .subscribe(to);

        subject.onNext(1);

        to.assertNoErrors();
        to.assertNotComplete();
        to.assertValues(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayEager() throws Exception {
        for (int i = 2; i < 10; i++) {
            Observable<Integer>[] obs = new Observable[i];
            Arrays.fill(obs, Observable.just(1));

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Observable.class.getMethod("concatArrayEager", ObservableSource[].class);

            TestObserver<Integer> to = TestObserver.create();

            ((Observable<Integer>)m.invoke(null, new Object[]{obs})).subscribe(to);

            to.assertValues(expected);
            to.assertNoErrors();
            to.assertComplete();
        }
    }

    @Test
    public void capacityHint() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> to = TestObserver.create();

        Observable.concatEager(Arrays.asList(source, source, source), 1, 1).subscribe(to);

        to.assertValues(1, 1, 1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void Observable() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> to = TestObserver.create();

        Observable.concatEager(Observable.just(source, source, source)).subscribe(to);

        to.assertValues(1, 1, 1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void ObservableCapacityHint() {
        Observable<Integer> source = Observable.just(1);
        TestObserver<Integer> to = TestObserver.create();

        Observable.concatEager(Observable.just(source, source, source), 1, 1).subscribe(to);

        to.assertValues(1, 1, 1);
        to.assertNoErrors();
        to.assertComplete();
    }

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
    public void innerError2() {
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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestObserverEx<Integer> to = ps1.concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer v) throws Exception {
                        return ps2;
                    }
                }).to(TestHelper.<Integer>testConsumer());

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

                TestHelper.race(r1, r2);

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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
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
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }

    @Test
    public void mapperCancels() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.just(1).hide()
        .concatMapEager(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                to.dispose();
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

    @Test
    @SuppressWarnings("unchecked")
    public void maxConcurrencyOf2() {
        List<Integer>[] list = new ArrayList[100];
        for (int i = 0; i < 100; i++) {
            List<Integer> lst = new ArrayList<>();
            list[i] = lst;
            for (int k = 1; k <= 10; k++) {
                lst.add((i) * 10 + k);
            }
        }

        Observable.range(1, 1000)
        .buffer(10)
        .concatMapEager(new Function<List<Integer>, ObservableSource<List<Integer>>>() {
            @Override
            public ObservableSource<List<Integer>> apply(List<Integer> v)
                    throws Exception {
                return Observable.just(v)
                        .subscribeOn(Schedulers.io())
                        .doOnNext(new Consumer<List<Integer>>() {
                            @Override
                            public void accept(List<Integer> v)
                                    throws Exception {
                                Thread.sleep(new Random().nextInt(20));
                            }
                        });
            }
        }
                , 2, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(list);
    }

    @Test
    public void arrayDelayErrorDefault() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();
        PublishSubject<Integer> ps3 = PublishSubject.create();

        TestObserver<Integer> to = Observable.concatArrayEagerDelayError(ps1, ps2, ps3)
        .test();

        to.assertEmpty();

        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        assertTrue(ps3.hasObservers());

        ps2.onNext(2);
        ps2.onComplete();

        to.assertEmpty();

        ps1.onNext(1);

        to.assertValuesOnly(1);

        ps1.onComplete();

        to.assertValuesOnly(1, 2);

        ps3.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void arrayDelayErrorMaxConcurrency() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();
        PublishSubject<Integer> ps3 = PublishSubject.create();

        @SuppressWarnings("unchecked")
        TestObserver<Integer> to = Observable.concatArrayEagerDelayError(2, 2, ps1, ps2, ps3)
        .test();

        to.assertEmpty();

        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        assertFalse(ps3.hasObservers());

        ps2.onNext(2);
        ps2.onComplete();

        to.assertEmpty();

        ps1.onNext(1);

        to.assertValuesOnly(1);

        ps1.onComplete();

        assertTrue(ps3.hasObservers());

        to.assertValuesOnly(1, 2);

        ps3.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void arrayDelayErrorMaxConcurrencyErrorDelayed() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();
        PublishSubject<Integer> ps3 = PublishSubject.create();

        @SuppressWarnings("unchecked")
        TestObserver<Integer> to = Observable.concatArrayEagerDelayError(2, 2, ps1, ps2, ps3)
        .test();

        to.assertEmpty();

        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        assertFalse(ps3.hasObservers());

        ps2.onNext(2);
        ps2.onError(new TestException());

        to.assertEmpty();

        ps1.onNext(1);

        to.assertValuesOnly(1);

        ps1.onComplete();

        assertTrue(ps3.hasObservers());

        to.assertValuesOnly(1, 2);

        ps3.onComplete();

        to.assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void cancelActive() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();

        TestObserver<Integer> to = Observable
                .concatEager(Observable.just(ps1, ps2))
                .test();

        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());

        to.dispose();

        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
    }

    @Test
    public void cancelNoInnerYet() {
        PublishSubject<Observable<Integer>> ps1 = PublishSubject.create();

        TestObserver<Integer> to = Observable
                .concatEager(ps1)
                .test();

        assertTrue(ps1.hasObservers());

        to.dispose();

        assertFalse(ps1.hasObservers());
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.concatMapEager(new Function<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Integer v) throws Throwable {
                        return Observable.just(v).hide();
                    }
                });
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.concatMapEagerDelayError(new Function<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Integer v) throws Throwable {
                        return Observable.just(v).hide();
                    }
                }, false);
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayErrorTillEnd() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.concatMapEagerDelayError(new Function<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Integer v) throws Throwable {
                        return Observable.just(v).hide();
                    }
                }, true);
            }
        });
    }
}
