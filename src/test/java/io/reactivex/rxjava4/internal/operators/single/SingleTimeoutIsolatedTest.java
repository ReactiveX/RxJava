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

package io.reactivex.rxjava4.internal.operators.single;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.TestScheduler;
import io.reactivex.rxjava4.subjects.SingleSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class SingleTimeoutIsolatedTest extends RxJavaTest {

    @Test
    public void successTimeoutRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final SingleSubject<Integer> subj = SingleSubject.create();
            SingleSubject<Integer> fallback = SingleSubject.create();

            final TestScheduler sch = new TestScheduler();

            TestObserver<Integer> to = subj.timeout(1, TimeUnit.MILLISECONDS, sch, fallback).test();

            Runnable r1 = () -> subj.onSuccess(1);

            Runnable r2 = () -> sch.advanceTimeBy(1, TimeUnit.MILLISECONDS);

            TestHelper.race(r1, r2);

            if (!fallback.hasObservers()) {
                to.assertResult(1);
            } else {
                to.assertEmpty();
            }
        }
    }

    @Test
    public void errorTimeoutRace() {
        final TestException ex = new TestException();
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                final SingleSubject<Integer> subj = SingleSubject.create();
                SingleSubject<Integer> fallback = SingleSubject.create();

                final TestScheduler sch = new TestScheduler();

                TestObserver<Integer> to = subj.timeout(1, TimeUnit.MILLISECONDS, sch, fallback).test();

                Runnable r1 = () -> subj.onError(ex);

                Runnable r2 = () -> sch.advanceTimeBy(1, TimeUnit.MILLISECONDS);

                TestHelper.race(r1, r2);

                if (!fallback.hasObservers()) {
                    to.assertFailure(TestException.class);
                } else {
                    to.assertEmpty();
                }
                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
