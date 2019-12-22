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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableFlatMapTest extends RxJavaTest {
    @Test
    public void normal() {
        Observer<Object> o = TestHelper.mockObserver();

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Function<Integer, List<Integer>> func = new Function<Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer t1) {
                return list;
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        for (Integer s : source) {
            for (Integer v : list) {
                verify(o).onNext(s | v);
            }
        }
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void collectionFunctionThrows() {
        Observer<Object> o = TestHelper.mockObserver();

        Function<Integer, List<Integer>> func = new Function<Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void resultFunctionThrows() {
        Observer<Object> o = TestHelper.mockObserver();

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Function<Integer, List<Integer>> func = new Function<Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer t1) {
                return list;
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void mergeError() {
        Observer<Object> o = TestHelper.mockObserver();

        Function<Integer, Observable<Integer>> func = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return Observable.error(new TestException());
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMap(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    <T, R> Function<T, R> just(final R value) {
        return new Function<T, R>() {

            @Override
            public R apply(T t1) {
                return value;
            }
        };
    }

    <R> Supplier<R> just0(final R value) {
        return new Supplier<R>() {

            @Override
            public R get() {
                return value;
            }
        };
    }

    @Test
    public void flatMapTransformsNormal() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), just(onError), just0(onComplete)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onComplete();

        verify(o, never()).onNext(5);
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void flatMapTransformsException() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.concat(
                Observable.fromIterable(Arrays.asList(10, 20, 30)),
                Observable.<Integer> error(new RuntimeException("Forced failure!"))
                );

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), just(onError), just0(onComplete)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(5);
        verify(o).onComplete();
        verify(o, never()).onNext(4);

        verify(o, never()).onError(any(Throwable.class));
    }

    <R> Supplier<R> funcThrow0(R r) {
        return new Supplier<R>() {
            @Override
            public R get() {
                throw new TestException();
            }
        };
    }

    <T, R> Function<T, R> funcThrow(T t, R r) {
        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                throw new TestException();
            }
        };
    }

    @Test
    public void flatMapTransformsOnNextFuncThrows() {
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(funcThrow(1, onError), just(onError), just0(onComplete)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void flatMapTransformsOnErrorFuncThrows() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.error(new TestException());

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), funcThrow((Throwable) null, onError), just0(onComplete)).subscribe(o);

        verify(o).onError(any(CompositeException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void flatMapTransformsOnCompletedFuncThrows() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.<Integer> asList());

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), just(onError), funcThrow0(onComplete)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void flatMapTransformsMergeException() {
        Observable<Integer> onNext = Observable.error(new TestException());
        Observable<Integer> onComplete = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Observer<Object> o = TestHelper.mockObserver();

        source.flatMap(just(onNext), just(onError), funcThrow0(onComplete)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    private static <T> Observable<T> composer(Observable<T> source, final AtomicInteger subscriptionCount, final int m) {
        return source.doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) {
                    int n = subscriptionCount.getAndIncrement();
                    if (n >= m) {
                        Assert.fail("Too many subscriptions! " + (n + 1));
                    }
            }
        }).doOnComplete(new Action() {
            @Override
            public void run() {
                    int n = subscriptionCount.decrementAndGet();
                    if (n < 0) {
                        Assert.fail("Too many unsubscriptions! " + (n - 1));
                    }
            }
        });
    }

    @Test
    public void flatMapMaxConcurrent() {
        final int m = 4;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> source = Observable.range(1, 10)
        .flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return composer(Observable.range(t1 * 10, 2), subscriptionCount, m)
                        .subscribeOn(Schedulers.computation());
            }
        }, m);

        TestObserver<Integer> to = new TestObserver<>();

        source.subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        Set<Integer> expected = new HashSet<>(Arrays.asList(
                10, 11, 20, 21, 30, 31, 40, 41, 50, 51, 60, 61, 70, 71, 80, 81, 90, 91, 100, 101
        ));
        Assert.assertEquals(expected.size(), to.values().size());
        Assert.assertTrue(expected.containsAll(to.values()));
    }

    @Test
    public void flatMapSelectorMaxConcurrent() {
        final int m = 4;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> source = Observable.range(1, 10)
            .flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return composer(Observable.range(t1 * 10, 2), subscriptionCount, m)
                        .subscribeOn(Schedulers.computation());
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 * 1000 + t2;
            }
        }, m);

        TestObserver<Integer> to = new TestObserver<>();

        source.subscribe(to);

        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        Set<Integer> expected = new HashSet<>(Arrays.asList(
                1010, 1011, 2020, 2021, 3030, 3031, 4040, 4041, 5050, 5051,
                6060, 6061, 7070, 7071, 8080, 8081, 9090, 9091, 10100, 10101
        ));
        Assert.assertEquals(expected.size(), to.values().size());
        System.out.println("--> testFlatMapSelectorMaxConcurrent: " + to.values());
        Assert.assertTrue(expected.containsAll(to.values()));
    }

    @Test
    public void flatMapTransformsMaxConcurrentNormalLoop() {
        for (int i = 0; i < 1000; i++) {
            if (i % 100 == 0) {
                System.out.println("testFlatMapTransformsMaxConcurrentNormalLoop => " + i);
            }
            flatMapTransformsMaxConcurrentNormal();
        }
    }

    @Test
    public void flatMapTransformsMaxConcurrentNormal() {
        final int m = 2;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> onNext =
                composer(
                        Observable.fromIterable(Arrays.asList(1, 2, 3))
                        .observeOn(Schedulers.computation())
                        ,
                subscriptionCount, m)
                .subscribeOn(Schedulers.computation())
                ;

        Observable<Integer> onComplete = composer(Observable.fromIterable(Arrays.asList(4)), subscriptionCount, m)
                .subscribeOn(Schedulers.computation());

        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Observer<Object> o = TestHelper.mockObserver();
        TestObserverEx<Object> to = new TestObserverEx<>(o);

        Function<Throwable, Observable<Integer>> just = just(onError);
        source.flatMap(just(onNext), just, just0(onComplete), m).subscribe(to);

        to.awaitDone(1, TimeUnit.SECONDS);
        to.assertNoErrors();
        to.assertTerminated();

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onComplete();

        verify(o, never()).onNext(5);
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void flatMapRangeMixedAsyncLoop() {
        for (int i = 0; i < 2000; i++) {
            if (i % 10 == 0) {
                System.out.println("flatMapRangeAsyncLoop > " + i);
            }
            TestObserverEx<Integer> to = new TestObserverEx<>();
            Observable.range(0, 1000)
            .flatMap(new Function<Integer, Observable<Integer>>() {
                final Random rnd = new Random();
                @Override
                public Observable<Integer> apply(Integer t) {
                    Observable<Integer> r = Observable.just(t);
                    if (rnd.nextBoolean()) {
                        r = r.hide();
                    }
                    return r;
                }
            })
            .observeOn(Schedulers.computation())
            .subscribe(to);

            to.awaitDone(2500, TimeUnit.MILLISECONDS);
            if (to.completions() == 0) {
                System.out.println(to.values().size());
            }
            to.assertTerminated();
            to.assertNoErrors();
            List<Integer> list = to.values();
            if (list.size() < 1000) {
                Set<Integer> set = new HashSet<>(list);
                for (int j = 0; j < 1000; j++) {
                    if (!set.contains(j)) {
                        System.out.println(j + " missing");
                    }
                }
            }
            assertEquals(1000, list.size());
        }
    }

    @Test
    public void flatMapIntPassthruAsync() {
        for (int i = 0; i < 1000; i++) {
            TestObserver<Integer> to = new TestObserver<>();

            Observable.range(1, 1000).flatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) {
                    return Observable.just(1).subscribeOn(Schedulers.computation());
                }
            }).subscribe(to);

            to.awaitDone(5, TimeUnit.SECONDS);
            to.assertNoErrors();
            to.assertComplete();
            to.assertValueCount(1000);
        }
    }

    @Test
    public void flatMapTwoNestedSync() {
        for (final int n : new int[] { 1, 1000, 1000000 }) {
            TestObserver<Integer> to = new TestObserver<>();

            Observable.just(1, 2).flatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) {
                    return Observable.range(1, n);
                }
            }).subscribe(to);

            System.out.println("flatMapTwoNestedSync >> @ " + n);
            to.assertNoErrors();
            to.assertComplete();
            to.assertValueCount(n * 2);
        }
    }

    @Test
    public void flatMapBiMapper() {
        Observable.just(1)
        .flatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.just(v * 10);
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true)
        .test()
        .assertResult(11);
    }

    @Test
    public void flatMapBiMapperWithError() {
        Observable.just(1)
        .flatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.just(v * 10).concatWith(Observable.<Integer>error(new TestException()));
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true)
        .test()
        .assertFailure(TestException.class, 11);
    }

    @Test
    public void flatMapBiMapperMaxConcurrency() {
        Observable.just(1, 2)
        .flatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.just(v * 10);
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        }, true, 1)
        .test()
        .assertResult(11, 22);
    }

    @Test
    public void flatMapEmpty() {
        assertSame(Observable.empty(), Observable.empty().flatMap(new Function<Object, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Object v) throws Exception {
                return Observable.just(v);
            }
        }));
    }

    @Test
    public void mergeScalar() {
        Observable.merge(Observable.just(Observable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void mergeScalar2() {
        Observable.merge(Observable.just(Observable.just(1)).hide())
        .test()
        .assertResult(1);
    }

    @Test
    public void mergeScalarEmpty() {
        Observable.merge(Observable.just(Observable.empty()).hide())
        .test()
        .assertResult();
    }

    @Test
    public void mergeScalarError() {
        Observable.merge(Observable.just(Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        })).hide())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void scalarReentrant() {
        final PublishSubject<Observable<Integer>> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(Observable.just(2));
                }
            }
        };

        Observable.merge(ps)
        .subscribe(to);

        ps.onNext(Observable.just(1));
        ps.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void scalarReentrant2() {
        final PublishSubject<Observable<Integer>> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(Observable.just(2));
                }
            }
        };

        Observable.merge(ps, 2)
        .subscribe(to);

        ps.onNext(Observable.just(1));
        ps.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void innerCompleteCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = Observable.merge(Observable.just(ps)).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void fusedInnerThrows() {
        Observable.just(1).hide()
        .flatMap(new Function<Integer, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Integer v) throws Exception {
                return Observable.range(1, 2).map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer w) throws Exception {
                        throw new TestException();
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedInnerThrows2() {
        TestObserverEx<Integer> to = Observable.range(1, 2).hide()
        .flatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(1, 2).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer w) throws Exception {
                        throw new TestException();
                    }
                });
            }
        }, true)
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.errorList(to);

        TestHelper.assertError(errors, 0, TestException.class);

        TestHelper.assertError(errors, 1, TestException.class);
    }

    @Test
    public void noCrossBoundaryFusion() {
        for (int i = 0; i < 500; i++) {
            TestObserver<Object> to = Observable.merge(
                    Observable.just(1).observeOn(Schedulers.single()).map(new Function<Integer, Object>() {
                        @Override
                        public Object apply(Integer v) throws Exception {
                            return Thread.currentThread().getName().substring(0, 4);
                        }
                    }),
                    Observable.just(1).observeOn(Schedulers.computation()).map(new Function<Integer, Object>() {
                        @Override
                        public Object apply(Integer v) throws Exception {
                            return Thread.currentThread().getName().substring(0, 4);
                        }
                    })
            )
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(2);

            List<Object> list = to.values();

            assertTrue(list.toString(), list.contains("RxSi"));
            assertTrue(list.toString(), list.contains("RxCo"));
        }
    }

    @Test
    public void cancelScalarDrainRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {

                final PublishSubject<Observable<Integer>> ps = PublishSubject.create();

                final TestObserver<Integer> to = ps.flatMap(Functions.<Observable<Integer>>identity()).test();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        to.dispose();
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps.onComplete();
                    }
                };

                TestHelper.race(r1, r2);

                assertTrue(errors.toString(), errors.isEmpty());
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void cancelDrainRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            for (int j = 1; j < 50; j += 5) {
                List<Throwable> errors = TestHelper.trackPluginErrors();
                try {

                    final PublishSubject<Observable<Integer>> ps = PublishSubject.create();

                    final TestObserver<Integer> to = ps.flatMap(Functions.<Observable<Integer>>identity()).test();

                    final PublishSubject<Integer> just = PublishSubject.create();
                    final PublishSubject<Integer> just2 = PublishSubject.create();
                    ps.onNext(just);
                    ps.onNext(just2);

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            just2.onNext(1);
                            to.dispose();
                        }
                    };
                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            just.onNext(1);
                        }
                    };

                    TestHelper.race(r1, r2);

                    assertTrue(errors.toString(), errors.isEmpty());
                } finally {
                    RxJavaPlugins.reset();
                }
            }
        }
    }

    @Test
    public void iterableMapperFunctionReturnsNull() {
        Observable.just(1)
        .flatMapIterable(new Function<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> apply(Integer v) throws Exception {
                return null;
            }
        }, new BiFunction<Integer, Object, Object>() {
            @Override
            public Object apply(Integer v, Object w) throws Exception {
                return v;
            }
        })
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The mapper returned a null Iterable");
    }

    @Test
    public void combinerMapperFunctionReturnsNull() {
        Observable.just(1)
        .flatMap(new Function<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Integer v) throws Exception {
                return null;
            }
        }, new BiFunction<Integer, Object, Object>() {
            @Override
            public Object apply(Integer v, Object w) throws Exception {
                return v;
            }
        })
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The mapper returned a null ObservableSource");
    }

    @Test
    public void failingFusedInnerCancelsSource() {
        final AtomicInteger counter = new AtomicInteger();
        Observable.range(1, 5)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                counter.getAndIncrement();
            }
        })
        .flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v)
                    throws Exception {
                return Observable.<Integer>fromIterable(new Iterable<Integer>() {
                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<Integer>() {
                            @Override
                            public boolean hasNext() {
                                return true;
                            }

                            @Override
                            public Integer next() {
                                throw new TestException();
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, counter.get());
    }

    @Test
    public void scalarQueueNoOverflow() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final PublishSubject<Integer> ps = PublishSubject.create();

            TestObserver<Integer> to = ps.flatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer v)
                        throws Exception {
                    return Observable.just(v + 1);
                }
            }, 1)
            .subscribeWith(new TestObserver<Integer>() {
                @Override
                public void onNext(Integer t) {
                    super.onNext(t);
                    if (t == 1) {
                        for (int i = 1; i < 10; i++) {
                            ps.onNext(i);
                        }
                        ps.onComplete();
                    }
                }
            });

            ps.onNext(0);

            if (!errors.isEmpty()) {
                to.onError(new CompositeException(errors));
            }

            to.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void scalarQueueNoOverflowHidden() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v)
                    throws Exception {
                return Observable.just(v + 1).hide();
            }
        }, 1)
        .subscribeWith(new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    for (int i = 1; i < 10; i++) {
                        ps.onNext(i);
                    }
                    ps.onComplete();
                }
            }
        });

        ps.onNext(0);

        to.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void fusedSourceCrashResumeWithNextSource() {
        final UnicastSubject<Integer> fusedSource = UnicastSubject.create();
        TestObserver<Integer> to = new TestObserver<>();

        ObservableFlatMap.MergeObserver<Integer, Integer> merger =
                new ObservableFlatMap.MergeObserver<>(to, new Function<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Integer t)
                            throws Exception {
                        if (t == 0) {
                            return fusedSource
                                    .map(new Function<Integer, Integer>() {
                                        @Override
                                        public Integer apply(Integer v)
                                                throws Exception {
                                            throw new TestException();
                                        }
                                    })
                                    .compose(TestHelper.<Integer>observableStripBoundary());
                        }
                        return Observable.range(10 * t, 5);
                    }
                }, true, Integer.MAX_VALUE, 128);

        merger.onSubscribe(Disposable.empty());
        merger.getAndIncrement();

        merger.onNext(0);
        merger.onNext(1);
        merger.onNext(2);

        assertTrue(fusedSource.hasObservers());

        fusedSource.onNext(-1);

        merger.drainLoop();

        to.assertValuesOnly(10, 11, 12, 13, 14, 20, 21, 22, 23, 24);
    }

    @Test
    public void maxConcurrencySustained() {
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();
        PublishSubject<Integer> ps3 = PublishSubject.create();
        PublishSubject<Integer> ps4 = PublishSubject.create();

        TestObserver<Integer> to = Observable.just(ps1, ps2, ps3, ps4)
        .flatMap(new Function<PublishSubject<Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(PublishSubject<Integer> v) throws Exception {
                return v;
            }
        }, 2)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (v == 1) {
                    // this will make sure the drain loop detects two completed
                    // inner sources and replaces them with fresh ones
                    ps1.onComplete();
                    ps2.onComplete();
                }
            }
        })
        .test();

        ps1.onNext(1);

        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        assertTrue(ps3.hasObservers());
        assertTrue(ps4.hasObservers());

        to.dispose();

        assertFalse(ps3.hasObservers());
        assertFalse(ps4.hasObservers());
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.flatMap(new Function<Integer, Observable<Integer>>() {
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
                return upstream.flatMap(new Function<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Integer v) throws Throwable {
                        return Observable.just(v).hide();
                    }
                }, true);
            }
        });
    }
}
