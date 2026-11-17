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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class SubscriptionHelperIsolatedTest extends RxJavaTest {

    @Test
    public void cancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();

            Runnable r = () -> SubscriptionHelper.cancel(atomicSubscription);

            TestHelper.race(r, r);
        }
    }

    @Test
    public void setRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();

            final BooleanSubscription bs1 = new BooleanSubscription();
            final BooleanSubscription bs2 = new BooleanSubscription();

            Runnable r1 = () -> SubscriptionHelper.set(atomicSubscription, bs1);

            Runnable r2 = () -> SubscriptionHelper.set(atomicSubscription, bs2);

            TestHelper.race(r1, r2);

            assertTrue(bs1.isCancelled() ^ bs2.isCancelled());
        }
    }

    @Test
    public void replaceRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();

            final BooleanSubscription bs1 = new BooleanSubscription();
            final BooleanSubscription bs2 = new BooleanSubscription();

            Runnable r1 = () -> SubscriptionHelper.replace(atomicSubscription, bs1);

            Runnable r2 = () -> SubscriptionHelper.replace(atomicSubscription, bs2);

            TestHelper.race(r1, r2);

            assertFalse(bs1.isCancelled());
            assertFalse(bs2.isCancelled());
        }
    }

    @Test
    public void deferredRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();
            final AtomicLong r = new AtomicLong();

            final AtomicLong q = new AtomicLong();

            final Subscription a = new Subscription() /* NFI */ {
                @Override
                public void request(long n) {
                    q.addAndGet(n);
                }

                @Override
                public void cancel() {

                }
            };

            Runnable r1 = () -> SubscriptionHelper.deferredSetOnce(atomicSubscription, r, a);

            Runnable r2 = () -> SubscriptionHelper.deferredRequest(atomicSubscription, r, 1);

            TestHelper.race(r1, r2);

            assertSame(a, atomicSubscription.get());
            assertEquals(1, q.get());
            assertEquals(0, r.get());
        }
    }
}
