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

package io.reactivex.rxjava4.internal.operators.observable;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.*;

@Isolated
public class ObservableAmbIsolatedTest extends RxJavaTest {

    @Test
    public void onNextRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();
            final PublishSubject<Integer> ps2 = PublishSubject.create();

            TestObserverEx<Integer> to = Observable.ambArray(ps1, ps2).to(TestHelper.<Integer>testConsumer());

            Runnable r1 = () -> ps1.onNext(1);
            Runnable r2 = () -> ps2.onNext(1);

            TestHelper.race(r1, r2);

            to.assertSubscribed()
            .assertNoErrors()
            .assertNotComplete()
            .assertValueCount(1);
        }
    }

    @Test
    public void onCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();
            final PublishSubject<Integer> ps2 = PublishSubject.create();

            TestObserver<Integer> to = Observable.ambArray(ps1, ps2).test();

            Runnable r1 = ps1::onComplete;
            Runnable r2 = ps2::onComplete;

            TestHelper.race(r1, r2);

            to.assertResult();
        }
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();
            final PublishSubject<Integer> ps2 = PublishSubject.create();

            TestObserver<Integer> to = Observable.ambArray(ps1, ps2).test();

            final Throwable ex = new TestException();

            Runnable r1 = () -> ps1.onError(ex);
            Runnable r2 = () -> ps2.onError(ex);

            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestHelper.race(r1, r2);
            } finally {
                RxJavaPlugins.reset();
            }

            to.assertFailure(TestException.class);
            if (!errors.isEmpty()) {
                TestHelper.assertUndeliverable(errors, 0, TestException.class);
            }
        }
    }
}
