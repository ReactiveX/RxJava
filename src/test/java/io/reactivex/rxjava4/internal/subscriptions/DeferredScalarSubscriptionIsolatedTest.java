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

package io.reactivex.rxjava4.internal.subscriptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class DeferredScalarSubscriptionIsolatedTest extends RxJavaTest {

    @Test
    public void completeCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<>(new TestSubscriber<>());

            Runnable r1 = () -> ds.complete(1);

            Runnable r2 = ds::cancel;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void requestClearRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

            final DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<>(ts);
            ts.onSubscribe(ds);
            ds.complete(1);

            Runnable r1 = () -> ds.request(1);

            Runnable r2 = () -> ds.value = null;

            TestHelper.race(r1, r2);

            if (!ts.values().isEmpty()) {
                ts.assertValue(1);
            }
        }
    }

    @Test
    public void requestCancelRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

            final DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<>(ts);
            ts.onSubscribe(ds);
            ds.complete(1);

            Runnable r1 = () -> ds.request(1);

            Runnable r2 = ds::cancel;

            TestHelper.race(r1, r2);

            if (!ts.values().isEmpty()) {
                ts.assertValue(1);
            }
        }
    }
}
