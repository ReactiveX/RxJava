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

package io.reactivex.rxjava4.internal.operators.maybe;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class MaybeTakeUntilPublisherIsolatedTest extends RxJavaTest {

    @Test
    public void onErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2).test();

            final TestException ex1 = new TestException();
            final TestException ex2 = new TestException();

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {

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

    @Test
    public void onCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestObserver<Integer> to = pp1.singleElement().takeUntil(pp2).test();

            Runnable r1 = pp1::onComplete;
            Runnable r2 = pp2::onComplete;

            TestHelper.race(r1, r2);

            to.assertResult();
        }
    }
}
