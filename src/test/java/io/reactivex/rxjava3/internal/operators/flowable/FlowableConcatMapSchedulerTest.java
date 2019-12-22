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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableConcatMapSchedulerTest extends RxJavaTest {

    @Test
    public void boundaryFusion() {
        Flowable.range(1, 10000)
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
        .concatMap(new Function<String, Publisher<? extends Object>>() {
            @Override
            public Publisher<? extends Object> apply(String v)
                    throws Exception {
                return Flowable.just(v);
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
        Flowable.range(1, 10000)
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
        .concatMapDelayError(new Function<String, Publisher<? extends Object>>() {
            @Override
            public Publisher<? extends Object> apply(String v)
                    throws Exception {
                return Flowable.just(v);
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE)
        .observeOn(Schedulers.computation())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("RxSingleScheduler");
    }

    @Test
    public void pollThrows() {
        Flowable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .compose(TestHelper.<Integer>flowableStripBoundary())
        .concatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v)
                    throws Exception {
                return Flowable.just(v);
            }
        }, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void pollThrowsDelayError() {
        Flowable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .compose(TestHelper.<Integer>flowableStripBoundary())
        .concatMapDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v)
                    throws Exception {
                return Flowable.just(v);
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void noCancelPrevious() {
        final AtomicInteger counter = new AtomicInteger();

        Flowable.range(1, 5)
        .concatMap(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return Flowable.just(v).doOnCancel(new Action() {
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
        Flowable.just(1, 2, 3, 101, 102, 23, 890, 120, 32)
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
          @Override public Flowable<Integer> apply(final Integer integer) throws Exception {
            return Flowable.fromCallable(new Callable<Integer>() {
              @Override public Integer call() throws Exception {
                if (integer >= 100) {
                  throw new NullPointerException("test null exp");
                }
                return integer;
              }
            });
          }
        }, true, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(CompositeException.class, 1, 2, 3, 23, 32);
    }

    @Test
    public void delayErrorCallableEager() {
        Flowable.just(1, 2, 3, 101, 102, 23, 890, 120, 32)
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
          @Override public Flowable<Integer> apply(final Integer integer) throws Exception {
            return Flowable.fromCallable(new Callable<Integer>() {
              @Override public Integer call() throws Exception {
                if (integer >= 100) {
                  throw new NullPointerException("test null exp");
                }
                return integer;
              }
            });
          }
        }, false, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(NullPointerException.class, 1, 2, 3);
    }

    @Test
    public void mapperScheduled() {
        TestSubscriber<String> ts = Flowable.just(1)
        .concatMap(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName());
            }
        }, 2, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperScheduledHidden() {
        TestSubscriber<String> ts = Flowable.just(1)
        .concatMap(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName()).hide();
            }
        }, 2, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayErrorScheduled() {
        TestSubscriber<String> ts = Flowable.just(1)
        .concatMapDelayError(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName());
            }
        }, false, 2, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayErrorScheduledHidden() {
        TestSubscriber<String> ts = Flowable.just(1)
        .concatMapDelayError(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName()).hide();
            }
        }, false, 2, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayError2Scheduled() {
        TestSubscriber<String> ts = Flowable.just(1)
        .concatMapDelayError(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName());
            }
        }, true, 2, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayError2ScheduledHidden() {
        TestSubscriber<String> ts = Flowable.just(1)
        .concatMapDelayError(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName()).hide();
            }
        }, true, 2, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void issue2890NoStackoverflow() throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Scheduler sch = Schedulers.from(executor);

        Function<Integer, Flowable<Integer>> func = new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                Flowable<Integer> flowable = Flowable.just(t)
                        .subscribeOn(sch)
                ;
                FlowableProcessor<Integer> processor = UnicastProcessor.create();
                flowable.subscribe(processor);
                return processor;
            }
        };

        int n = 5000;
        final AtomicInteger counter = new AtomicInteger();

        Flowable.range(1, n).concatMap(func, 2, ImmediateThinScheduler.INSTANCE).subscribe(new DefaultSubscriber<Integer>() {
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

    @Test
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
            TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
            Flowable.range(0, 1000)
            .concatMap(new Function<Integer, Flowable<Integer>>() {
                @Override
                public Flowable<Integer> apply(Integer t) {
                    return Flowable.fromIterable(Arrays.asList(t));
                }
            }, 2, ImmediateThinScheduler.INSTANCE)
            .observeOn(Schedulers.computation()).subscribe(ts);

            ts.awaitDone(2500, TimeUnit.MILLISECONDS);
            ts.assertTerminated();
            ts.assertNoErrors();
            assertEquals(1000, ts.values().size());
            assertEquals((Integer)999, ts.values().get(999));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArray() throws Exception {
        for (int i = 2; i < 10; i++) {
            Flowable<Integer>[] obs = new Flowable[i];
            Arrays.fill(obs, Flowable.just(1));

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Flowable.class.getMethod("concatArray",  Publisher[].class);

            TestSubscriber<Integer> ts = TestSubscriber.create();

            ((Flowable<Integer>)m.invoke(null, new Object[]{obs})).subscribe(ts);

            ts.assertValues(expected);
            ts.assertNoErrors();
            ts.assertComplete();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapJustJust() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(Flowable.just(1)).concatMap((Function)Functions.identity(), 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapJustRange() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(Flowable.range(1, 5)).concatMap((Function)Functions.identity(), 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapDelayErrorJustJust() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(Flowable.just(1)).concatMapDelayError((Function)Functions.identity(), true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void concatMapDelayErrorJustRange() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(Flowable.range(1, 5)).concatMapDelayError((Function)Functions.identity(), true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void startWithArray() throws Exception {
        for (int i = 2; i < 10; i++) {
            Object[] obs = new Object[i];
            Arrays.fill(obs, 1);

            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);

            Method m = Flowable.class.getMethod("startWithArray", Object[].class);

            TestSubscriber<Integer> ts = TestSubscriber.create();

            ((Flowable<Integer>)m.invoke(Flowable.empty(), new Object[]{obs})).subscribe(ts);

            ts.assertValues(expected);
            ts.assertNoErrors();
            ts.assertComplete();
        }
    }

    @Test
    public void concatMapDelayError() {
        Flowable.just(Flowable.just(1), Flowable.just(2))
        .concatMapDelayError(Functions.<Flowable<Integer>>identity(), true, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatMapDelayErrorJustSource() {
        Flowable.just(0)
        .concatMapDelayError(new Function<Object, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Object v) throws Exception {
                return Flowable.just(1);
            }
        }, true, 16, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1);

    }

    @Test
    public void concatMapJustSource() {
        Flowable.just(0).hide()
        .concatMap(new Function<Object, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Object v) throws Exception {
                return Flowable.just(1);
            }
        }, 16, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1);
    }

    @Test
    public void concatMapJustSourceDelayError() {
        Flowable.just(0).hide()
        .concatMapDelayError(new Function<Object, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Object v) throws Exception {
                return Flowable.just(1);
            }
        }, false, 16, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult(1);
    }

    @Test
    public void concatMapScalarBackpressured() {
        Flowable.just(1).hide()
        .concatMap(Functions.justFunction(Flowable.just(2)), 2, ImmediateThinScheduler.INSTANCE)
        .test(1L)
        .assertResult(2);
    }

    @Test
    public void concatMapScalarBackpressuredDelayError() {
        Flowable.just(1).hide()
        .concatMapDelayError(Functions.justFunction(Flowable.just(2)), true, 2, ImmediateThinScheduler.INSTANCE)
        .test(1L)
        .assertResult(2);
    }

    @Test
    public void concatMapEmpty() {
        Flowable.just(1).hide()
        .concatMap(Functions.justFunction(Flowable.empty()), 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult();
    }

    @Test
    public void concatMapEmptyDelayError() {
        Flowable.just(1).hide()
        .concatMapDelayError(Functions.justFunction(Flowable.empty()), true, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertResult();
    }

    @Test
    public void ignoreBackpressure() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < 10; i++) {
                    s.onNext(i);
                }
            }
        }
        .concatMap(Functions.justFunction(Flowable.just(2)), 8, ImmediateThinScheduler.INSTANCE)
        .test(0L)
        .assertFailure(IllegalStateException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Object> f) throws Exception {
                return f.concatMap(Functions.justFunction(Flowable.just(2)), 2, ImmediateThinScheduler.INSTANCE);
            }
        });
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Object> f) throws Exception {
                return f.concatMapDelayError(Functions.justFunction(Flowable.just(2)), true, 2, ImmediateThinScheduler.INSTANCE);
            }
        });
    }

    @Test
    public void immediateInnerNextOuterError() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        final TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onError(new TestException("First"));
                }
            }
        };

        pp.concatMap(Functions.justFunction(Flowable.just(1)), 2, ImmediateThinScheduler.INSTANCE)
        .subscribe(ts);

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        ts.assertFailureAndMessage(TestException.class, "First", 1);
    }

    @Test
    public void immediateInnerNextOuterError2() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        final TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onError(new TestException("First"));
                }
            }
        };

        pp.concatMap(Functions.justFunction(Flowable.just(1).hide()), 2, ImmediateThinScheduler.INSTANCE)
        .subscribe(ts);

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        ts.assertFailureAndMessage(TestException.class, "First", 1);
    }

    @Test
    public void concatMapInnerError() {
        Flowable.just(1).hide()
        .concatMap(Functions.justFunction(Flowable.error(new TestException())), 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void concatMapInnerErrorDelayError() {
        Flowable.just(1).hide()
        .concatMapDelayError(Functions.justFunction(Flowable.error(new TestException())), true, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.concatMap(Functions.justFunction(Flowable.just(1).hide()), 2, ImmediateThinScheduler.INSTANCE);
            }
        }, true, 1, 1, 1);
    }

    @Test
    public void badInnerSource() {
        @SuppressWarnings("rawtypes")
        final Subscriber[] ts0 = { null };
        TestSubscriberEx<Integer> ts = Flowable.just(1).hide().concatMap(Functions.justFunction(new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                ts0[0] = s;
                s.onSubscribe(new BooleanSubscription());
                s.onError(new TestException("First"));
            }
        }), 2, ImmediateThinScheduler.INSTANCE)
        .to(TestHelper.<Integer>testConsumer());

        ts.assertFailureAndMessage(TestException.class, "First");

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
        final Subscriber[] ts0 = { null };
        TestSubscriberEx<Integer> ts = Flowable.just(1).hide().concatMapDelayError(Functions.justFunction(new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                ts0[0] = s;
                s.onSubscribe(new BooleanSubscription());
                s.onError(new TestException("First"));
            }
        }), true, 2, ImmediateThinScheduler.INSTANCE)
        .to(TestHelper.<Integer>testConsumer());

        ts.assertFailureAndMessage(TestException.class, "First");

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
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.concatMapDelayError(Functions.justFunction(Flowable.just(1).hide()), true, 2, ImmediateThinScheduler.INSTANCE);
            }
        }, true, 1, 1, 1);
    }

    @Test
    public void fusedCrash() {
        Flowable.range(1, 2)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception { throw new TestException(); }
        })
        .concatMap(Functions.justFunction(Flowable.just(1)), 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedCrashDelayError() {
        Flowable.range(1, 2)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception { throw new TestException(); }
        })
        .concatMapDelayError(Functions.justFunction(Flowable.just(1)), true, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void callableCrash() {
        Flowable.just(1).hide()
        .concatMap(Functions.justFunction(Flowable.fromCallable(new Callable<Object>() {
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
        Flowable.just(1).hide()
        .concatMapDelayError(Functions.justFunction(Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        })), true, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.range(1, 2)
        .concatMap(Functions.justFunction(Flowable.just(1)), 2, ImmediateThinScheduler.INSTANCE));

        TestHelper.checkDisposed(Flowable.range(1, 2)
        .concatMapDelayError(Functions.justFunction(Flowable.just(1)), true, 2, ImmediateThinScheduler.INSTANCE));
    }

    @Test
    public void notVeryEnd() {
        Flowable.range(1, 2)
        .concatMapDelayError(Functions.justFunction(Flowable.error(new TestException())), false, 16, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .concatMapDelayError(Functions.justFunction(Flowable.just(2)), false, 16, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperThrows() {
        Flowable.range(1, 2)
        .concatMap(new Function<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Integer v) throws Exception {
                throw new TestException();
            }
        }, 2, ImmediateThinScheduler.INSTANCE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainErrors() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        source.concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return Flowable.range(v, 2);
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.assertValues(1, 2, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void innerErrors() {
        final Flowable<Integer> inner = Flowable.range(1, 2)
                .concatWith(Flowable.<Integer>error(new TestException()));

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.range(1, 3).concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return inner;
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertValues(1, 2, 1, 2, 1, 2);
        ts.assertError(CompositeException.class);
        ts.assertNotComplete();
    }

    @Test
    public void singleInnerErrors() {
        final Flowable<Integer> inner = Flowable.range(1, 2).concatWith(Flowable.<Integer>error(new TestException()));

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1)
        .hide() // prevent scalar optimization
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return inner;
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void innerNull() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1)
        .hide() // prevent scalar optimization
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return null;
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(NullPointerException.class);
        ts.assertNotComplete();
    }

    @Test
    public void innerThrows() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.just(1)
        .hide() // prevent scalar optimization
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                throw new TestException();
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void innerWithEmpty() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.range(1, 3)
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return v == 2 ? Flowable.<Integer>empty() : Flowable.range(1, 2);
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertValues(1, 2, 1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void innerWithScalar() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.range(1, 3)
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return v == 2 ? Flowable.just(3) : Flowable.range(1, 2);
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertValues(1, 2, 3, 1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Flowable.range(1, 3).concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return Flowable.range(v, 2);
            }
        }, true, 2, ImmediateThinScheduler.INSTANCE).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1);
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(3);
        ts.assertValues(1, 2, 2, 3);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(2);

        ts.assertValues(1, 2, 2, 3, 3, 4);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void mapperScheduledLong() {
        TestSubscriber<String> ts = Flowable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMap(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName())
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

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayErrorScheduledLong() {
        TestSubscriber<String> ts = Flowable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMapDelayError(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName())
                        .repeat(1000)
                        .observeOn(Schedulers.io());
            }
        }, false, 2, Schedulers.single())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void mapperDelayError2ScheduledLong() {
        TestSubscriber<String> ts = Flowable.range(1, 1000)
        .hide()
        .observeOn(Schedulers.computation())
        .concatMapDelayError(new Function<Integer, Flowable<String>>() {
            @Override
            public Flowable<String> apply(Integer t) throws Throwable {
                return Flowable.just(Thread.currentThread().getName())
                        .repeat(1000)
                        .observeOn(Schedulers.io());
            }
        }, true, 2, Schedulers.single())
        .distinct()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertTrue(ts.values().toString(), ts.values().get(0).startsWith("RxSingleScheduler-"));
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.concatMap(new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer v) throws Throwable {
                        return Flowable.just(v).hide();
                    }
                }, 2, ImmediateThinScheduler.INSTANCE);
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.concatMapDelayError(new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer v) throws Throwable {
                        return Flowable.just(v).hide();
                    }
                }, false, 2, ImmediateThinScheduler.INSTANCE);
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayErrorTillEnd() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.concatMapDelayError(new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer v) throws Throwable {
                        return Flowable.just(v).hide();
                    }
                }, true, 2, ImmediateThinScheduler.INSTANCE);
            }
        });
    }
}
