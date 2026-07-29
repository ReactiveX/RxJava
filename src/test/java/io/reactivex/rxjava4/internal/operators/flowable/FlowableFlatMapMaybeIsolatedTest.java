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

package io.reactivex.rxjava4.internal.operators.flowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.core.Maybe;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.subjects.MaybeSubject;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class FlowableFlatMapMaybeIsolatedTest extends RxJavaTest {

    @Test
    public void requestCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = Flowable.just(1).concatWith(Flowable.<Integer>never())
            .flatMapMaybe(Functions.justFunction(Maybe.just(2))).test(0);

            Runnable r1 = () -> ts.request(1);
            Runnable r2 = ts::cancel;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void successRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            MaybeSubject<Integer> ms1 = MaybeSubject.create();
            MaybeSubject<Integer> ms2 = MaybeSubject.create();

            TestSubscriber<Integer> ts = Flowable.just(ms1, ms2).flatMapMaybe(v -> v)
            .test();

            TestHelper.race(
                    () -> ms1.onSuccess(1),
                    () -> ms2.onSuccess(1)
            );

            ts.assertResult(1, 1);
        }
    }

    @Test
    public void successCompleteRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            MaybeSubject<Integer> ms1 = MaybeSubject.create();
            MaybeSubject<Integer> ms2 = MaybeSubject.create();

            TestSubscriber<Integer> ts = Flowable.just(ms1, ms2).flatMapMaybe(v -> v)
            .test();

            TestHelper.race(
                    () -> ms1.onSuccess(1),
                    ms2::onComplete
            );

            ts.assertResult(1);
        }
    }
}
