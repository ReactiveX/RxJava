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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.util.CrashingIterable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.*;

public class SingleFlatMapIterableFlowableTest {

    @Test
    public void normal() {

        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void emptyIterable() {

        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Collections.<Integer>emptyList();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void error() {

        Single.<Integer>error(new TestException()).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void backpressure() {

        TestSubscriber<Integer> ts = Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .test(0);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void take() {
        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void fused() {
        TestSubscriber<Integer> to = SubscriberFusion.newTest(QueueDisposable.ANY);

        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(to);

        to.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult(1, 2);
        ;
    }

    @Test
    public void fusedNoSync() {
        TestSubscriber<Integer> to = SubscriberFusion.newTest(QueueDisposable.SYNC);

        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(to);

        to.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueDisposable.NONE))
        .assertResult(1, 2);
        ;
    }

    @Test
    public void iteratorCrash() {

        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new CrashingIterable(1, 100, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNextCrash() {

        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new CrashingIterable(100, 1, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextCrash() {

        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new CrashingIterable(100, 100, 1);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void hasNextCrash2() {

        Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new CrashingIterable(100, 2, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void async1() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .hide()
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async2() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async3() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .take(500 * 1000)
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async4() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .observeOn(Schedulers.single())
        .take(500 * 1000)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void fusedEmptyCheck() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return Arrays.asList(1, 2, 3);
                    }
        }).subscribe(new FlowableSubscriber<Integer>() {
            QueueSubscription<Integer> qd;
            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Subscription d) {
                qd = (QueueSubscription<Integer>)d;

                assertEquals(QueueSubscription.ASYNC, qd.requestFusion(QueueSubscription.ANY));
            }

            @Override
            public void onNext(Integer value) {
                assertFalse(qd.isEmpty());

                qd.clear();

                assertTrue(qd.isEmpty());

                qd.cancel();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void hasNextThrowsUnbounded() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return new CrashingIterable(100, 2, 100);
                    }
                })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void nextThrowsUnbounded() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return new CrashingIterable(100, 100, 1);
                    }
                })
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void hasNextThrows() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return new CrashingIterable(100, 2, 100);
                    }
                })
        .test(2L)
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void nextThrows() {
        Single.just(1)
        .flattenAsFlowable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return new CrashingIterable(100, 100, 1);
                    }
                })
        .test(2L)
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void requestBefore() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.singleElement().flattenAsFlowable(
            new Function<Integer, Iterable<Integer>>() {
                @Override
                public Iterable<Integer> apply(Integer v) throws Exception {
                    return Arrays.asList(1, 2, 3);
                }
            })
            .test(5L)
            .assertEmpty();
        }
    }

    @Test
    public void requestCreateInnerRace() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.onNext(1);

            final TestSubscriber<Integer> ts = ps.singleElement().flattenAsFlowable(
            new Function<Integer, Iterable<Integer>>() {
                @Override
                public Iterable<Integer> apply(Integer v) throws Exception {
                    return Arrays.asList(a);
                }
            })
            .test(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                    for (int i = 0; i < 500; i++) {
                        ts.request(1);
                    }
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 500; i++) {
                        ts.request(1);
                    }
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void cancelCreateInnerRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.onNext(1);

            final TestSubscriber<Integer> ts = ps.singleElement().flattenAsFlowable(
            new Function<Integer, Iterable<Integer>>() {
                @Override
                public Iterable<Integer> apply(Integer v) throws Exception {
                    return Arrays.asList(1, 2, 3);
                }
            })
            .test(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void slowPathCancelAfterHasNext() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        Single.just(1)
        .flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new Iterable<Integer>() {
                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<Integer>() {
                            int count;
                            @Override
                            public boolean hasNext() {
                                if (count++ == 2) {
                                    ts.cancel();
                                }
                                return true;
                            }

                            @Override
                            public Integer next() {
                                return 1;
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        })
        .subscribe(ts);

        ts.request(3);
        ts.assertValues(1, 1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void fastPathCancelAfterHasNext() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        Single.just(1)
        .flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new Iterable<Integer>() {
                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<Integer>() {
                            int count;
                            @Override
                            public boolean hasNext() {
                                if (count++ == 2) {
                                    ts.cancel();
                                }
                                return true;
                            }

                            @Override
                            public Integer next() {
                                return 1;
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        })
        .subscribe(ts);

        ts.request(Long.MAX_VALUE);
        ts.assertValues(1, 1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void requestIteratorRace() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestSubscriber<Integer> ts = ps.singleOrError().flattenAsFlowable(new Function<Integer, Iterable<Integer>>() {
                @Override
                public Iterable<Integer> apply(Integer v) throws Exception {
                    return Arrays.asList(a);
                }
            }).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        ts.request(1);
                    }
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                    ps.onComplete();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }
}
