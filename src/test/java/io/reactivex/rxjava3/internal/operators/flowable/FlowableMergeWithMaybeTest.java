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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.MaybeSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableMergeWithMaybeTest extends RxJavaTest {

    @Test
    public void normal() {
        Flowable.range(1, 5)
        .mergeWith(Maybe.just(100))
        .test()
        .assertResult(1, 2, 3, 4, 5, 100);
    }

    @Test
    public void emptyOther() {
        Flowable.range(1, 5)
        .mergeWith(Maybe.<Integer>empty())
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalLong() {
        Flowable.range(1, 512)
        .mergeWith(Maybe.just(100))
        .test()
        .assertValueCount(513)
        .assertComplete();
    }

    @Test
    public void normalLongRequestExact() {
        Flowable.range(1, 512)
        .mergeWith(Maybe.just(100))
        .test(513)
        .assertValueCount(513)
        .assertComplete();
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .mergeWith(Maybe.just(100))
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void cancel() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();
        final MaybeSubject<Integer> cs = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs).test();

        assertTrue(pp.hasSubscribers());
        assertTrue(cs.hasObservers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
        assertFalse(cs.hasObservers());
    }

    @Test
    public void normalBackpressured() {
        Flowable.range(1, 5).mergeWith(
                Maybe.just(100)
        )
        .test(0L)
        .assertEmpty()
        .requestMore(2)
        .assertValues(100, 1)
        .requestMore(2)
        .assertValues(100, 1, 2, 3)
        .requestMore(2)
        .assertResult(100, 1, 2, 3, 4, 5);
    }

    @Test
    public void mainError() {
        Flowable.error(new TestException())
        .mergeWith(Maybe.just(100))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        Flowable.never()
        .mergeWith(Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void completeRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final MaybeSubject<Integer> cs = MaybeSubject.create();

            TestSubscriber<Integer> ts = pp.mergeWith(cs).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cs.onSuccess(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1, 1);
        }
    }

    @Test
    public void onNextSlowPath() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();
        final MaybeSubject<Integer> cs = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs).subscribeWith(new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onNext(2);
                }
            }
        });

        pp.onNext(1);
        cs.onSuccess(3);

        pp.onNext(4);
        pp.onComplete();

        ts.assertResult(1, 2, 3, 4);
    }

    @Test
    public void onSuccessSlowPath() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();
        final MaybeSubject<Integer> cs = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs).subscribeWith(new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cs.onSuccess(2);
                }
            }
        });

        pp.onNext(1);

        pp.onNext(3);
        pp.onComplete();

        ts.assertResult(1, 2, 3);
    }

    @Test
    public void onSuccessSlowPathBackpressured() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();
        final MaybeSubject<Integer> cs = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs).subscribeWith(new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cs.onSuccess(2);
                }
            }
        });

        pp.onNext(1);

        pp.onNext(3);
        pp.onComplete();

        ts.request(2);
        ts.assertResult(1, 2, 3);
    }

    @Test
    public void onSuccessFastPathBackpressuredRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final MaybeSubject<Integer> cs = MaybeSubject.create();

            final TestSubscriber<Integer> ts = pp.mergeWith(cs).subscribeWith(new TestSubscriber<>(0));

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    cs.onSuccess(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(2);
                }
            };

            TestHelper.race(r1, r2);

            pp.onNext(2);
            pp.onComplete();

            ts.assertResult(1, 2);
        }
    }

    @Test
    public void onErrorMainOverflow() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final AtomicReference<Subscriber<?>> subscriber = new AtomicReference<>();
            TestSubscriber<Integer> ts = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    subscriber.set(s);
                }
            }
            .mergeWith(Maybe.<Integer>error(new IOException()))
            .test();

            subscriber.get().onError(new TestException());

            ts.assertFailure(IOException.class)
            ;

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorOtherOverflow() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.error(new IOException())
            .mergeWith(Maybe.error(new TestException()))
            .test()
            .assertFailure(IOException.class)
            ;

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextRequestRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final MaybeSubject<Integer> cs = MaybeSubject.create();

            final TestSubscriber<Integer> ts = pp.mergeWith(cs).test(0);

            pp.onNext(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(3);
                }
            };

            TestHelper.race(r1, r2);

            cs.onSuccess(1);
            pp.onComplete();

            ts.assertResult(0, 1, 1);
        }
    }

    @Test
    public void doubleOnSubscribeMain() {
        TestHelper.checkDoubleOnSubscribeFlowable(
                new Function<Flowable<Object>, Publisher<Object>>() {
                    @Override
                    public Publisher<Object> apply(Flowable<Object> f)
                            throws Exception {
                        return f.mergeWith(Maybe.just(1));
                    }
                }
        );
    }

    @Test
    public void noRequestOnError() {
        Flowable.empty()
        .mergeWith(Maybe.error(new TestException()))
        .test(0)
        .assertFailure(TestException.class);
    }

    @Test
    public void drainExactRequestCancel() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();
        final MaybeSubject<Integer> cs = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs)
                .take(2)
                .subscribeWith(new TestSubscriber<Integer>(2) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    cs.onSuccess(2);
                }
            }
        });

        pp.onNext(1);

        pp.onComplete();

        ts.request(2);
        ts.assertResult(1, 2);
    }

    @Test
    public void drainRequestWhenLimitReached() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();
        final MaybeSubject<Integer> cs = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs)
                .subscribeWith(new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    for (int i = 0; i < Flowable.bufferSize() - 1; i++) {
                        pp.onNext(i + 2);
                    }
                }
            }
        });

        cs.onSuccess(1);

        pp.onComplete();

        ts.request(2);
        ts.assertValueCount(Flowable.bufferSize());
        ts.assertComplete();
    }

    @Test
    public void cancelOtherOnMainError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(ms).test();

        assertTrue(pp.hasSubscribers());
        assertTrue(ms.hasObservers());

        pp.onError(new TestException());

        ts.assertFailure(TestException.class);

        assertFalse("main has observers!", pp.hasSubscribers());
        assertFalse("other has observers", ms.hasObservers());
    }

    @Test
    public void cancelMainOnOtherError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(ms).test();

        assertTrue(pp.hasSubscribers());
        assertTrue(ms.hasObservers());

        ms.onError(new TestException());

        ts.assertFailure(TestException.class);

        assertFalse("main has observers!", pp.hasSubscribers());
        assertFalse("other has observers", ms.hasObservers());
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.mergeWith(Maybe.just(1).hide());
            }
        });
    }
}
