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

package io.reactivex.rxjava3.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscribers.ForEachWhileSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableSubscriberTest {

    /**
     * Should request n for whatever the final Subscriber asks for.
     */
    @Test
    public void requestFromFinalSubscribeWithRequestValue() {
        TestSubscriber<String> s = new TestSubscriber<>(0L);
        s.request(10);
        final AtomicLong r = new AtomicLong();
        s.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }

            @Override
            public void cancel() {

            }

        });
        assertEquals(10, r.get());
    }

    /**
     * Should request -1 for infinite.
     */
    @Test
    public void requestFromFinalSubscribeWithoutRequestValue() {
        TestSubscriber<String> s = new TestSubscriber<>();
        final AtomicLong r = new AtomicLong();
        s.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }

            @Override
            public void cancel() {

            }

        });
        assertEquals(Long.MAX_VALUE, r.get());
    }

    @Test
    public void requestFromChainedOperator() throws Throwable {
        TestSubscriber<String> s = new TestSubscriber<>(10L);
        FlowableOperator<String, String> o = new FlowableOperator<String, String>() {
            @Override
            public Subscriber<? super String> apply(final Subscriber<? super String> s1) {
                return new FlowableSubscriber<String>() {

                    @Override
                    public void onSubscribe(Subscription a) {
                        s1.onSubscribe(a);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String t) {

                    }

                };
            }
        };
        Subscriber<? super String> ns = o.apply(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }

            @Override
            public void cancel() {

            }

        });
        assertEquals(10, r.get());
    }

    @Test
    public void requestFromDecoupledOperator() throws Throwable {
        TestSubscriber<String> s = new TestSubscriber<>(0L);
        FlowableOperator<String, String> o = new FlowableOperator<String, String>() {
            @Override
            public Subscriber<? super String> apply(final Subscriber<? super String> s1) {
                return new FlowableSubscriber<String>() {

                    @Override
                    public void onSubscribe(Subscription a) {
                        s1.onSubscribe(a);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String t) {

                    }

                };
            }
        };
        s.request(10);
        Subscriber<? super String> ns = o.apply(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }

            @Override
            public void cancel() {

            }

        });
        assertEquals(10, r.get());
    }

    @Test
    public void requestFromDecoupledOperatorThatRequestsN() throws Throwable {
        TestSubscriber<String> s = new TestSubscriber<>(10L);
        final AtomicLong innerR = new AtomicLong();
        FlowableOperator<String, String> o = new FlowableOperator<String, String>() {
            @Override
            public Subscriber<? super String> apply(Subscriber<? super String> child) {
                // we want to decouple the chain so set our own Producer on the child instead of it coming from the parent
                child.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        innerR.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });

                ResourceSubscriber<String> as = new ResourceSubscriber<String>() {

                    @Override
                    protected void onStart() {
                        // we request 99 up to the parent
                        request(99);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String t) {

                    }
                };

                return as;
            }
        };
        Subscriber<? super String> ns = o.apply(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }

            @Override
            public void cancel() {

            }

        });
        assertEquals(99, r.get());
        assertEquals(10, innerR.get());
    }

    @Test
    public void requestToFlowable() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(3L);
        final AtomicLong requested = new AtomicLong();
        Flowable.<Integer>unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        }).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void requestThroughMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Flowable.<Integer>unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        }).map(Functions.<Integer>identity()).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void requestThroughTakeThatReducesRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Flowable.<Integer>unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }
        }).take(2).subscribe(ts);

        assertEquals(2, requested.get());
    }

    @Test
    public void requestThroughTakeWhereRequestIsSmallerThanTake() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Flowable.<Integer>unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                    @Override
                    public void cancel() {

                    }

                });
            }
        }).take(10).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void onStartCalledOnceViaSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Flowable.just(1, 2, 3, 4).take(2).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
                request(1);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                request(1);
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void onStartCalledOnceViaUnsafeSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Flowable.just(1, 2, 3, 4).take(2).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
                request(1);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                request(1);
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void onStartCalledOnceViaLift() {
        final AtomicInteger c = new AtomicInteger();
        Flowable.just(1, 2, 3, 4).lift(new FlowableOperator<Integer, Integer>() {

            @Override
            public Subscriber<? super Integer> apply(final Subscriber<? super Integer> child) {
                return new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        c.incrementAndGet();
                        request(1);
                    }

                    @Override
                    public void onComplete() {
                        child.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Integer t) {
                        child.onNext(t);
                        request(1);
                    }

                };
            }

        }).subscribe();

        assertEquals(1, c.get());
    }

    @Test
    public void onStartRequestsAreAdditive() {
        final List<Integer> list = new ArrayList<>();
        Flowable.just(1, 2, 3, 4, 5)
        .subscribe(new DefaultSubscriber<Integer>() {
            @Override
            public void onStart() {
                request(3);
                request(2);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                list.add(t);
            }});
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void onStartRequestsAreAdditiveAndOverflowBecomesMaxValue() {
        final List<Integer> list = new ArrayList<>();
        Flowable.just(1, 2, 3, 4, 5).subscribe(new DefaultSubscriber<Integer>() {
            @Override
            public void onStart() {
                request(2);
                request(Long.MAX_VALUE - 1);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                list.add(t);
            }});
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void forEachWhile() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final List<Integer> list = new ArrayList<>();

        Disposable d = pp.forEachWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                list.add(v);
                return v < 3;
            }
        });

        assertFalse(d.isDisposed());

        pp.onNext(1);
        pp.onNext(2);
        pp.onNext(3);

        assertFalse(pp.hasSubscribers());

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void doubleSubscribe() {
        ForEachWhileSubscriber<Integer> s = new ForEachWhileSubscriber<>(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        }, Functions.<Throwable>emptyConsumer(), Functions.EMPTY_ACTION);

        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            s.onSubscribe(new BooleanSubscription());

            BooleanSubscription bs = new BooleanSubscription();
            s.onSubscribe(bs);

            assertTrue(bs.isCancelled());

            TestHelper.assertError(list, 0, IllegalStateException.class, "Subscription already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void suppressAfterCompleteEvents() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final TestSubscriber<Integer> ts = new TestSubscriber<>();
            ts.onSubscribe(new BooleanSubscription());

            ForEachWhileSubscriber<Integer> s = new ForEachWhileSubscriber<>(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    ts.onNext(v);
                    return true;
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    ts.onError(e);
                }
            }, new Action() {
                @Override
                public void run() throws Exception {
                    ts.onComplete();
                }
            });

            s.onComplete();
            s.onNext(1);
            s.onError(new TestException());
            s.onComplete();

            ts.assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextCrashes() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(new BooleanSubscription());

        ForEachWhileSubscriber<Integer> s = new ForEachWhileSubscriber<>(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new TestException();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                ts.onError(e);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                ts.onComplete();
            }
        });

        BooleanSubscription b = new BooleanSubscription();

        s.onSubscribe(b);
        s.onNext(1);

        assertTrue(b.isCancelled());
        ts.assertFailure(TestException.class);
    }

    @Test
    public void onErrorThrows() {
        ForEachWhileSubscriber<Integer> s = new ForEachWhileSubscriber<>(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        }, new Action() {
            @Override
            public void run() throws Exception {

            }
        });

        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            s.onSubscribe(new BooleanSubscription());

            s.onError(new TestException("Outer"));

            TestHelper.assertError(list, 0, CompositeException.class);
            List<Throwable> cel = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(cel, 0, TestException.class, "Outer");
            TestHelper.assertError(cel, 1, TestException.class, "Inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteThrows() {
        ForEachWhileSubscriber<Integer> s = new ForEachWhileSubscriber<>(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                throw new TestException("Inner");
            }
        });

        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            s.onSubscribe(new BooleanSubscription());

            s.onComplete();

            TestHelper.assertUndeliverable(list, 0, TestException.class, "Inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void subscribeConsumerConsumerWithError() {
        final List<Integer> list = new ArrayList<>();

        Flowable.<Integer>error(new TestException()).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                list.add(100);
            }
        });

        assertEquals(Arrays.asList(100), list);
    }

    @Test
    public void methodTestCancelled() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.test(Long.MAX_VALUE, true);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void safeSubscriberAlreadySafe() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.just(1).safeSubscribe(new SafeSubscriber<>(ts));

        ts.assertResult(1);
    }

    @Test
    public void methodTestNoCancel() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.test(Long.MAX_VALUE, false);

        assertTrue(pp.hasSubscribers());
    }

    @Test
    public void subscribeConsumerConsumer() {
        final List<Integer> list = new ArrayList<>();

        Flowable.just(1).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                list.add(100);
            }
        });

        assertEquals(Arrays.asList(1), list);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void pluginNull() {
        RxJavaPlugins.setOnFlowableSubscribe(new BiFunction<Flowable, Subscriber, Subscriber>() {
            @Override
            public Subscriber apply(Flowable a, Subscriber b) throws Exception {
                return null;
            }
        });

        try {
            try {

                Flowable.just(1).test();
                fail("Should have thrown");
            } catch (NullPointerException ex) {
                assertEquals("The RxJavaPlugins.onSubscribe hook returned a null FlowableSubscriber. Please check the handler provided to RxJavaPlugins.setOnFlowableSubscribe for invalid null returns. Further reading: https://github.com/ReactiveX/RxJava/wiki/Plugins", ex.getMessage());
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    static final class BadFlowable extends Flowable<Integer> {
        @Override
        protected void subscribeActual(Subscriber<? super Integer> s) {
            throw new IllegalArgumentException();
        }
    }

    @Test
    public void subscribeActualThrows() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        try {
            try {
                new BadFlowable().test();
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                if (!(ex.getCause() instanceof IllegalArgumentException)) {
                    fail(ex.toString() + ": Should be NPE(IAE)");
                }
            }

            TestHelper.assertError(list, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
