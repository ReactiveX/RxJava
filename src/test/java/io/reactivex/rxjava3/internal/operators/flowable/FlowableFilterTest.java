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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableFilterTest extends RxJavaTest {

    @Test
    public void filter() {
        Flowable<String> w = Flowable.just("one", "two", "three");
        Flowable<String> flowable = w.filter(new Predicate<String>() {

            @Override
            public boolean test(String t1) {
                return t1.equals("two");
            }
        });

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, Mockito.never()).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, Mockito.never()).onNext("three");
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    /**
     * Make sure we are adjusting subscriber.request() for filtered items.
     * @throws InterruptedException if the test is interrupted
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void withBackpressure() throws InterruptedException {
        Flowable<String> w = Flowable.just("one", "two", "three");
        Flowable<String> f = w.filter(new Predicate<String>() {

            @Override
            public boolean test(String t1) {
                return t1.equals("three");
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        TestSubscriber<String> ts = new TestSubscriber<String>() {

            @Override
            public void onComplete() {
                System.out.println("onComplete");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onNext(String t) {
                System.out.println("Received: " + t);
                // request more each time we receive
                request(1);
            }

        };
        // this means it will only request "one" and "two", expecting to receive them before requesting more
        ts.request(2);

        f.subscribe(ts);

        // this will wait forever unless OperatorTake handles the request(n) on filtered items
        latch.await();
    }

    /**
     * Make sure we are adjusting subscriber.request() for filtered items.
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void withBackpressure2() throws InterruptedException {
        Flowable<Integer> w = Flowable.range(1, Flowable.bufferSize() * 2);
        Flowable<Integer> f = w.filter(new Predicate<Integer>() {

            @Override
            public boolean test(Integer t1) {
                return t1 > 100;
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {

            @Override
            public void onComplete() {
                System.out.println("onComplete");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onNext(Integer t) {
                System.out.println("Received: " + t);
                // request more each time we receive
                request(1);
            }
        };
        // this means it will only request 1 item and expect to receive more
        ts.request(1);

        f.subscribe(ts);

        // this will wait forever unless OperatorTake handles the request(n) on filtered items
        latch.await();
    }

    @Test
    public void functionCrashUnsubscribes() {

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        pp.filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                throw new TestException();
            }
        }).subscribe(ts);

        Assert.assertTrue("Not subscribed?", pp.hasSubscribers());

        pp.onNext(1);

        Assert.assertFalse("Subscribed?", pp.hasSubscribers());

        ts.assertError(TestException.class);
    }

    @Test
    public void doesntRequestOnItsOwn() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);

        Flowable.range(1, 10).filter(Functions.alwaysTrue()).subscribe(ts);

        ts.assertNoValues();

        ts.request(10);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void conditional() {
        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void conditionalNone() {
        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .filter(Functions.alwaysFalse())
        .test()
        .assertResult();
    }

    @Test
    public void conditionalNone2() {
        Flowable.range(1, 5)
        .filter(Functions.alwaysFalse())
        .filter(Functions.alwaysFalse())
        .test()
        .assertResult();
    }

    @Test
    public void conditionalFusedSync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void conditionalFusedSync2() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .filter(Functions.alwaysFalse())
        .filter(Functions.alwaysFalse())
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult();
    }

    @Test
    public void conditionalFusedAsync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .filter(Functions.alwaysTrue())
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        up.onNext(1);
        up.onNext(2);
        up.onNext(3);
        up.onNext(4);
        up.onNext(5);
        up.onComplete();

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void conditionalFusedNoneAsync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .filter(Functions.alwaysTrue())
        .filter(Functions.alwaysFalse())
        .subscribe(ts);

        up.onNext(1);
        up.onNext(2);
        up.onNext(3);
        up.onNext(4);
        up.onNext(5);
        up.onComplete();

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void conditionalFusedNoneAsync2() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .filter(Functions.alwaysFalse())
        .filter(Functions.alwaysFalse())
        .subscribe(ts);

        up.onNext(1);
        up.onNext(2);
        up.onNext(3);
        up.onNext(4);
        up.onNext(5);
        up.onComplete();

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
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
            .filter(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    return true;
                }
            })
            .filter(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
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
    public void mapCrashesBeforeFilter() {
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
    public void syncFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void syncNoneFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .filter(Functions.alwaysFalse())
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult();
    }

    @Test
    public void syncNoneFused2() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .filter(Functions.alwaysFalse())
        .filter(Functions.alwaysFalse())
        .subscribe(ts);

        ts.assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult();
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
            .filter(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
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
    public void sourceIgnoresCancel2() {
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
            .filter(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
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
    public void sourceIgnoresCancelConditional2() {
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
            .filter(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
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
    public void dispose() {
        TestHelper.checkDisposed(Flowable.range(1, 5).filter(Functions.alwaysTrue()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.filter(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void fusedSync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 4);
    }

    @Test
    public void fusedAsync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> us = UnicastProcessor.create();

        us
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 4);
    }

    @Test
    public void fusedReject() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY | QueueFuseable.BOUNDARY);

        Flowable.range(1, 5)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertResult(2, 4);
    }

    @Test
    public void filterThrows() {
        Flowable.range(1, 5)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
}
