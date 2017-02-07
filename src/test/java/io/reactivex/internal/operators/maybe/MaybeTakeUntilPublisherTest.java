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

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class MaybeTakeUntilPublisherTest {

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishProcessor.create().singleElement().takeUntil(Flowable.never()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Maybe<Object> m) throws Exception {
                return m.takeUntil(Flowable.never());
            }
        });
    }

    @Test
    public void mainErrors() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void otherErrors() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainCompletes() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void otherCompletes() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2).test();

            final TestException ex1 = new TestException();
            final TestException ex2 = new TestException();

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp2.onError(ex2);
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
    public void onCompleteRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2).test();

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

            to.assertResult();
        }
    }

    @Test
    public void otherSignalsAndCompletes() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Maybe.just(1).takeUntil(Flowable.just(1).take(1))
            .test()
            .assertResult();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
