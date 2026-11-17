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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.exceptions.ProtocolViolationException;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class SubscriptionHelperTest extends RxJavaTest {

    @Test
    public void checkEnum() {
        TestHelper.checkEnum(SubscriptionHelper.class);
    }

    @Test
    public void validateNullThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            SubscriptionHelper.validate(null, null);

            TestHelper.assertError(errors, 0, NullPointerException.class, "next is null");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelNoOp() {
        SubscriptionHelper.CANCELLED.cancel();
    }

    @Test
    public void set() {
        AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();

        BooleanSubscription bs1 = new BooleanSubscription();

        assertTrue(SubscriptionHelper.set(atomicSubscription, bs1));

        BooleanSubscription bs2 = new BooleanSubscription();

        assertTrue(SubscriptionHelper.set(atomicSubscription, bs2));

        assertTrue(bs1.isCancelled());

        assertFalse(bs2.isCancelled());
    }

    @Test
    public void replace() {
        AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();

        BooleanSubscription bs1 = new BooleanSubscription();

        assertTrue(SubscriptionHelper.replace(atomicSubscription, bs1));

        BooleanSubscription bs2 = new BooleanSubscription();

        assertTrue(SubscriptionHelper.replace(atomicSubscription, bs2));

        assertFalse(bs1.isCancelled());

        assertFalse(bs2.isCancelled());
    }

    @Test
    public void cancelAndChange() {
        AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();

        SubscriptionHelper.cancel(atomicSubscription);

        BooleanSubscription bs1 = new BooleanSubscription();
        assertFalse(SubscriptionHelper.set(atomicSubscription, bs1));
        assertTrue(bs1.isCancelled());

        assertFalse(SubscriptionHelper.set(atomicSubscription, null));

        BooleanSubscription bs2 = new BooleanSubscription();
        assertFalse(SubscriptionHelper.replace(atomicSubscription, bs2));
        assertTrue(bs2.isCancelled());

        assertFalse(SubscriptionHelper.replace(atomicSubscription, null));
    }

    @Test
    public void invalidDeferredRequest() {
        AtomicReference<Subscription> atomicSubscription = new AtomicReference<>();
        AtomicLong r = new AtomicLong();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            SubscriptionHelper.deferredRequest(atomicSubscription, r, -99);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class, "n > 0 required but it was -99");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void setOnceAndRequest() {
        AtomicReference<Subscription> ref = new AtomicReference<>();

        Subscription sub = mock(Subscription.class);

        assertTrue(SubscriptionHelper.setOnce(ref, sub, 1));

        verify(sub).request(1);
        verify(sub, never()).cancel();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            sub = mock(Subscription.class);

            assertFalse(SubscriptionHelper.setOnce(ref, sub, 1));

            verify(sub, never()).request(anyLong());
            verify(sub).cancel();

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
