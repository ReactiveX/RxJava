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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CancellationException;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;

public class SingleTakeUntilTest {

    @Test
    public void mainSuccessPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp)
        .test();

        source.onNext(1);
        source.onComplete();

        to.assertResult(1);
    }

    @Test
    public void mainSuccessSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.single(-99))
        .test();

        source.onNext(1);
        source.onComplete();

        to.assertResult(1);
    }


    @Test
    public void mainSuccessCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        source.onNext(1);
        source.onComplete();

        to.assertResult(1);
    }

    @Test
    public void mainErrorPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp)
        .test();

        source.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.single(-99))
        .test();

        source.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        source.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void otherOnNextPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp)
        .test();

        pp.onNext(1);

        to.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnNextSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.single(-99))
        .test();

        pp.onNext(1);
        pp.onComplete();

        to.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnNextCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        pp.onNext(1);
        pp.onComplete();

        to.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnCompletePublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp)
        .test();

        pp.onComplete();

        to.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnCompleteCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        pp.onComplete();

        to.assertFailure(CancellationException.class);
    }

    @Test
    public void otherErrorPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp)
        .test();

        pp.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void otherErrorSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.single(-99))
        .test();

        pp.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void otherErrorCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> to = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        pp.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void withPublisherDispose() {
        TestHelper.checkDisposed(Single.never().takeUntil(Flowable.never()));
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestObserver<Integer> to = pp1.singleOrError().takeUntil(pp2).test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp2.onError(ex);
                    }
                };

                TestHelper.race(r1, r2);

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void otherSignalsAndCompletes() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Single.just(1).takeUntil(Flowable.just(1).take(1))
            .test()
            .assertFailure(CancellationException.class);

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void flowableCancelDelayed() {
        Single.never()
        .takeUntil(new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
            }
        })
        .test()
        .assertFailure(CancellationException.class);
    }
}
