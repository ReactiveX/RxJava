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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Flowable<Integer> w = Flowable.just(1, 2);
        Single<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Flowable<Integer> w = Flowable.just(1, 2);
        Single<Boolean> observable = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithOneItem() {
        Flowable<Integer> w = Flowable.just(1);
        Single<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Flowable<Integer> w = Flowable.just(1);
        Single<Boolean> observable = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithEmpty() {
        Flowable<Integer> w = Flowable.empty();
        Single<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Flowable<Integer> w = Flowable.empty();
        Single<Boolean> observable = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithPredicate1() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Single<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testExists1() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Single<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithPredicate2() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Single<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 1;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Flowable<Integer> w = Flowable.empty();
        Single<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWithFollowingFirst() {
        Flowable<Integer> o = Flowable.fromArray(1, 3, 5, 6);
        Single<Boolean> anyEven = o.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 0;
            }
        });

        assertTrue(anyEven.blockingGet());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Flowable<Integer> source = Flowable.just(1).isEmpty()
            .flatMapPublisher(new Function<Boolean, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Boolean t1) {
                    return Flowable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    @Ignore("Single doesn't do backpressure")
    public void testBackpressureIfNoneRequestedNoneShouldBeDelivered() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();

        Flowable.just(1).any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        })
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDelivered() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();
        Flowable.just(1).any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).subscribe(ts);

        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValue(true);
    }

    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();
        final IllegalArgumentException ex = new IllegalArgumentException();

        Flowable.just("Boo!").any(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        }).subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void testAnyWithTwoItemsFlowable() {
        Flowable<Integer> w = Flowable.just(1, 2);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        })
        .toFlowable()
        ;

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithTwoItemsFlowable() {
        Flowable<Integer> w = Flowable.just(1, 2);
        Flowable<Boolean> observable = w.isEmpty().toFlowable();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithOneItemFlowable() {
        Flowable<Integer> w = Flowable.just(1);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).toFlowable();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithOneItemFlowable() {
        Flowable<Integer> w = Flowable.just(1);
        Single<Boolean> observable = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        observable.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAnyWithEmptyFlowable() {
        Flowable<Integer> w = Flowable.empty();
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).toFlowable();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithEmptyFlowable() {
        Flowable<Integer> w = Flowable.empty();
        Flowable<Boolean> observable = w.isEmpty().toFlowable();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate1Flowable() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        }).toFlowable();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testExists1Flowable() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        }).toFlowable();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate2Flowable() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 1;
            }
        }).toFlowable();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmptyAndPredicateFlowable() {
        // If the source is empty, always output false.
        Flowable<Integer> w = Flowable.empty();
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        }).toFlowable();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirstFlowable() {
        Flowable<Integer> o = Flowable.fromArray(1, 3, 5, 6);
        Flowable<Boolean> anyEven = o.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 0;
            }
        }).toFlowable();

        assertTrue(anyEven.blockingFirst());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstreamFlowable() {
        Flowable<Integer> source = Flowable.just(1).isEmpty()
            .flatMapPublisher(new Function<Boolean, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Boolean t1) {
                    return Flowable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    public void testBackpressureIfNoneRequestedNoneShouldBeDeliveredFlowable() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(0L);

        Flowable.just(1).any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        }).toFlowable()
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDeliveredFlowable() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(1L);
        Flowable.just(1).any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).toFlowable().subscribe(ts);

        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValue(true);
    }

    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessageFlowable() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>();
        final IllegalArgumentException ex = new IllegalArgumentException();

        Flowable.just("Boo!").any(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        }).toFlowable().subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).any(Functions.alwaysTrue()).toFlowable());

        TestHelper.checkDisposed(Flowable.just(1).any(Functions.alwaysTrue()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Flowable<Object> o) throws Exception {
                return o.any(Functions.alwaysTrue()).toFlowable();
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowableToSingle(new Function<Flowable<Object>, Single<Boolean>>() {
            @Override
            public Single<Boolean> apply(Flowable<Object> o) throws Exception {
                return o.any(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void predicateThrowsSuppressOthers() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());

                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new IOException());
                    observer.onComplete();
                }
            }
            .any(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .toFlowable()
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceSingle() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> observer) {
                    observer.onSubscribe(new BooleanSubscription());
                    observer.onError(new TestException("First"));

                    observer.onNext(1);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }
            .any(Functions.alwaysTrue())
            .test()
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
