/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;


public class FlowablePublishFunctionTest {
    @Test
    public void concatTakeFirstLastCompletes() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 3).publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return Flowable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);

        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void concatTakeFirstLastBackpressureCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);

        Flowable.range(1, 6).publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return Flowable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1); // make sure take() doesn't go unbounded
        ts.request(4);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(5);

        ts.assertValues(1, 2, 3, 4, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void canBeCancelled() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return Flowable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);

        ps.onNext(1);
        ps.onNext(2);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.cancel();

        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }

    @Test
    public void invalidPrefetch() {
        try {
            Flowable.<Integer>never().publish(
                    Functions.<Flowable<Integer>>identity(), -99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void takeCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o.take(1);
            }
        }).subscribe(ts);

        ps.onNext(1);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());

    }

    @Test
    public void oneStartOnly() {

        final AtomicInteger startCount = new AtomicInteger();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                startCount.incrementAndGet();
            }
        };

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o.take(1);
            }
        }).subscribe(ts);

        Assert.assertEquals(1, startCount.get());
    }

    @Test
    public void takeCompletesUnsafe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o.take(1);
            }
        }).subscribe(ts);

        ps.onNext(1);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }

    @Test
    public void directCompletesUnsafe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o;
            }
        }).subscribe(ts);

        ps.onNext(1);
        ps.onComplete();

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();

        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }

    @Test
    public void overflowMissingBackpressureException() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o;
            }
        }).subscribe(ts);

        for (int i = 0; i < Flowable.bufferSize() * 2; i++) {
            ps.onNext(i);
        }

        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();

        Assert.assertEquals("Could not emit value due to lack of requests",
                ts.errors().get(0).getMessage());
        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }

    @Test
    public void overflowMissingBackpressureExceptionDelayed() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishProcessor<Integer> ps = PublishProcessor.create();

        new FlowablePublishMulticast<Integer, Integer>(ps, new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o;
            }
        }, Flowable.bufferSize(), true).subscribe(ts);

        for (int i = 0; i < Flowable.bufferSize() * 2; i++) {
            ps.onNext(i);
        }

        ts.request(Flowable.bufferSize());

        ts.assertValueCount(Flowable.bufferSize());
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();

        Assert.assertEquals("Could not emit value due to lack of requests", ts.errors().get(0).getMessage());
        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }

    @Test
    public void emptyIdentityMapped() {
        Flowable.empty()
        .publish(Functions.<Flowable<Object>>identity())
        .test()
        .assertResult()
        ;
    }

    @Test
    public void independentlyMapped() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.range(1, 5);
            }
        }).test(0);

        assertTrue("pp has no Subscribers?!", pp.hasSubscribers());

        ts.assertNoValues()
        .assertNoErrors()
        .assertNotComplete();

        ts.request(5);

        ts.assertResult(1, 2, 3, 4, 5);

        assertFalse("pp has Subscribers?!", pp.hasSubscribers());
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.publish(Functions.<Flowable<Integer>>identity());
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void frontOverflow() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < 9; i++) {
                    s.onNext(i);
                }
            }
        }
        .publish(Functions.<Flowable<Integer>>identity(), 8)
        .test(0)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void errorResubscribe() {
        Flowable.error(new TestException())
        .publish(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.onErrorResumeNext(f);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedInputCrash() {
        Flowable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .publish(Functions.<Flowable<Integer>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void error() {
        new FlowablePublishMulticast<Integer, Integer>(Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException())),
                Functions.<Flowable<Integer>>identity(), 16, true)
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void backpressuredEmpty() {
        Flowable.<Integer>empty()
        .publish(Functions.<Flowable<Integer>>identity())
        .test(0L)
        .assertResult();
    }

    @Test
    public void oneByOne() {
        Flowable.range(1, 10)
        .publish(Functions.<Flowable<Integer>>identity())
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void completeCancelRaceNoRequest() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cancel();
                    onComplete();
                }
            }
        };

        pp.publish(Functions.<Flowable<Integer>>identity()).subscribe(ts);

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        ts.assertResult(1);
    }

    @Test
    public void inputOutputSubscribeRace() {
        Flowable<Integer> source = Flowable.just(1)
                .publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Flowable<Integer> f) throws Exception {
                        return f.subscribeOn(Schedulers.single());
                    }
                });

        for (int i = 0; i < 500; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);
        }
    }

    @Test
    public void inputOutputSubscribeRace2() {
        Flowable<Integer> source = Flowable.just(1).subscribeOn(Schedulers.single())
                .publish(Functions.<Flowable<Integer>>identity());

        for (int i = 0; i < 500; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);
        }
    }

    @Test
    public void sourceSubscriptionDelayed() {
        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>(0L);

            Flowable.just(1)
            .publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(final Flowable<Integer> f) throws Exception {
                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            f.subscribe(ts1);
                        }
                    };

                    Runnable r2 = new Runnable() {
                        @Override
                        public void run() {
                            for (int j = 0; j < 100; j++) {
                                ts1.request(1);
                            }
                        }
                    };

                    TestHelper.race(r1, r2);
                    return f;
                }
            }).test()
            .assertResult(1);

            ts1.assertResult(1);
        }
    }

    @Test
    public void longFlow() {
        Flowable.range(1, 1000000)
        .publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.mergeArray(
                        v.filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer w) throws Exception {
                                return w % 2 == 0;
                            }
                        }),
                        v.filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer w) throws Exception {
                                return w % 2 != 0;
                            }
                        }));
            }
        })
        .takeLast(1)
        .test()
        .assertResult(1000000);
    }

    @Test
    public void longFlow2() {
        Flowable.range(1, 100000)
        .publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.mergeArray(
                        v.filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer w) throws Exception {
                                return w % 2 == 0;
                            }
                        }),
                        v.filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer w) throws Exception {
                                return w % 2 != 0;
                            }
                        }));
            }
        })
        .test()
        .assertValueCount(100000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void longFlowHidden() {
        Flowable.range(1, 1000000).hide()
        .publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.mergeArray(
                        v.filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer w) throws Exception {
                                return w % 2 == 0;
                            }
                        }),
                        v.filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer w) throws Exception {
                                return w % 2 != 0;
                            }
                        }));
            }
        })
        .takeLast(1)
        .test()
        .assertResult(1000000);
    }
}
