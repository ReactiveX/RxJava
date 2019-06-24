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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.observers.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.*;
import io.reactivex.testsupport.*;

public class ObservableConcatMapSchedulerTest {

    @Test
    public void boundaryFusion() {
        Observable.range(1, 10000)
        .observeOn(Schedulers.single())
        .map(new Function<Integer, String>() {
            @Override
            public String apply(Integer t) throws Exception {
                String name = Thread.currentThread().getName();
                if (name.contains("RxSingleScheduler")) {
                    return "RxSingleScheduler";
                }
                return name;
            }
        })
        .concatMap(new Function<String, ObservableSource<? extends Object>>() {
            @Override
            public ObservableSource<? extends Object> apply(String v)
                    throws Exception {
                return Observable.just(v);
            }
        }, 2, ImmediateThinScheduler.INSTANCE)
        .observeOn(Schedulers.computation())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("RxSingleScheduler");
    }

    @Test
    public void boundaryFusionDelayError() {
        Observable.range(1, 10000)
        .observeOn(Schedulers.single())
        .map(new Function<Integer, String>() {
            @Override
            public String apply(Integer t) throws Exception {
                String name = Thread.currentThread().getName();
                if (name.contains("RxSingleScheduler")) {
                    return "RxSingleScheduler";
                }
                return name;
            }
        })
        .concatMapDelayError(new Function<String, ObservableSource<? extends Object>>() {
            @Override
            public ObservableSource<? extends Object> apply(String v)
                    throws Exception {
                return Observable.just(v);
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE)
        .observeOn(Schedulers.computation())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("RxSingleScheduler");
    }

    @Test
    public void pollThrows() {
        Observable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .compose(TestHelper.<Integer>observableStripBoundary())
        .concatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                return Observable.just(v);
            }
        }, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void pollThrowsDelayError() {
        Observable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .compose(TestHelper.<Integer>observableStripBoundary())
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                return Observable.just(v);
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void noCancelPrevious() {
        final AtomicInteger counter = new AtomicInteger();

        Observable.range(1, 5)
        .concatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.just(v).doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        counter.getAndIncrement();
                    }
                });
            }
        }, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(0, counter.get());
    }

    @Test
    public void delayErrorCallableTillTheEnd() {
        Observable.just(1, 2, 3, 101, 102, 23, 890, 120, 32)
        .concatMapDelayError(new Function<Integer, Observable<Integer>>() {
          @Override public Observable<Integer> apply(final Integer integer) throws Exception {
            return Observable.fromCallable(new Callable<Integer>() {
              @Override public Integer call() throws Exception {
                if (integer >= 100) {
                  throw new NullPointerException("test null exp");
                }
                return integer;
              }
            });
          }
        }, 2, true, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(CompositeException.class, 1, 2, 3, 23, 32);
    }

    @Test
    public void delayErrorCallableEager() {
        Observable.just(1, 2, 3, 101, 102, 23, 890, 120, 32)
        .concatMapDelayError(new Function<Integer, Observable<Integer>>() {
          @Override public Observable<Integer> apply(final Integer integer) throws Exception {
            return Observable.fromCallable(new Callable<Integer>() {
              @Override public Integer call() throws Exception {
                if (integer >= 100) {
                  throw new NullPointerException("test null exp");
                }
                return integer;
              }
            });
          }
        }, 2, false, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(NullPointerException.class, 1, 2, 3);
    }

    @Test
    public void mapperScheduled() {
        TestObserver<String> to = Observable.just(1)
        .concatMap(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName());
            }
        }, 2, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperScheduledHidden() {
        TestObserver<String> to = Observable.just(1)
        .concatMap(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName()).hide();
            }
        }, 2, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayErrorScheduled() {
        TestObserver<String> to = Observable.just(1)
        .concatMapDelayError(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName());
            }
        }, 2, false, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayErrorScheduledHidden() {
        TestObserver<String> to = Observable.just(1)
        .concatMapDelayError(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName()).hide();
            }
        }, 2, false, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayError2Scheduled() {
        TestObserver<String> to = Observable.just(1)
        .concatMapDelayError(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName());
            }
        }, 2, true, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayError2ScheduledHidden() {
        TestObserver<String> to = Observable.just(1)
        .concatMapDelayError(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName()).hide();
            }
        }, 2, true, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test(timeout = 30000)
    public void issue2890NoStackoverflow() throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Scheduler sch = Schedulers.from(executor);

        Function<Integer, Observable<Integer>> func = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                Observable<Integer> flowable = Observable.just(t)
                        .subscribeOn(sch)
                ;
                Subject<Integer> processor = UnicastSubject.create();
                flowable.subscribe(processor);
                return processor;
            }
        };

        int n = 5000;
        final AtomicInteger counter = new AtomicInteger();

        Observable.range(1, n).concatMap(func, 2, ImmediateThinScheduler.INSTANCE).subscribe(new DefaultObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                // Consume after sleep for 1 ms
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // ignored
                }
                if (counter.getAndIncrement() % 100 == 0) {
                    System.out.print("testIssue2890NoStackoverflow -> ");
                    System.out.println(counter.get());
                };
            }

            @Override
            public void onComplete() {
                executor.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                executor.shutdown();
            }
        });

        executor.awaitTermination(20000, TimeUnit.MILLISECONDS);

        assertEquals(n, counter.get());
    }

    @Test//(timeout = 100000)
    public void concatMapRangeAsyncLoopIssue2876() {
        final long durationSeconds = 2;
        final long startTime = System.currentTimeMillis();
        for (int i = 0;; i++) {
            //only run this for a max of ten seconds
            if (System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(durationSeconds)) {
                return;
            }
            if (i % 1000 == 0) {
                System.out.println("concatMapRangeAsyncLoop > " + i);
            }
            TestObserverEx<Integer> to = new TestObserverEx<Integer>();
            Observable.range(0, 1000)
            .concatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) {
                    return Observable.fromIterable(Arrays.asList(t));
                }
            }, 2, ImmediateThinScheduler.INSTANCE)
            .observeOn(Schedulers.computation()).subscribe(to);

            to.awaitDone(2500, TimeUnit.MILLISECONDS);
            to.assertTerminated();
            to.assertNoErrors();
            assertEquals(1000, to.values().size());
            assertEquals((Integer)999, to.values().get(999));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    @Ignore("concat(a, b, ...) replaced by concatArray(T...)")
    public void concatMany() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Observable.class);

            Observable<Integer>[] obs = new Observable[i];
            Arrays.fill(obs, Observable.just(1));

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Observable.class.getMethod("concat", clazz);

            TestObserver<Integer> to = TestObserver.create();

            ((Observable<Integer>)m.invoke(null, (Object[])obs)).subscribe(to);

            to.assertValues(expected);
            to.assertNoErrors();
            to.assertComplete();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapJustJust() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(Observable.just(1)).concatMap((Function)Functions.identity(), 2, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertValue(1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapJustRange() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(Observable.range(1, 5)).concatMap((Function)Functions.identity(), 2, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertValues(1, 2, 3, 4, 5);
        to.assertNoErrors();
        to.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapDelayErrorJustJust() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(Observable.just(1)).concatMapDelayError((Function)Functions.identity(), 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertValue(1);
        to.assertNoErrors();
        to.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapDelayErrorJustRange() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(Observable.range(1, 5)).concatMapDelayError((Function)Functions.identity(), 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertValues(1, 2, 3, 4, 5);
        to.assertNoErrors();
        to.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    @Ignore("startWith(a, b, ...) replaced by startWithArray(T...)")
    public void startWith() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Object.class);

            Object[] obs = new Object[i];
            Arrays.fill(obs, 1);

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Observable.class.getMethod("startWith", clazz);

            TestObserver<Integer> to = TestObserver.create();

            ((Observable<Integer>)m.invoke(Observable.empty(), obs)).subscribe(to);

            to.assertValues(expected);
            to.assertNoErrors();
            to.assertComplete();
        }
    }

    static final class InfiniteIterator implements Iterator<Integer>, Iterable<Integer> {

        int count;

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Integer next() {
            return count++;
        }

        @Override
        public void remove() {
        }

        @Override
        public Iterator<Integer> iterator() {
            return this;
        }
    }

    @Test
    public void concatMapDelayError() {
        Observable.just(Observable.just(1), Observable.just(2))
        .concatMapDelayError(Functions.<Observable<Integer>>identity(), 2, true, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatMapDelayErrorJustSource() {
        Observable.just(0)
        .concatMapDelayError(new Function<Object, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Object v) throws Exception {
                return Observable.just(1);
            }
        }, 16, true, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1);

    }

    @Test
    public void concatMapJustSource() {
        Observable.just(0).hide()
        .concatMap(new Function<Object, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Object v) throws Exception {
                return Observable.just(1);
            }
        }, 16, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1);
    }

    @Test
    public void concatMapJustSourceDelayError() {
        Observable.just(0).hide()
        .concatMapDelayError(new Function<Object, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Object v) throws Exception {
                return Observable.just(1);
            }
        }, 16, false, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1);
    }

    @Test
    public void concatMapEmpty() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.empty()), 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult();
    }

    @Test
    public void concatMapEmptyDelayError() {
        Observable.just(1).hide()
        .concatMapDelayError(Functions.justFunction(Observable.empty()), 2, true, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult();
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Object> f) throws Exception {
                return f.concatMap(Functions.justFunction(Observable.just(2)), 2, ImmediateThinScheduler.INSTANCE);
            }
        });
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Object> f) throws Exception {
                return f.concatMapDelayError(Functions.justFunction(Observable.just(2)), 2, true, ImmediateThinScheduler.INSTANCE);
            }
        });
    }

    @Test
    public void immediateInnerNextOuterError() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        final TestObserverEx<Integer> to = new TestObserverEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onError(new TestException("First"));
                }
            }
        };

        ps.concatMap(Functions.justFunction(Observable.just(1)), 2, ImmediateThinScheduler.INSTANCE)
        .subscribe(to);

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        to.assertFailureAndMessage(TestException.class, "First", 1);
    }

    @Test
    public void immediateInnerNextOuterError2() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        final TestObserverEx<Integer> to = new TestObserverEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onError(new TestException("First"));
                }
            }
        };

        ps.concatMap(Functions.justFunction(Observable.just(1).hide()), 2, ImmediateThinScheduler.INSTANCE)
        .subscribe(to);

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        to.assertFailureAndMessage(TestException.class, "First", 1);
    }

    @Test
    public void concatMapInnerError() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.error(new TestException())), 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void concatMapInnerErrorDelayError() {
        Observable.just(1).hide()
        .concatMapDelayError(Functions.justFunction(Observable.error(new TestException())), 2, true, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(Observable<Integer> f) throws Exception {
                return f.concatMap(Functions.justFunction(Observable.just(1).hide()), 2, ImmediateThinScheduler.INSTANCE);
            }
        }, true, 1, 1, 1);
    }

    @Test
    public void badInnerSource() {
        @SuppressWarnings("rawtypes")
        final Observer[] ts0 = { null };
        TestObserverEx<Integer> to = Observable.just(1).hide().concatMap(Functions.justFunction(new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> o) {
                ts0[0] = o;
                o.onSubscribe(Disposables.empty());
                o.onError(new TestException("First"));
            }
        }), 2, ImmediateThinScheduler.INSTANCE)
        .to(TestHelper.<Integer>testConsumer());

        to.assertFailureAndMessage(TestException.class, "First");

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ts0[0].onError(new TestException("Second"));

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badInnerSourceDelayError() {
        @SuppressWarnings("rawtypes")
        final Observer[] ts0 = { null };
        TestObserverEx<Integer> to = Observable.just(1).hide().concatMapDelayError(Functions.justFunction(new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> o) {
                ts0[0] = o;
                o.onSubscribe(Disposables.empty());
                o.onError(new TestException("First"));
            }
        }), 2, true, ImmediateThinScheduler.INSTANCE)
        .to(TestHelper.<Integer>testConsumer());

        to.assertFailureAndMessage(TestException.class, "First");

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ts0[0].onError(new TestException("Second"));

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceDelayError() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(Observable<Integer> f) throws Exception {
                return f.concatMapDelayError(Functions.justFunction(Observable.just(1).hide()), 2, true, ImmediateThinScheduler.INSTANCE);
            }
        }, true, 1, 1, 1);
    }

    @Test
    public void fusedCrash() {
        Observable.range(1, 2)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception { throw new TestException(); }
        })
        .concatMap(Functions.justFunction(Observable.just(1)), 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedCrashDelayError() {
        Observable.range(1, 2)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception { throw new TestException(); }
        })
        .concatMapDelayError(Functions.justFunction(Observable.just(1)), 2, true, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void callableCrash() {
        Observable.just(1).hide()
        .concatMap(Functions.justFunction(Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        })), 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void callableCrashDelayError() {
        Observable.just(1).hide()
        .concatMapDelayError(Functions.justFunction(Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        })), 2, true, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 2)
        .concatMap(Functions.justFunction(Observable.just(1)), 2, ImmediateThinScheduler.INSTANCE));

        TestHelper.checkDisposed(Observable.range(1, 2)
        .concatMapDelayError(Functions.justFunction(Observable.just(1)), 2, true, ImmediateThinScheduler.INSTANCE));
    }

    @Test
    public void notVeryEnd() {
        Observable.range(1, 2)
        .concatMapDelayError(Functions.justFunction(Observable.error(new TestException())), 16, false, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .concatMapDelayError(Functions.justFunction(Observable.just(2)), 16, false, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperThrows() {
        Observable.range(1, 2)
        .concatMap(new Function<Integer, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Integer v) throws Exception {
                throw new TestException();
            }
        }, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainErrors() {
        PublishSubject<Integer> source = PublishSubject.create();

        TestObserver<Integer> to = TestObserver.create();

        source.concatMapDelayError(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return Observable.range(v, 2);
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        to.assertValues(1, 2, 2, 3);
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerErrors() {
        final Observable<Integer> inner = Observable.range(1, 2)
                .concatWith(Observable.<Integer>error(new TestException()));

        TestObserver<Integer> to = TestObserver.create();

        Observable.range(1, 3).concatMapDelayError(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return inner;
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertValues(1, 2, 1, 2, 1, 2);
        to.assertError(CompositeException.class);
        to.assertNotComplete();
    }

    @Test
    public void singleInnerErrors() {
        final Observable<Integer> inner = Observable.range(1, 2).concatWith(Observable.<Integer>error(new TestException()));

        TestObserver<Integer> to = TestObserver.create();

        Observable.just(1)
        .hide() // prevent scalar optimization
        .concatMapDelayError(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return inner;
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertValues(1, 2);
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerNull() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(1)
        .hide() // prevent scalar optimization
        .concatMapDelayError(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return null;
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertNoValues();
        to.assertError(NullPointerException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerThrows() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.just(1)
        .hide() // prevent scalar optimization
        .concatMapDelayError(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                throw new TestException();
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertNoValues();
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void innerWithEmpty() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.range(1, 3)
        .concatMapDelayError(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return v == 2 ? Observable.<Integer>empty() : Observable.range(1, 2);
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertValues(1, 2, 1, 2);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void innerWithScalar() {
        TestObserver<Integer> to = TestObserver.create();

        Observable.range(1, 3)
        .concatMapDelayError(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return v == 2 ? Observable.just(3) : Observable.range(1, 2);
            }
        }, 2, true, ImmediateThinScheduler.INSTANCE).subscribe(to);

        to.assertValues(1, 2, 3, 1, 2);
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void mapperScheduledLong() {
        TestObserver<String> to = Observable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMap(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName())
                        .repeat(1000)
                        .observeOn(Schedulers.io());
            }
        }, 2, Schedulers.single())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayErrorScheduledLong() {
        TestObserver<String> to = Observable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMapDelayError(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName())
                        .repeat(1000)
                        .observeOn(Schedulers.io());
            }
        }, 2, false, Schedulers.single())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayError2ScheduledLong() {
        TestObserver<String> to = Observable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMapDelayError(new Function<Integer, Observable<String>>() {
            @Override
            public Observable<String> apply(Integer t) throws Throwable {
                return Observable.just(Thread.currentThread().getName())
                        .repeat(1000)
                        .observeOn(Schedulers.io());
            }
        }, 2, true, Schedulers.single())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(to.values().toString(), to.values().get(0).startsWith("RxSingleScheduler-"));
    }
}
