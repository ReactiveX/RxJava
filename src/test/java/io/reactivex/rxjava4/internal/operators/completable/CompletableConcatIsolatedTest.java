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
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class CompletableConcatIsolatedTest extends RxJavaTest {

    @Test
    public void errorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestObserver<Void> to = Completable.concat(pp1.map(_ -> pp2.ignoreElements())).test();

                pp1.onNext(1);

                final TestException ex = new TestException();

                Runnable r1 = () -> pp1.onError(ex);
                Runnable r2 = () -> pp2.onError(ex);

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
    public void arrayCancelRace() {
        Completable[] a = new Completable[1024];
        Arrays.fill(a, Completable.complete());

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final Completable c = Completable.concatArray(a);

            final TestObserver<Void> to = new TestObserver<>();

            Runnable r1 = () -> c.subscribe(to);

            Runnable r2 = to::dispose;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void iterableCancelRace() {
        Completable[] a = new Completable[1024];
        Arrays.fill(a, Completable.complete());

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final Completable c = Completable.concat(Arrays.asList(a));

            final TestObserver<Void> to = new TestObserver<>();

            Runnable r1 = () -> c.subscribe(to);

            Runnable r2 = to::dispose;

            TestHelper.race(r1, r2);
        }
    }
}
