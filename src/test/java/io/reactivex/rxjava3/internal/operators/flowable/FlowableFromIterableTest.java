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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.util.CrashingIterable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableFromIterableTest extends RxJavaTest {

    @Test(expected = NullPointerException.class)
    public void nullValue() {
        Flowable.fromIterable(null);
    }

    @Test
    public void listIterable() {
        Flowable<String> flowable = Flowable.fromIterable(Arrays.<String> asList("one", "two", "three"));

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    /**
     * This tests the path that can not optimize based on size so must use setProducer.
     */
    @Test
    public void rawIterable() {
        Iterable<String> it = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {

                    int i;

                    @Override
                    public boolean hasNext() {
                        return i < 3;
                    }

                    @Override
                    public String next() {
                        return String.valueOf(++i);
                    }

                    @Override
                    public void remove() {
                    }

                };
            }

        };
        Flowable<String> flowable = Flowable.fromIterable(it);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext("1");
        verify(subscriber, times(1)).onNext("2");
        verify(subscriber, times(1)).onNext("3");
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void observableFromIterable() {
        Flowable<String> flowable = Flowable.fromIterable(Arrays.<String> asList("one", "two", "three"));

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void backpressureViaRequest() {
        ArrayList<Integer> list = new ArrayList<>(Flowable.bufferSize());
        for (int i = 1; i <= Flowable.bufferSize() + 1; i++) {
            list.add(i);
        }
        Flowable<Integer> f = Flowable.fromIterable(list);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);

        ts.assertNoValues();
        ts.request(1);

        f.subscribe(ts);

        ts.assertValue(1);
        ts.request(2);
        ts.assertValues(1, 2, 3);
        ts.request(3);
        ts.assertValues(1, 2, 3, 4, 5, 6);
        ts.request(list.size());
        ts.assertTerminated();
    }

    @Test
    public void noBackpressure() {
        Flowable<Integer> f = Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5));

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>(0L);

        ts.assertNoValues();
        ts.request(Long.MAX_VALUE); // infinite

        f.subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertTerminated();
    }

    @Test
    public void subscribeMultipleTimes() {
        Flowable<Integer> f = Flowable.fromIterable(Arrays.asList(1, 2, 3));

        for (int i = 0; i < 10; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<>();

            f.subscribe(ts);

            ts.assertValues(1, 2, 3);
            ts.assertNoErrors();
            ts.assertComplete();
        }
    }

    @Test
    public void fromIterableRequestOverflow() throws InterruptedException {
        Flowable<Integer> f = Flowable.fromIterable(Arrays.asList(1, 2, 3, 4));

        final int expectedCount = 4;
        final CountDownLatch latch = new CountDownLatch(expectedCount);

        f.subscribeOn(Schedulers.computation())
        .subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                request(2);
            }

            @Override
            public void onComplete() {
                //ignore
            }

            @Override
            public void onError(Throwable e) {
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(Integer t) {
                latch.countDown();
                request(Long.MAX_VALUE - 1);
            }});
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void fromEmptyIterableWhenZeroRequestedShouldStillEmitOnCompletedEagerly() {

        final AtomicBoolean completed = new AtomicBoolean(false);

        Flowable.fromIterable(Collections.emptyList()).subscribe(new DefaultSubscriber<Object>() {

            @Override
            public void onStart() {
//                request(0);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Object t) {

            }});
        assertTrue(completed.get());
    }

    @Test
    public void doesNotCallIteratorHasNextMoreThanRequiredWithBackpressure() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    int count = 1;

                    @Override
                    public void remove() {
                        // ignore
                    }

                    @Override
                    public boolean hasNext() {
                        if (count > 1) {
                            called.set(true);
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                };
            }
        };
        Flowable.fromIterable(iterable).take(1).subscribe();
        assertFalse(called.get());
    }

    @Test
    public void doesNotCallIteratorHasNextMoreThanRequiredFastPath() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = new Iterable<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                        // ignore
                    }

                    int count = 1;

                    @Override
                    public boolean hasNext() {
                        if (count > 1) {
                            called.set(true);
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return count++;
                    }

                };
            }
        };
        Flowable.fromIterable(iterable).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                // unsubscribe on first emission
                cancel();
            }
        });
        assertFalse(called.get());
    }

    @Test
    public void getIteratorThrows() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                throw new TestException("Forced failure");
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void hasNextThrowsImmediately() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        throw new TestException("Forced failure");
                    }

                    @Override
                    public Integer next() {
                        return null;
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void hasNextThrowsSecondTimeFastpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;
                    @Override
                    public boolean hasNext() {
                        if (++count >= 2) {
                            throw new TestException("Forced failure");
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void hasNextThrowsSecondTimeSlowpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;
                    @Override
                    public boolean hasNext() {
                        if (++count >= 2) {
                            throw new TestException("Forced failure");
                        }
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<>(5);

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertValues(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void nextThrowsFastpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        throw new TestException("Forced failure");
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void nextThrowsSlowpath() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        throw new TestException("Forced failure");
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<>(5);

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void deadOnArrival() {
        Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        // ignored
                    }
                };
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<>(5);
        ts.cancel();

        Flowable.fromIterable(it).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

    }

    @Test
    public void fusionWithConcatMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.fromIterable(Arrays.asList(1, 2, 3, 4)).concatMap(
        new Function<Integer, Flowable  <Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return Flowable.range(v, 2);
            }
        }).subscribe(ts);

        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void fusedAPICalls() {
        Flowable.fromIterable(Arrays.asList(1, 2, 3))
        .subscribe(new FlowableSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {
                @SuppressWarnings("unchecked")
                QueueSubscription<Integer> qs = (QueueSubscription<Integer>)s;

                assertFalse(qs.isEmpty());

                try {
                    assertEquals(1, qs.poll().intValue());
                } catch (Throwable ex) {
                    throw new AssertionError(ex);
                }

                assertFalse(qs.isEmpty());

                qs.clear();

                List<Throwable> errors = TestHelper.trackPluginErrors();
                try {
                    qs.request(-99);

                    TestHelper.assertError(errors, 0, IllegalArgumentException.class, "n > 0 required but it was -99");
                } finally {
                    RxJavaPlugins.reset();
                }
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @Test
    public void normalConditional() {
        Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5))
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalConditionalBackpressured() {
        Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5))
        .filter(Functions.alwaysTrue())
        .test(5L)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalConditionalBackpressured2() {
        Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5))
        .filter(Functions.alwaysTrue())
        .to(TestHelper.<Integer>testSubscriber(4L))
        .assertSubscribed()
        .assertValues(1, 2, 3, 4)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void emptyConditional() {
        Flowable.fromIterable(Arrays.asList(1, 2, 3, 4, 5))
        .filter(Functions.alwaysFalse())
        .test()
        .assertResult();
    }

    @Test
    public void nullConditional() {
        Flowable.fromIterable(Arrays.asList(1, null, 3, 4, 5))
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(NullPointerException.class, 1);
    }

    @Test
    public void nullConditionalBackpressured() {
        Flowable.fromIterable(Arrays.asList(1, null, 3, 4, 5))
        .filter(Functions.alwaysTrue())
        .test(5L)
        .assertFailure(NullPointerException.class, 1);
    }

    @Test
    public void normalConditionalCrash() {
        Flowable.fromIterable(new CrashingIterable(100, 2, 100))
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class, 0);
    }

    @Test
    public void normalConditionalCrash2() {
        Flowable.fromIterable(new CrashingIterable(100, 100, 2))
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class, 0);
    }

    @Test
    public void normalConditionalCrashBackpressured() {
        Flowable.fromIterable(new CrashingIterable(100, 2, 100))
        .filter(Functions.alwaysTrue())
        .test(5L)
        .assertFailure(TestException.class, 0);
    }

    @Test
    public void normalConditionalCrashBackpressured2() {
        Flowable.fromIterable(new CrashingIterable(100, 100, 2))
        .filter(Functions.alwaysTrue())
        .test(5L)
        .assertFailure(TestException.class, 0);
    }

    @Test
    public void normalConditionalLong() {
        Flowable.fromIterable(new CrashingIterable(100, 10 * 1000 * 1000, 10 * 1000 * 1000))
        .filter(Functions.alwaysTrue())
        .take(1000 * 1000)
        .to(TestHelper.<Integer>testConsumer())
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void normalConditionalLong2() {
        Flowable.fromIterable(new CrashingIterable(100, 10 * 1000 * 1000, 10 * 1000 * 1000))
        .filter(Functions.alwaysTrue())
        .rebatchRequests(128)
        .take(1000 * 1000)
        .to(TestHelper.<Integer>testConsumer())
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void requestRaceConditional() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            Flowable.fromIterable(Arrays.asList(1, 2, 3, 4))
            .filter(Functions.alwaysTrue())
            .subscribe(ts);

            TestHelper.race(r, r);
        }
    }

    @Test
    public void requestRaceConditional2() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            Flowable.fromIterable(Arrays.asList(1, 2, 3, 4))
            .filter(Functions.alwaysFalse())
            .subscribe(ts);

            TestHelper.race(r, r);
        }
    }

    @Test
    public void requestCancelConditionalRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Flowable.fromIterable(Arrays.asList(1, 2, 3, 4))
            .filter(Functions.alwaysTrue())
            .subscribe(ts);

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void requestCancelConditionalRace2() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(Long.MAX_VALUE);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Flowable.fromIterable(Arrays.asList(1, 2, 3, 4))
            .filter(Functions.alwaysTrue())
            .subscribe(ts);

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void requestCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Flowable.fromIterable(Arrays.asList(1, 2, 3, 4))
            .subscribe(ts);

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void requestCancelRace2() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(Long.MAX_VALUE);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Flowable.fromIterable(Arrays.asList(1, 2, 3, 4))
            .subscribe(ts);

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void fusionRejected() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ASYNC);

        Flowable.fromIterable(Arrays.asList(1, 2, 3))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3);
    }

    @Test
    public void fusionClear() {
        Flowable.fromIterable(Arrays.asList(1, 2, 3))
        .subscribe(new FlowableSubscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                @SuppressWarnings("unchecked")
                QueueSubscription<Integer> qs = (QueueSubscription<Integer>)s;

                qs.requestFusion(QueueFuseable.ANY);

                try {
                    assertEquals(1, qs.poll().intValue());
                } catch (Throwable ex) {
                    fail(ex.toString());
                }

                qs.clear();
                try {
                    assertNull(qs.poll());
                } catch (Throwable ex) {
                    fail(ex.toString());
                }
            }

            @Override
            public void onNext(Integer value) {
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
    public void iteratorThrows() {
        Flowable.fromIterable(new CrashingIterable(1, 100, 100))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNext2Throws() {
        Flowable.fromIterable(new CrashingIterable(100, 2, 100))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void hasNextCancels() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.fromIterable(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;

                    @Override
                    public boolean hasNext() {
                        if (++count == 2) {
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
        })
        .subscribe(ts);

        ts.assertValue(1)
        .assertNoErrors()
        .assertNotComplete();
    }
}
