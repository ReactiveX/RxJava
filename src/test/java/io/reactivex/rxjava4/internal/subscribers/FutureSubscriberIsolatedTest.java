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

package io.reactivex.rxjava4.internal.subscribers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava4.testsupport.SuppressUndeliverable;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class FutureSubscriberIsolatedTest extends RxJavaTest {

    @Test
    public void cancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final FutureSubscriber<Integer> fs = new FutureSubscriber<>();

            Runnable r = () -> fs.cancel(false);

            TestHelper.race(r, r);
        }
    }

    @Test
    @SuppressUndeliverable
    public void onErrorCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final FutureSubscriber<Integer> fs = new FutureSubscriber<>();

            final TestException ex = new TestException();

            Runnable r1 = () -> fs.cancel(false);

            Runnable r2 = () -> fs.onError(ex);

            TestHelper.race(r1, r2);
        }
    }

    @Test
    @SuppressUndeliverable
    public void onCompleteCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final FutureSubscriber<Integer> fs = new FutureSubscriber<>();

            if (i % 3 == 0) {
                fs.onSubscribe(new BooleanSubscription());
            }

            if (i % 2 == 0) {
                fs.onNext(1);
            }

            Runnable r1 = () -> fs.cancel(false);

            Runnable r2 = fs::onComplete;

            TestHelper.race(r1, r2);
        }
    }
}
