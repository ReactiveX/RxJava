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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.*;

public class FlowableMapTest {

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
    public void testMap() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Flowable<Map<String, String>> observable = Flowable.just(m1, m2);

        Flowable<String> m = observable.map(new Function<Map<String, String>, String>() {
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
    public void testMapMany() {
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
    public void testMapMany2() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Flowable<Map<String, String>> observable1 = Flowable.just(m1, m2);

        Map<String, String> m3 = getMap("Three");
        Map<String, String> m4 = getMap("Four");
        Flowable<Map<String, String>> observable2 = Flowable.just(m3, m4);

        Flowable<Flowable<Map<String, String>>> observable = Flowable.just(observable1, observable2);

        Flowable<String> m = observable.flatMap(new Function<Flowable<Map<String, String>>, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Flowable<Map<String, String>> o) {
                return o.map(new Function<Map<String, String>, String>() {

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
    public void testMapWithError() {
        Flowable<String> w = Flowable.just("one", "fail", "two", "three", "fail");
        Flowable<String> m = w.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                return s;
            }
        }).doOnError(new Consumer<Throwable>() {

            @Override
            public void accept(Throwable t1) {
                t1.printStackTrace();
            }

        });

        m.subscribe(stringSubscriber);
        verify(stringSubscriber, times(1)).onNext("one");
        verify(stringSubscriber, never()).onNext("two");
        verify(stringSubscriber, never()).onNext("three");
        verify(stringSubscriber, never()).onComplete();
        verify(stringSubscriber, times(1)).onError(any(Throwable.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapWithIssue417() {
        Flowable.just(1).observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                }).blockingSingle();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapWithErrorInFuncAndThreadPoolScheduler() throws InterruptedException {
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
    public void testErrorPassesThruMap() {
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
    public void testErrorPassesThruMap2() {
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
    public void testMapWithErrorInFunc() {
        Flowable.range(1, 1).lastElement().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i / 0;
            }

        }).blockingGet();
    }

    private static Map<String, String> getMap(String prefix) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("firstName", prefix + "First");
        m.put("lastName", prefix + "Last");
        return m;
    }

    @Test//(expected = OnErrorNotImplementedException.class)
    @Ignore("RS subscribers can't throw")
    public void testShouldNotSwallowOnErrorNotImplementedException() {
//        Flowable.just("a", "b").flatMap(new Function<String, Flowable<String>>() {
//            @Override
//            public Flowable<String> apply(String s) {
//                return Flowable.just(s + "1", s + "2");
//            }
//        }).flatMap(new Function<String, Flowable<String>>() {
//            @Override
//            public Flowable<String> apply(String s) {
//                return Flowable.error(new Exception("test"));
//            }
//        }).forEach(new Consumer<String>() {
//            @Override
//            public void accept(String s) {
//                System.out.println(s);
//            }
//        });
    }

    @Test//(expected = OnErrorNotImplementedException.class)
    @Ignore("RS subscribers can't throw")
    public void verifyExceptionIsThrownIfThereIsNoExceptionHandler() {
//
//        Flowable.OnSubscribe<Object> creator = new Flowable.OnSubscribe<Object>() {
//
//            @Override
//            public void call(Subscriber<? super Object> observer) {
//                observer.onNext("a");
//                observer.onNext("b");
//                observer.onNext("c");
//                observer.onComplete();
//            }
//        };
//
//        Func1<Object, Flowable<Object>> manyMapper = new Func1<Object, Flowable<Object>>() {
//
//            @Override
//            public Flowable<Object> call(Object object) {
//                return Flowable.just(object);
//            }
//        };
//
//        Func1<Object, Object> mapper = new Func1<Object, Object>() {
//            private int count = 0;
//
//            @Override
//            public Object call(Object object) {
//                ++count;
//                if (count > 2) {
//                    throw new RuntimeException();
//                }
//                return object;
//            }
//        };
//
//        Action1<Object> onNext = new Action1<Object>() {
//
//            @Override
//            public void call(Object object) {
//                System.out.println(object.toString());
//            }
//        };
//
//        try {
//            Flowable.create(creator).flatMap(manyMapper).map(mapper).subscribe(onNext);
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            throw e;
//        }
    }

    @Test
    public void functionCrashUnsubscribes() {

        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        ps.map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) {
                throw new TestException();
            }
        }).subscribe(ts);

        Assert.assertTrue("Not subscribed?", ps.hasSubscribers());

        ps.onNext(1);

        Assert.assertFalse("Subscribed?", ps.hasSubscribers());

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
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

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

        ts.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.SYNC))
        .assertResult(2, 3);
    }

    @Test
    public void mapFilterFusedHidden() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

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

        ts.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.NONE))
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
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

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

        ts.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.NONE))
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
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

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

        ts.assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
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
            public Flowable<Object> apply(Flowable<Object> o) throws Exception {
                return o.map(Functions.identity());
            }
        });
    }

    @Test
    public void fusedSync() {
        TestSubscriber<Integer> to = SubscriberFusion.newTest(QueueDisposable.ANY);

        Flowable.range(1, 5)
        .map(Functions.<Integer>identity())
        .subscribe(to);

        SubscriberFusion.assertFusion(to, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedAsync() {
        TestSubscriber<Integer> to = SubscriberFusion.newTest(QueueDisposable.ANY);

        UnicastProcessor<Integer> us = UnicastProcessor.create();

        us
        .map(Functions.<Integer>identity())
        .subscribe(to);

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        SubscriberFusion.assertFusion(to, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedReject() {
        TestSubscriber<Integer> to = SubscriberFusion.newTest(QueueDisposable.ANY | QueueDisposable.BOUNDARY);

        Flowable.range(1, 5)
        .map(Functions.<Integer>identity())
        .subscribe(to);

        SubscriberFusion.assertFusion(to, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> o) throws Exception {
                return o.map(Functions.identity());
            }
        }, false, 1, 1, 1);
    }

}
