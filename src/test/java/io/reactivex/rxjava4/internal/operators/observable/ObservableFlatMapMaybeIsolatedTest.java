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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.subjects.MaybeSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class ObservableFlatMapMaybeIsolatedTest extends RxJavaTest {

    @Test
    public void successCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            MaybeSubject<Integer> ms1 = MaybeSubject.create();
            MaybeSubject<Integer> ms2 = MaybeSubject.create();

            TestObserver<Integer> to = Observable.just(1, 2)
            .flatMapMaybe(v -> v == 1 ? ms1 : ms2)
            .test();

            TestHelper.race(
                    ms1::onComplete,
                    () -> ms2.onSuccess(1)
            );

            to.assertResult(1);
        }
    }

    @Test
    public void successCompleteRace2() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            MaybeSubject<Integer> ms1 = MaybeSubject.create();
            MaybeSubject<Integer> ms2 = MaybeSubject.create();

            TestObserver<Integer> to = Observable.just(1, 2)
            .flatMapMaybe(v -> v == 1 ? ms1 : ms2)
            .test();

            TestHelper.race(
                    () -> ms2.onSuccess(1),
                    ms1::onComplete
            );

            to.assertResult(1);
        }
    }
}
