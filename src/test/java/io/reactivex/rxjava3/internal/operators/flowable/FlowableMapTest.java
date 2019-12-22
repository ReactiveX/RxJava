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

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableMapTest extends RxJavaTest {

    Subscriber<String> stringSubscriber;
    Subscriber<String> stringSubscriber2;

    static final BiFunction<String, Integer, String> APPEND_INDEX = new BiFunction<String, Integer, String>() {
        @Override
        public String apply(String value, Integer index) {
            return value + index;
        }
    };

    @Before
    public void before() {
        stringSubscriber = TestHelper.mockSubscriber();
        stringSubscriber2 = TestHelper.mockSubscriber();
    }

    @Test
    public void map() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Flowable<Map<String, String>> flowable = Flowable.just(m1, m2);

        Flowable<String> m = flowable.map(new Function<Map<String, String>, String>() {
            @Override
            public String apply(Map<String, String> map) {
                return map.get("firstName");
            }
        });

        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(1)).onNext("OneFirst");
        verify(stringSubscriber, times(1)).onNext("TwoFirst");
        verify(stringSubscriber, times(1)).onComplete();
    }

    @Test
    public void mapMany() {
        /* simulate a top-level async call which returns IDs */
        Flowable<Integer> ids = Flowable.just(1, 2);

        /* now simulate the behavior to take those IDs and perform nested async calls based on them */
        Flowable<String> m = ids.flatMap(new Function<Integer, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Integer id) {
                /* simulate making a nested async call which creates another Flowable */
                Flowable<Map<String, String>> subFlowable = null;
                if (id == 1) {
                    Map<String, String> m1 = getMap("One");
                    Map<String, String> m2 = getMap("Two");
                    subFlowable = Flowable.just(m1, m2);
                } else {
                    Map<String, String> m3 = getMap("Three");
                    Map<String, String> m4 = getMap("Four");
                    subFlowable = Flowable.just(m3, m4);
                }

                /* simulate kicking off the async call and performing a select on it to transform the data */
                return subFlowable.map(new Function<Map<String, String>, String>() {
                    @Override
                    public String apply(Map<String, String> map) {
                        return map.get("firstName");
                    }
                });
            }

        });
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(1)).onNext("OneFirst");
        verify(stringSubscriber, times(1)).onNext("TwoFirst");
        verify(stringSubscriber, times(1)).onNext("ThreeFirst");
        verify(stringSubscriber, times(1)).onNext("FourFirst");
        verify(stringSubscriber, times(1)).onComplete();
    }

    @Test
    public void mapMany2() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Flowable<Map<String, String>> flowable1 = Flowable.just(m1, m2);

        Map<String, String> m3 = getMap("Three");
        Map<String, String> m4 = getMap("Four");
        Flowable<Map<String, String>> flowable2 = Flowable.just(m3, m4);

        Flowable<Flowable<Map<String, String>>> f = Flowable.just(flowable1, flowable2);

        Flowable<String> m = f.flatMap(new Function<Flowable<Map<String, String>>, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Flowable<Map<String, String>> f) {
                return f.map(new Function<Map<String, String>, String>() {

                    @Override
                    public String apply(Map<String, String> map) {
                        return map.get("firstName");
                    }
                });
            }

        });
        m.subscribe(stringSubscriber);

        verify(stringSubscriber, never()).onError(any(Throwable.class));
        verify(stringSubscriber, times(1)).onNext("OneFirst");
        verify(stringSubscriber, times(1)).onNext("TwoFirst");
        verify(stringSubscriber, times(1)).onNext("ThreeFirst");
        verify(stringSubscriber, times(1)).onNext("FourFirst");
        verify(stringSubscriber, times(1)).onComplete();

    }

    @Test
    public void mapWithError() {
        final List<Throwable> errors = new ArrayList<>();

        Flowable<String> w = Flowable.just("one", "fail", "two", "three", "fail");
        Flowable<String> m = w.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s)) {
                    throw new TestException("Forced Failure");
                }
                return s;
            }
        }).doOnError(new Consumer<Throwable>() {

            @Override
            public void accept(Throwable t1) {
                errors.add(t1);
            }

        });

        m.subscribe(stringSubscriber);
        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, never()).onNext("two");
        verify(stringSubscriber, never()).onNext("three");
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(1)).onError(any(TestException.class));

        TestHelper.assertError(errors, 0, TestException.class, "Forced Failure");
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapWithIssue417() {
        Flowable.just(1).observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                }).blockingSingle();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapWithErrorInFuncAndThreadPoolScheduler() throws InterruptedException {
        // The error will throw in one of threads in the thread pool.
        // If map does not handle it, the error will disappear.
        // so map needs to handle the error by itself.
        Flowable<String> m = Flowable.just("one")
                .observeOn(Schedulers.computation())
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                });

        // block for response, expecting exception thrown
        m.blockingLast();
    }

    /**
     * While mapping over range(1,0).last() we expect NoSuchElementException since the sequence is empty.
     */
    @Test
    public void errorPassesThruMap() {
        assertNull(Flowable.range(1, 0).lastElement().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i;
            }

        }).blockingGet());
    }

    /**
     * We expect IllegalStateException to pass thru map.
     */
    @Test(expected = IllegalStateException.class)
    public void errorPassesThruMap2() {
        Flowable.error(new IllegalStateException()).map(new Function<Object, Object>() {

            @Override
            public Object apply(Object i) {
                return i;
            }

        }).blockingSingle();
    }

    /**
     * We expect an ArithmeticException exception here because last() emits a single value
     * but then we divide by 0.
     */
    @Test(expected = ArithmeticException.class)
    public void mapWithErrorInFunc() {
        Flowable.range(1, 1).lastElement().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i / 0;
            }

        }).blockingGet();
    }

    private static Map<String, String> getMap(String prefix) {
        Map<String, String> m = new HashMap<>();
        m.put("firstName", prefix + "First");
        m.put("lastName", prefix + "Last");
        return m;
    }

    @Test
    public void functionCrashUnsubscribes() {

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        pp.map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) {
                throw new TestException();
            }
        }).subscribe(ts);

        Assert.assertTrue("Not subscribed?", pp.hasSubscribers());

        pp.onNext(1);

        Assert.assertFalse("Subscribed?", pp.hasSubscribers());

        ts.assertError(TestException.class);
    }

    @Test
    public void mapFilter() {
        Flowable.range(1, 2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        })
        .test()
        .assertResult(2, 3);
    }

    @Test
    public void mapFilterMapperCrash() {
        Flowable.range(1, 2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapFilterHidden() {
        Flowable.range(1, 2).hide()
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        })
        .test()
        .assertResult(2, 3);
    }

    @Test
    public void mapFilterFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        })
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 3);
    }

    @Test
    public void mapFilterFusedHidden() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 2).hide()
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        })
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(2, 3);
    }

    @Test
    public void sourceIgnoresCancel() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Integer>() {
                @Override
                public void subscribe(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onError(new IOException());
                    s.onComplete();
                }
            })
            .map(new Function<Integer, Object>() {
                @Override
                public Object apply(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void mapFilterMapperCrashFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 2).hide()
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        })
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertFailure(TestException.class);
    }

    @Test
    public void sourceIgnoresCancelFilter() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Integer>() {
                @Override
                public void subscribe(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onError(new IOException());
                    s.onComplete();
                }
            })
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .filter(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    return true;
                }
            })
           .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void mapFilterFused2() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        })
        .subscribe(ts);

        up.onNext(1);
        up.onNext(2);
        up.onComplete();

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 3);
    }

    @Test
    public void sourceIgnoresCancelConditional() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Integer>() {
                @Override
                public void subscribe(Subscriber<? super Integer> s) {
                    ConditionalSubscriber<? super Integer> cs = (ConditionalSubscriber<? super Integer>)s;
                    cs.onSubscribe(new BooleanSubscription());
                    cs.tryOnNext(1);
                    cs.tryOnNext(2);
                    cs.onError(new IOException());
                    cs.onComplete();
                }
            })
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .filter(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    return true;
                }
            })
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.range(1, 5).map(Functions.identity()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.map(Functions.identity());
            }
        });
    }

    @Test
    public void fusedSync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .map(Functions.<Integer>identity())
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedAsync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> us = UnicastProcessor.create();

        us
        .map(Functions.<Integer>identity())
        .subscribe(ts);

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedReject() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY | QueueFuseable.BOUNDARY);

        Flowable.range(1, 5)
        .map(Functions.<Integer>identity())
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> f) throws Exception {
                return f.map(Functions.identity());
            }
        }, false, 1, 1, 1);
    }

}
