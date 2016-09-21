/**
 * Copyright 2016 Netflix, Inc.
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableAllTest {

    @Test
    public void testAll() {
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
    public void testNotAll() {
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
    public void testEmpty() {
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
    public void testError() {
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
    public void testFollowingFirst() {
        Flowable<Integer> o = Flowable.fromArray(1, 3, 5, 6);
        Single<Boolean> allOdd = o.all(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 1;
            }
        });

        assertFalse(allOdd.blockingGet());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
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
    @Ignore("No backpressure in Single")
    public void testBackpressureIfNoneRequestedNoneShouldBeDelivered() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();
        Flowable.empty().all(new Predicate<Object>() {
            @Override
            public boolean test(Object t1) {
                return false;
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDelivered() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();

        Flowable.empty().all(new Predicate<Object>() {
            @Override
            public boolean test(Object t) {
                return false;
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

        Flowable.just("Boo!").all(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        })
        .subscribe(ts);

        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void testAllFlowable() {
        Flowable<String> obs = Flowable.just("one", "two", "six");

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .toFlowable()
        .subscribe(observer);

        verify(observer).onSubscribe((Subscription)any());
        verify(observer).onNext(true);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testNotAllFlowable() {
        Flowable<String> obs = Flowable.just("one", "two", "three", "six");

        Subscriber <Boolean> observer = TestHelper.mockSubscriber();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .toFlowable()
        .subscribe(observer);

        verify(observer).onSubscribe((Subscription)any());
        verify(observer).onNext(false);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testEmptyFlowable() {
        Flowable<String> obs = Flowable.empty();

        Subscriber <Boolean> observer = TestHelper.mockSubscriber();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .toFlowable()
        .subscribe(observer);

        verify(observer).onSubscribe((Subscription)any());
        verify(observer).onNext(true);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testErrorFlowable() {
        Throwable error = new Throwable();
        Flowable<String> obs = Flowable.error(error);

        Subscriber <Boolean> observer = TestHelper.mockSubscriber();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .toFlowable()
        .subscribe(observer);

        verify(observer).onSubscribe((Subscription)any());
        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testFollowingFirstFlowable() {
        Flowable<Integer> o = Flowable.fromArray(1, 3, 5, 6);
        Flowable<Boolean> allOdd = o.all(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 1;
            }
        })
        .toFlowable()
        ;

        assertFalse(allOdd.blockingFirst());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstreamFlowable() {
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
    public void testBackpressureIfNoneRequestedNoneShouldBeDeliveredFlowable() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(0L);
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
    public void testBackpressureIfOneRequestedOneShouldBeDeliveredFlowable() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(1L);

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
    public void testPredicateThrowsExceptionAndValueInCauseMessageFlowable() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>();

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
}