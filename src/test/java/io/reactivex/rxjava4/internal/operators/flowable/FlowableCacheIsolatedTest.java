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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.*;

@Isolated
public class FlowableCacheIsolatedTest extends RxJavaTest {

    @Test
    public void subscribeSubscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final Flowable<Integer> cache = Flowable.range(1, 500).cache();

            final TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<>();
            final TestSubscriberEx<Integer> ts2 = new TestSubscriberEx<>();

            Runnable r1 = () -> cache.subscribe(ts1);

            Runnable r2 = () -> cache.subscribe(ts2);

            TestHelper.race(r1, r2);

            ts1
            .awaitDone(5, TimeUnit.SECONDS)
            .assertSubscribed()
            .assertValueCount(500)
            .assertComplete()
            .assertNoErrors();

            ts2
            .awaitDone(5, TimeUnit.SECONDS)
            .assertSubscribed()
            .assertValueCount(500)
            .assertComplete()
            .assertNoErrors();
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            Flowable<Object> f = Flowable.never().cache();

            TestSubscriber<Object> ts = f.test();

            TestHelper.race(
                    ts::cancel,
                    f::test
            );
        }
    }
}
