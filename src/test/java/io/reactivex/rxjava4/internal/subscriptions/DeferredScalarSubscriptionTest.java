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

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.operators.QueueFuseable;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class DeferredScalarSubscriptionTest extends RxJavaTest {

    @Test
    public void queueSubscriptionSyncRejected() {
        DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<>(new TestSubscriber<>());

        assertEquals(QueueFuseable.NONE, ds.requestFusion(QueueFuseable.SYNC));
    }

    @Test
    public void clear() {
        DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<>(new TestSubscriber<>());

        ds.value = 1;

        ds.clear();

        assertEquals(DeferredScalarSubscription.FUSED_CONSUMED, ds.get());
        assertNull(ds.value);
    }

    @Test
    public void cancel() {
        DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<>(new TestSubscriber<>());

        assertTrue(ds.tryCancel());

        assertFalse(ds.tryCancel());
    }

}
