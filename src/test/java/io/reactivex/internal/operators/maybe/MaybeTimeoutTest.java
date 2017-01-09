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

package io.reactivex.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class MaybeTimeoutTest {

    @Test
    public void normal() {
        Maybe.just(1)
        .timeout(1, TimeUnit.DAYS)
        .test()
        .assertResult(1);
    }

    @Test
    public void normalMaybe() {
        Maybe.just(1)
        .timeout(Maybe.timer(1, TimeUnit.DAYS))
        .test()
        .assertResult(1);
    }

    @Test
    public void never() {
        Maybe.never()
        .timeout(1, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void neverMaybe() {
        Maybe.never()
        .timeout(Maybe.timer(1, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void normalFallback() {
        Maybe.just(1)
        .timeout(1, TimeUnit.DAYS, Maybe.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void normalMaybeFallback() {
        Maybe.just(1)
        .timeout(Maybe.timer(1, TimeUnit.DAYS), Maybe.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void neverFallback() {
        Maybe.never()
        .timeout(1, TimeUnit.MILLISECONDS, Maybe.just(2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }

    @Test
    public void neverMaybeFallback() {
        Maybe.never()
        .timeout(Maybe.timer(1, TimeUnit.MILLISECONDS), Maybe.just(2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }

    @Test
    public void neverFallbackScheduler() {
        Maybe.never()
        .timeout(1, TimeUnit.MILLISECONDS, Schedulers.single(), Maybe.just(2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }

    @Test
    public void neverScheduler() {
        Maybe.never()
        .timeout(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void normalFlowableFallback() {
        Maybe.just(1)
        .timeout(Flowable.timer(1, TimeUnit.DAYS), Maybe.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void neverFlowableFallback() {
        Maybe.never()
        .timeout(Flowable.timer(1, TimeUnit.MILLISECONDS), Maybe.just(2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }

    @Test
    public void normalFlowable() {
        Maybe.just(1)
        .timeout(Flowable.timer(1, TimeUnit.DAYS))
        .test()
        .assertResult(1);
    }

    @Test
    public void neverFlowable() {
        Maybe.never()
        .timeout(Flowable.timer(1, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void mainError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2.singleElement()).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2.singleElement()).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void fallbackError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2.singleElement(), Maybe.<Integer>error(new TestException())).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(1);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void fallbackComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2.singleElement(), Maybe.<Integer>empty()).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(1);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void mainComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2.singleElement()).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void otherComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().timeout(pp2.singleElement()).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TimeoutException.class);
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestHelper.checkDisposed(pp1.singleElement().timeout(pp2.singleElement()));
    }

    @Test
    public void dispose2() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestHelper.checkDisposed(pp1.singleElement().timeout(pp2.singleElement(), Maybe.just(1)));
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestObserver<Integer> to = pp1.singleElement().timeout(pp2.singleElement()).test();

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

                TestHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void onCompleteRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestObserver<Integer> to = pp1.singleElement().timeout(pp2.singleElement()).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onComplete();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            to.assertSubscribed().assertNoValues();

            if (to.errorCount() != 0) {
                to.assertError(TimeoutException.class).assertNotComplete();
            } else {
                to.assertNoErrors().assertComplete();
            }
        }
    }
}
