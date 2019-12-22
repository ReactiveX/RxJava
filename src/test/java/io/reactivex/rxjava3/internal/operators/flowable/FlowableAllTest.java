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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableAllTest extends RxJavaTest {

    @Test
    public void all() {
        Flowable<String> obs = Flowable.just("one", "two", "six");

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(true);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void notAll() {
        Flowable<String> obs = Flowable.just("one", "two", "three", "six");

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(false);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void empty() {
        Flowable<String> obs = Flowable.empty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(true);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void error() {
        Throwable error = new Throwable();
        Flowable<String> obs = Flowable.error(error);

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void followingFirst() {
        Flowable<Integer> f = Flowable.fromArray(1, 3, 5, 6);
        Single<Boolean> allOdd = f.all(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 1;
            }
        });

        assertFalse(allOdd.blockingGet());
    }

    @Test
    public void issue1935NoUnsubscribeDownstream() {
        Flowable<Integer> source = Flowable.just(1)
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer t1) {
                    return false;
                }
            })
            .flatMapPublisher(new Function<Boolean, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Boolean t1) {
                    return Flowable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    public void backpressureIfOneRequestedOneShouldBeDelivered() {
        TestObserverEx<Boolean> to = new TestObserverEx<>();

        Flowable.empty().all(new Predicate<Object>() {
            @Override
            public boolean test(Object t) {
                return false;
            }
        }).subscribe(to);

        to.assertTerminated();
        to.assertNoErrors();
        to.assertComplete();

        to.assertValue(true);
    }

    @Test
    public void predicateThrowsExceptionAndValueInCauseMessage() {
        TestObserverEx<Boolean> to = new TestObserverEx<>();

        final IllegalArgumentException ex = new IllegalArgumentException();

        Flowable.just("Boo!").all(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        })
        .subscribe(to);

        to.assertTerminated();
        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void allFlowable() {
        Flowable<String> obs = Flowable.just("one", "two", "six");

        Subscriber<Boolean> subscriber = TestHelper.mockSubscriber();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .toFlowable()
        .subscribe(subscriber);

        verify(subscriber).onSubscribe((Subscription)any());
        verify(subscriber).onNext(true);
        verify(subscriber).onComplete();
        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void notAllFlowable() {
        Flowable<String> obs = Flowable.just("one", "two", "three", "six");

        Subscriber <Boolean> subscriber = TestHelper.mockSubscriber();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .toFlowable()
        .subscribe(subscriber);

        verify(subscriber).onSubscribe((Subscription)any());
        verify(subscriber).onNext(false);
        verify(subscriber).onComplete();
        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void emptyFlowable() {
        Flowable<String> obs = Flowable.empty();

        Subscriber <Boolean> subscriber = TestHelper.mockSubscriber();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .toFlowable()
        .subscribe(subscriber);

        verify(subscriber).onSubscribe((Subscription)any());
        verify(subscriber).onNext(true);
        verify(subscriber).onComplete();
        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void errorFlowable() {
        Throwable error = new Throwable();
        Flowable<String> obs = Flowable.error(error);

        Subscriber <Boolean> subscriber = TestHelper.mockSubscriber();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .toFlowable()
        .subscribe(subscriber);

        verify(subscriber).onSubscribe((Subscription)any());
        verify(subscriber).onError(error);
        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void followingFirstFlowable() {
        Flowable<Integer> f = Flowable.fromArray(1, 3, 5, 6);
        Flowable<Boolean> allOdd = f.all(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 1;
            }
        })
        .toFlowable()
        ;

        assertFalse(allOdd.blockingFirst());
    }

    @Test
    public void issue1935NoUnsubscribeDownstreamFlowable() {
        Flowable<Integer> source = Flowable.just(1)
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer t1) {
                    return false;
                }
            })
            .toFlowable()
            .flatMap(new Function<Boolean, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Boolean t1) {
                    return Flowable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            })
            ;

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    public void backpressureIfNoneRequestedNoneShouldBeDeliveredFlowable() {
        TestSubscriber<Boolean> ts = new TestSubscriber<>(0L);
        Flowable.empty().all(new Predicate<Object>() {
            @Override
            public boolean test(Object t1) {
                return false;
            }
        })
        .toFlowable()
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void backpressureIfOneRequestedOneShouldBeDeliveredFlowable() {
        TestSubscriberEx<Boolean> ts = new TestSubscriberEx<>(1L);

        Flowable.empty().all(new Predicate<Object>() {
            @Override
            public boolean test(Object t) {
                return false;
            }
        })
        .toFlowable()
        .subscribe(ts);

        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertComplete();

        ts.assertValue(true);
    }

    @Test
    public void predicateThrowsExceptionAndValueInCauseMessageFlowable() {
        TestSubscriberEx<Boolean> ts = new TestSubscriberEx<>();

        final IllegalArgumentException ex = new IllegalArgumentException();

        Flowable.just("Boo!").all(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        })
        .toFlowable()
        .subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).all(Functions.alwaysTrue()).toFlowable());

        TestHelper.checkDisposed(Flowable.just(1).all(Functions.alwaysTrue()));
    }

    @Test
    public void predicateThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());

                    subscriber.onNext(1);
                    subscriber.onNext(2);
                    subscriber.onError(new TestException());
                    subscriber.onComplete();
                }
            }
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .toFlowable()
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void predicateThrowsObservable() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());

                    subscriber.onNext(1);
                    subscriber.onNext(2);
                    subscriber.onError(new TestException());
                    subscriber.onComplete();
                }
            }
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .toFlowable()
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.all(Functions.alwaysTrue());
            }
        }, false, 1, 1, true);

        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.all(Functions.alwaysTrue()).toFlowable();
            }
        }, false, 1, 1, true);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Flowable<Object> f) throws Exception {
                return f.all(Functions.alwaysTrue()).toFlowable();
            }
        });
        TestHelper.checkDoubleOnSubscribeFlowableToSingle(new Function<Flowable<Object>, Single<Boolean>>() {
            @Override
            public Single<Boolean> apply(Flowable<Object> f) throws Exception {
                return f.all(Functions.alwaysTrue());
            }
        });
    }
}
