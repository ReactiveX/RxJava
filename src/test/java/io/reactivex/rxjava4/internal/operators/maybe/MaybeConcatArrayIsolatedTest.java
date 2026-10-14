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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StandardBufferedConfig;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class MaybeConcatArrayIsolatedTest extends RxJavaTest {

    @Test
    public void requestCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = Maybe.concatArray(Maybe.just(1), Maybe.just(2))
                    .test(0L);

            Runnable r1 = ts::cancel;

            Runnable r2 = () -> ts.request(1);

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void requestCancelRaceDelayError() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = Maybe.concatArray(StandardBufferedConfig.DELAY_ERRORS, Maybe.just(1), Maybe.just(2))
                    .test(0L);

            Runnable r1 = ts::cancel;

            Runnable r2 = () -> ts.request(1);

            TestHelper.race(r1, r2);
        }
    }
}
