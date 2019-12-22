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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableConcatMap.WeakScalarSubscription;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableConcatMapTest extends RxJavaTest {

    @Test
    public void weakSubscriptionRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0);
        WeakScalarSubscription<Integer> ws = new WeakScalarSubscription<>(1, ts);
        ts.onSubscribe(ws);

        ws.request(0);

        ts.assertEmpty();

        ws.request(1);

        ts.assertResult(1);

        ws.request(1);

        ts.assertResult(1);
    }

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
        })
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
        })
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
        })
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
        })
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
        })
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
        })
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
        }, false, 2)
        .test()
        .assertFailure(NullPointerException.class, 1, 2, 3);
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
                });
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
                }, false, 2);
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
                }, true, 2);
            }
        });
    }
}
