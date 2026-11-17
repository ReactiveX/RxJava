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
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class ArrayCompositeSubscriptionIsolatedTest extends RxJavaTest {

    @Test
    public void disposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ArrayCompositeSubscription ac = new ArrayCompositeSubscription(1000);

            Runnable r = ac::dispose;

            TestHelper.race(r, r);
        }
    }

    @Test
    public void setReplaceRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ArrayCompositeSubscription ac = new ArrayCompositeSubscription(1);

            final BooleanSubscription s1 = new BooleanSubscription();
            final BooleanSubscription s2 = new BooleanSubscription();

            Runnable r1 = () -> ac.setResource(0, s1);

            Runnable r2 = () -> ac.replaceResource(0, s2);

            TestHelper.race(r1, r2);
        }
    }
}
