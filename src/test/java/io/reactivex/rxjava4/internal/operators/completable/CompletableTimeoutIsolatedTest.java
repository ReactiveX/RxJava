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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.testsupport.TestObserverEx;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.TestScheduler;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class CompletableTimeoutIsolatedTest extends RxJavaTest {

    @Test
    public void errorTimeoutRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                final TestScheduler scheduler = new TestScheduler();

                final PublishSubject<Integer> ps = PublishSubject.create();

                TestObserverEx<Void> to = ps.ignoreElements()
                        .timeout(1, TimeUnit.MILLISECONDS, scheduler, Completable.complete())
                        .to(TestHelper.<Void>testConsumer());

                final TestException ex = new TestException();

                Runnable r1 = () -> ps.onError(ex);

                Runnable r2 = () -> scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

                TestHelper.race(r1, r2);

                to.assertTerminated();

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }

            } finally {
                RxJavaPlugins.reset();
            }
        }
    }
}
