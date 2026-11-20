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
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.operators.QueueFuseable;
import io.reactivex.rxjava4.schedulers.TestScheduler;
import io.reactivex.rxjava4.testsupport.*;

@Isolated
public class FlowableObserveOnIsolatedTest extends RxJavaTest {

    @Test
    public void backFusedCancelConditional() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

            final TestScheduler scheduler = new TestScheduler();

            Flowable.just(1).hide()
            .observeOn(scheduler)
            .filter(Functions.alwaysTrue())
            .subscribe(ts);

            Runnable r1 = ts::cancel;

            Runnable r2 = scheduler::triggerActions;

            TestHelper.race(r1, r2);

            ts.assertFusionMode(QueueFuseable.ASYNC);

            if (!ts.values().isEmpty()) {
                ts.assertResult(1);
            }
        }
    }
}
