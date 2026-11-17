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

package io.reactivex.rxjava4.internal.observers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class FutureObserverIsolatedTest extends RxJavaTest {

    @Test
    public void cancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final FutureObserver<Integer> fo = new FutureObserver<>();

            Runnable r = () -> fo.cancel(false);

            TestHelper.race(r, r);
        }
    }

    @Test
    public void onErrorCancelRace() {
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());
        try {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                final FutureObserver<Integer> fo = new FutureObserver<>();

                final TestException ex = new TestException();

                Runnable r1 = () -> fo.cancel(false);

                Runnable r2 = () -> fo.onError(ex);

                TestHelper.race(r1, r2);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCancelRace() {
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());
        try {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                final FutureObserver<Integer> fo = new FutureObserver<>();

                if (i % 3 == 0) {
                    fo.onSubscribe(Disposable.empty());
                }

                if (i % 2 == 0) {
                    fo.onNext(1);
                }

                Runnable r1 = () -> fo.cancel(false);

                Runnable r2 = fo::onComplete;

                TestHelper.race(r1, r2);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelOnSubscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final FutureObserver<Integer> fo = new FutureObserver<>();

            Runnable r = () -> fo.cancel(false);

            Disposable d = Disposable.empty();

            TestHelper.race(r, () -> fo.onSubscribe(d));
        }
    }
}
