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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.operators.maybe.MaybeMergeArray.MergeMaybeObserver;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.operators.QueueSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class MaybeMergeArrayTest extends RxJavaTest {

    @Test
    public void normal() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.SYNC);

        Maybe.mergeArray(Maybe.just(1), Maybe.just(2))
        .subscribe(ts);
        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2);
    }

    @Test
    public void fusedPollMixed() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Maybe.mergeArray(Maybe.just(1), Maybe.<Integer>empty(), Maybe.just(2))
        .subscribe(ts);
        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fusedEmptyCheck() {
        Maybe.mergeArray(Maybe.just(1), Maybe.<Integer>empty(), Maybe.just(2))
        .subscribe(new FlowableSubscriber<Integer>() {
            QueueSubscription<Integer> qs;
            @Override
            public void onSubscribe(Subscription s) {
                qs = (QueueSubscription<Integer>)s;

                assertEquals(QueueFuseable.ASYNC, qs.requestFusion(QueueFuseable.ANY));
            }

            @Override
            public void onNext(Integer value) {
                assertFalse(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                qs.cancel();
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
    public void cancel() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        Maybe.mergeArray(Maybe.just(1), Maybe.<Integer>empty(), Maybe.just(2))
        .subscribe(ts);

        ts.cancel();
        ts.request(10);

        ts.assertEmpty();
    }

    @Test
    public void firstErrors() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        Maybe.mergeArray(Maybe.<Integer>error(new TestException()), Maybe.<Integer>empty(), Maybe.just(2))
        .subscribe(ts);

        ts.assertFailure(TestException.class);
    }

    @Test
    public void errorFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Maybe.mergeArray(Maybe.<Integer>error(new TestException()), Maybe.just(2))
        .subscribe(ts);
        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void errorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestSubscriber<Integer> ts = Maybe.mergeArray(ps1.singleElement(), ps2.singleElement())
                .test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex);
                    }
                };

                TestHelper.race(r1, r2);

                ts.assertFailure(Throwable.class);

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void mergeBadSource() {
        Maybe.mergeArray(new Maybe<Integer>() {
            @Override
            protected void subscribeActual(MaybeObserver<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onSuccess(1);
                observer.onSuccess(2);
                observer.onSuccess(3);
            }
        }, Maybe.never())
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void smallOffer2Throws() {
        Maybe.mergeArray(Maybe.never(), Maybe.never())
        .subscribe(new FlowableSubscriber<Object>() {

            @SuppressWarnings("rawtypes")
            @Override
            public void onSubscribe(Subscription s) {
                MergeMaybeObserver o = (MergeMaybeObserver)s;

                try {
                    o.queue.offer(1, 2);
                    fail("Should have thrown");
                } catch (UnsupportedOperationException ex) {
                    // expected
                }
            }

            @Override
            public void onNext(Object t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void largeOffer2Throws() {
        Maybe<Integer>[] a = new Maybe[1024];
        Arrays.fill(a, Maybe.never());
        Maybe.mergeArray(a)
        .subscribe(new FlowableSubscriber<Object>() {

            @SuppressWarnings("rawtypes")
            @Override
            public void onSubscribe(Subscription s) {
                MergeMaybeObserver o = (MergeMaybeObserver)s;

                try {
                    o.queue.offer(1, 2);
                    fail("Should have thrown");
                } catch (UnsupportedOperationException ex) {
                    // expected
                }

                o.queue.drop();
            }

            @Override
            public void onNext(Object t) {
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
    public void badRequest() {
        TestHelper.assertBadRequestReported(
                Maybe.mergeArray(MaybeSubject.create(), MaybeSubject.create())
        );
    }

    @Test
    public void cancel2() {
        TestHelper.checkDisposed(Maybe.mergeArray(MaybeSubject.create(), MaybeSubject.create()));
    }

    @Test
    public void take() {
        Maybe.mergeArray(Maybe.just(1), Maybe.empty(), Maybe.just(2))
        .doOnSubscribe(s -> s.request(Long.MAX_VALUE))
        .take(1)
        .test()
        .assertResult(1);
    }
}
