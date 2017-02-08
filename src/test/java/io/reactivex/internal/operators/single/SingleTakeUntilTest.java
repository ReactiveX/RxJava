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

import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.*;
import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class SingleTakeUntilTest {

    @Test
    public void mainSuccessPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp)
        .test();

        source.onNext(1);
        source.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void mainSuccessSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.single(-99))
        .test();

        source.onNext(1);
        source.onComplete();

        ts.assertResult(1);
    }


    @Test
    public void mainSuccessCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        source.onNext(1);
        source.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void mainErrorPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp)
        .test();

        source.onError(new TestException());

        ts.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.single(-99))
        .test();

        source.onError(new TestException());

        ts.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        source.onError(new TestException());

        ts.assertFailure(TestException.class);
    }

    @Test
    public void otherOnNextPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp)
        .test();

        pp.onNext(1);

        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnNextSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.single(-99))
        .test();

        pp.onNext(1);
        pp.onComplete();

        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnNextCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        pp.onNext(1);
        pp.onComplete();

        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnCompletePublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp)
        .test();

        pp.onComplete();

        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnCompleteCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        pp.onComplete();

        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherErrorPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp)
        .test();

        pp.onError(new TestException());

        ts.assertFailure(TestException.class);
    }

    @Test
    public void otherErrorSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.single(-99))
        .test();

        pp.onError(new TestException());

        ts.assertFailure(TestException.class);
    }

    @Test
    public void otherErrorCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();

        TestObserver<Integer> ts = source.single(-99).takeUntil(pp.ignoreElements())
        .test();

        pp.onError(new TestException());

        ts.assertFailure(TestException.class);
    }

    @Test
    public void withPublisherDispose() {
        TestHelper.checkDisposed(Single.never().takeUntil(Flowable.never()));
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                final PublishProcessor<Integer> ps1 = PublishProcessor.create();
                final PublishProcessor<Integer> ps2 = PublishProcessor.create();

                TestObserver<Integer> to = ps1.singleOrError().takeUntil(ps2).test();

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

                TestHelper.race(r1, r2, Schedulers.single());

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
}
