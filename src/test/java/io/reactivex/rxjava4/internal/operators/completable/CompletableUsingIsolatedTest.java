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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.testsupport.TestObserverEx;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class CompletableUsingIsolatedTest extends RxJavaTest {

    @Test
    public void successDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserverEx<Void> to = Completable.using(() -> 1, _ -> ps.ignoreElements(),
                    _ -> { }, true)
            .to(TestHelper.<Void>testConsumer());

            ps.onNext(1);

            Runnable r1 = to::dispose;

            Runnable r2 = ps::onComplete;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void errorDisposeRace() {
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());
        try {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

                final PublishSubject<Integer> ps = PublishSubject.create();

                final TestObserver<Void> to = Completable.using(() -> 1, _ -> ps.ignoreElements(),
                        _ -> { }, true)
                .test();

                final TestException ex = new TestException();

                Runnable r1 = to::dispose;

                Runnable r2 = () -> ps.onError(ex);

                TestHelper.race(r1, r2);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void emptyDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Void> to = Completable.using(() -> 1, _ -> ps.ignoreElements(),
                    _ -> { }, true)
            .test();

            Runnable r1 = to::dispose;

            Runnable r2 = ps::onComplete;

            TestHelper.race(r1, r2);
        }
    }
}
