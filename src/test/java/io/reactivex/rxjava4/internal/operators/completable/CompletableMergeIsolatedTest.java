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

package io.reactivex.rxjava4.internal.operators.completable;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StandardConcurrentConfig;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.testsupport.*;

@Isolated
public class CompletableMergeIsolatedTest extends RxJavaTest {

    @Test
    public void mainErrorInnerErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestObserverEx<Void> to = Completable.merge(pp1.map(_ -> pp2.ignoreElements()))
                        .to(TestHelper.<Void>testConsumer());

                pp1.onNext(1);

                final Throwable ex1 = new TestException();
                final Throwable ex2 = new TestException();

                Runnable r1 = () -> pp1.onError(ex1);
                Runnable r2 = () -> pp2.onError(ex2);

                TestHelper.race(r1, r2);

                Throwable ex = to.errors().getFirst();
                if (ex instanceof CompositeException) {
                    to.assertSubscribed().assertNoValues().assertNotComplete();

                    errors = TestHelper.compositeList(ex);
                    TestHelper.assertError(errors, 0, TestException.class);
                    TestHelper.assertError(errors, 1, TestException.class);
                } else {
                    to.assertFailure(TestException.class);

                    if (!errors.isEmpty()) {
                        TestHelper.assertUndeliverable(errors, 0, TestException.class);
                    }
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void mainErrorInnerErrorDelayedRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestObserverEx<Void> to = Completable.merge(pp1.map(_ -> pp2.ignoreElements()), StandardConcurrentConfig.MAX_DELAY_ERRORS)
                    .to(TestHelper.<Void>testConsumer());

            pp1.onNext(1);

            final Throwable ex1 = new TestException();

            Runnable r1 = () -> pp1.onError(ex1);

            final Throwable ex2 = new TestException();
            Runnable r2 = () -> pp2.onError(ex2);

            TestHelper.race(r1, r2);

            to.assertFailure(CompositeException.class);

            List<Throwable> errors = TestHelper.compositeList(to.errors().getFirst());

            TestHelper.assertError(errors, 0, TestException.class);
            TestHelper.assertError(errors, 1, TestException.class);
        }
    }

    @Test
    public void mergeArrayInnerErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestObserver<Void> to = Completable.mergeArray(pp1.ignoreElements(), pp2.ignoreElements()).test();

                pp1.onNext(1);

                final Throwable ex1 = new TestException();
                final Throwable ex2 = new TestException();

                Runnable r1 = () -> pp1.onError(ex1);
                Runnable r2 = () -> pp2.onError(ex2);

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
}
