/*
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.UnicastProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableDoOnEachTest extends RxJavaTest {

    Subscriber<String> subscribedSubscriber;
    Subscriber<String> sideEffectSubscriber;

    @Before
    public void before() {
        subscribedSubscriber = TestHelper.mockSubscriber();
        sideEffectSubscriber = TestHelper.mockSubscriber();
    }

    @Test
    public void doOnEach() {
        Flowable<String> base = Flowable.just("a", "b", "c");
        Flowable<String> doOnEach = base.doOnEach(sideEffectSubscriber);

        doOnEach.subscribe(subscribedSubscriber);

        // ensure the leaf observer is still getting called
        verify(subscribedSubscriber, never()).onError(any(Throwable.class));
        verify(subscribedSubscriber, times(1)).onNext("a");
        verify(subscribedSubscriber, times(1)).onNext("b");
        verify(subscribedSubscriber, times(1)).onNext("c");
        verify(subscribedSubscriber, times(1)).onComplete();

        // ensure our injected observer is getting called
        verify(sideEffectSubscriber, never()).onError(any(Throwable.class));
        verify(sideEffectSubscriber, times(1)).onNext("a");
        verify(sideEffectSubscriber, times(1)).onNext("b");
        verify(sideEffectSubscriber, times(1)).onNext("c");
        verify(sideEffectSubscriber, times(1)).onComplete();
    }

    @Test
    public void doOnEachWithError() {
        Flowable<String> base = Flowable.just("one", "fail", "two", "three", "fail");
        Flowable<String> errs = base.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                return s;
            }
        });

        Flowable<String> doOnEach = errs.doOnEach(sideEffectSubscriber);

        doOnEach.subscribe(subscribedSubscriber);
        verify(subscribedSubscriber, times(1)).onNext("one");
        verify(subscribedSubscriber, never()).onNext("two");
        verify(subscribedSubscriber, never()).onNext("three");
        verify(subscribedSubscriber, never()).onComplete();
        verify(subscribedSubscriber, times(1)).onError(any(Throwable.class));

        verify(sideEffectSubscriber, times(1)).onNext("one");
        verify(sideEffectSubscriber, never()).onNext("two");
        verify(sideEffectSubscriber, never()).onNext("three");
        verify(sideEffectSubscriber, never()).onComplete();
        verify(sideEffectSubscriber, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void doOnEachWithErrorInCallback() {
        Flowable<String> base = Flowable.just("one", "two", "fail", "three");
        Flowable<String> doOnEach = base.doOnNext(new Consumer<String>() {
            @Override
            public void accept(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
            }
        });

        doOnEach.subscribe(subscribedSubscriber);
        verify(subscribedSubscriber, times(1)).onNext("one");
        verify(subscribedSubscriber, times(1)).onNext("two");
        verify(subscribedSubscriber, never()).onNext("three");
        verify(subscribedSubscriber, never()).onComplete();
        verify(subscribedSubscriber, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void issue1451Case1() {
        // https://github.com/Netflix/RxJava/issues/1451
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < expectedCount; i++) {
            Flowable
                    .just(Boolean.TRUE, Boolean.FALSE)
                    .takeWhile(new Predicate<Boolean>() {
                        @Override
                        public boolean test(Boolean value) {
                            return value;
                        }
                    })
                    .toList()
                    .doOnSuccess(new Consumer<List<Boolean>>() {
                        @Override
                        public void accept(List<Boolean> booleans) {
                            count.incrementAndGet();
                        }
                    })
                    .subscribe();
        }
        assertEquals(expectedCount, count.get());
    }

    @Test
    public void issue1451Case2() {
        // https://github.com/Netflix/RxJava/issues/1451
        final int expectedCount = 3;
        final AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < expectedCount; i++) {
            Flowable
                    .just(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE)
                    .takeWhile(new Predicate<Boolean>() {
                        @Override
                        public boolean test(Boolean value) {
                            return value;
                        }
                    })
                    .toList()
                    .doOnSuccess(new Consumer<List<Boolean>>() {
                        @Override
                        public void accept(List<Boolean> booleans) {
                            count.incrementAndGet();
                        }
                    })
                    .subscribe();
        }
        assertEquals(expectedCount, count.get());
    }

    @Test
    public void onErrorThrows() {
        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();

        Flowable.error(new TestException())
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(CompositeException.class);

        CompositeException ex = (CompositeException)ts.errors().get(0);

        List<Throwable> exceptions = ex.getExceptions();
        assertEquals(2, exceptions.size());
        Assert.assertTrue(exceptions.get(0) instanceof TestException);
        Assert.assertTrue(exceptions.get(1) instanceof TestException);
    }

    @Test
    public void ignoreCancel() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Object>() {
                @Override
                public void subscribe(Subscriber<? super Object> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onError(new IOException());
                    s.onComplete();
                }
            })
            .doOnNext(new Consumer<Object>() {
                @Override
                public void accept(Object e) throws Exception {
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
    public void onErrorAfterCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Object>() {
                @Override
                public void subscribe(Subscriber<? super Object> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new TestException());
                }
            })
            .doAfterTerminate(new Action() {
                @Override
                public void run() throws Exception {
                    throw new IOException();
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
    public void onCompleteAfterCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Object>() {
                @Override
                public void subscribe(Subscriber<? super Object> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onComplete();
                }
            })
            .doAfterTerminate(new Action() {
                @Override
                public void run() throws Exception {
                    throw new IOException();
                }
            })
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrash() {
        Flowable.fromPublisher(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onComplete();
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void ignoreCancelConditional() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Object>() {
                @Override
                public void subscribe(Subscriber<? super Object> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onError(new IOException());
                    s.onComplete();
                }
            })
            .doOnNext(new Consumer<Object>() {
                @Override
                public void accept(Object e) throws Exception {
                    throw new TestException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void ignoreCancelConditional2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Object>() {
                @Override
                public void subscribe(Subscriber<? super Object> s) {
                    ConditionalSubscriber<? super Object> cs = (ConditionalSubscriber<? super Object>)s;
                    cs.onSubscribe(new BooleanSubscription());
                    cs.tryOnNext(1);
                    cs.tryOnNext(2);
                    cs.onError(new IOException());
                    cs.onComplete();
                }
            })
            .doOnNext(new Consumer<Object>() {
                @Override
                public void accept(Object e) throws Exception {
                    throw new TestException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorAfterCrashConditional() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Object>() {
                @Override
                public void subscribe(Subscriber<? super Object> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new TestException());
                }
            })
            .doAfterTerminate(new Action() {
                @Override
                public void run() throws Exception {
                    throw new IOException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteAfter() {
        final int[] call = { 0 };
        Flowable.just(1)
        .doAfterTerminate(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, call[0]);
    }

    @Test
    public void onCompleteAfterCrashConditional() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.fromPublisher(new Publisher<Object>() {
                @Override
                public void subscribe(Subscriber<? super Object> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onComplete();
                }
            })
            .doAfterTerminate(new Action() {
                @Override
                public void run() throws Exception {
                    throw new IOException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrashConditional() {
        Flowable.fromPublisher(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onComplete();
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                throw new IOException();
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void onErrorOnErrorCrashConditional() {
        TestSubscriberEx<Object> ts = Flowable.error(new TestException("Outer"))
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .filter(Functions.alwaysTrue())
        .to(TestHelper.testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(ts.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void fused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        final int[] call = { 0, 0 };

        Flowable.range(1, 5)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                call[0]++;
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[1]++;
            }
        })
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    public void fusedOnErrorCrash() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        final int[] call = { 0 };

        Flowable.range(1, 5)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void fusedConditional() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        final int[] call = { 0, 0 };

        Flowable.range(1, 5)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                call[0]++;
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[1]++;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    public void fusedOnErrorCrashConditional() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        final int[] call = { 0 };

        Flowable.range(1, 5)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void fusedAsync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        final int[] call = { 0, 0 };

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                call[0]++;
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[1]++;
            }
        })
        .subscribe(ts);

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    public void fusedAsyncConditional() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        final int[] call = { 0, 0 };

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                call[0]++;
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[1]++;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    public void fusedAsyncConditional2() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        final int[] call = { 0, 0 };

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up.hide()
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                call[0]++;
            }
        })
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[1]++;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        TestHelper.emit(up, 1, 2, 3, 4, 5);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, call[0]);
        assertEquals(1, call[1]);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).doOnEach(new TestSubscriber<>()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.doOnEach(new TestSubscriber<>());
            }
        });
    }

    @Test
    public void doOnNextDoOnErrorFused() {
        ConnectableFlowable<Integer> cf = Flowable.just(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException("First");
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Second");
            }
        })
        .publish();

        TestSubscriberEx<Integer> ts = cf.to(TestHelper.<Integer>testConsumer());
        cf.connect();

        ts.assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class, "First");
        TestHelper.assertError(ts, 1, TestException.class, "Second");
    }

    @Test
    public void doOnNextDoOnErrorCombinedFused() {
        ConnectableFlowable<Integer> cf = Flowable.just(1)
                .compose(new FlowableTransformer<Integer, Integer>() {
                    @Override
                    public Publisher<Integer> apply(Flowable<Integer> v) {
                        return new FlowableDoOnEach<>(v,
                                new Consumer<Integer>() {
                                    @Override
                                    public void accept(Integer v) throws Exception {
                                        throw new TestException("First");
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable e) throws Exception {
                                        throw new TestException("Second");
                                    }
                                },
                                Functions.EMPTY_ACTION
                                ,
                                Functions.EMPTY_ACTION
                        );
                    }
                })
        .publish();

        TestSubscriberEx<Integer> ts = cf.to(TestHelper.<Integer>testConsumer());
        cf.connect();

        ts.assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class, "First");
        TestHelper.assertError(ts, 1, TestException.class, "Second");
    }

    @Test
    public void doOnNextDoOnErrorFused2() {
        ConnectableFlowable<Integer> cf = Flowable.just(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException("First");
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Second");
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Third");
            }
        })
        .publish();

        TestSubscriberEx<Integer> ts = cf.to(TestHelper.<Integer>testConsumer());
        cf.connect();

        ts.assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class, "First");
        TestHelper.assertError(ts, 1, TestException.class, "Second");
        TestHelper.assertError(ts, 2, TestException.class, "Third");
    }

    @Test
    public void doOnNextDoOnErrorFusedConditional() {
        ConnectableFlowable<Integer> cf = Flowable.just(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException("First");
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Second");
            }
        })
        .filter(Functions.alwaysTrue())
        .publish();

        TestSubscriberEx<Integer> ts = cf.to(TestHelper.<Integer>testConsumer());
        cf.connect();

        ts.assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class, "First");
        TestHelper.assertError(ts, 1, TestException.class, "Second");
    }

    @Test
    public void doOnNextDoOnErrorFusedConditional2() {
        ConnectableFlowable<Integer> cf = Flowable.just(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException("First");
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Second");
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Third");
            }
        })
        .filter(Functions.alwaysTrue())
        .publish();

        TestSubscriberEx<Integer> ts = cf.to(TestHelper.<Integer>testConsumer());
        cf.connect();

        ts.assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class, "First");
        TestHelper.assertError(ts, 1, TestException.class, "Second");
        TestHelper.assertError(ts, 2, TestException.class, "Third");
    }

    @Test
    public void doOnNextDoOnErrorCombinedFusedConditional() {
        ConnectableFlowable<Integer> cf = Flowable.just(1)
                .compose(new FlowableTransformer<Integer, Integer>() {
                    @Override
                    public Publisher<Integer> apply(Flowable<Integer> v) {
                        return new FlowableDoOnEach<>(v,
                                new Consumer<Integer>() {
                                    @Override
                                    public void accept(Integer v) throws Exception {
                                        throw new TestException("First");
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable e) throws Exception {
                                        throw new TestException("Second");
                                    }
                                },
                                Functions.EMPTY_ACTION
                                ,
                                Functions.EMPTY_ACTION
                        );
                    }
                })
        .filter(Functions.alwaysTrue())
        .publish();

        TestSubscriberEx<Integer> ts = cf.to(TestHelper.<Integer>testConsumer());
        cf.connect();

        ts.assertFailure(CompositeException.class);

        TestHelper.assertError(ts, 0, TestException.class, "First");
        TestHelper.assertError(ts, 1, TestException.class, "Second");
    }
}
