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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.core.Single;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.subjects.ReplaySubject;
import io.reactivex.rxjava4.subjects.Subject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class SingleAmbIsolatedTest extends RxJavaTest {

    @Test
    public void nullSourceSuccessRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {

                final Subject<Integer> ps = ReplaySubject.create();
                ps.onNext(1);

                final Single<Integer> source = Single.ambArray(ps.singleOrError(), Single.<Integer>never(), Single.<Integer>never(), null);

                Runnable r1 = source::test;

                Runnable r2 = ps::onComplete;

                TestHelper.race(r1, r2);

                if (!errors.isEmpty()) {
                    TestHelper.assertError(errors, 0, NullPointerException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void multipleErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {

                final Subject<Integer> ps1 = PublishSubject.create();
                final Subject<Integer> ps2 = PublishSubject.create();

                Single.ambArray(ps1.singleOrError(), ps2.singleOrError()).test();

                final TestException ex = new TestException();

                Runnable r1 = () -> ps1.onError(ex);

                Runnable r2 = () -> ps2.onError(ex);

                TestHelper.race(r1, r2);

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void successErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {

                final Subject<Integer> ps1 = PublishSubject.create();
                final Subject<Integer> ps2 = PublishSubject.create();

                Single.ambArray(ps1.singleOrError(), ps2.singleOrError()).test();

                final TestException ex = new TestException();

                Runnable r1 = () -> {
                    ps1.onNext(1);
                    ps1.onComplete();
                };

                Runnable r2 = () -> ps2.onError(ex);

                TestHelper.race(r1, r2);

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }
}
