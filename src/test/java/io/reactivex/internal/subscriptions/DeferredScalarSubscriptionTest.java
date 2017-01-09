/**
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

package io.reactivex.internal.subscriptions;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class DeferredScalarSubscriptionTest {

    @Test
    public void queueSubscriptionSyncRejected() {
        DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<Integer>(new TestSubscriber<Integer>());

        assertEquals(QueueSubscription.NONE, ds.requestFusion(QueueSubscription.SYNC));
    }

    @Test
    public void clear() {
        DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<Integer>(new TestSubscriber<Integer>());

        ds.value = 1;

        ds.clear();

        assertEquals(DeferredScalarSubscription.FUSED_CONSUMED, ds.get());
        assertNull(ds.value);
    }

    @Test
    public void cancel() {
        DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<Integer>(new TestSubscriber<Integer>());

        assertTrue(ds.tryCancel());

        assertFalse(ds.tryCancel());
    }

    @Test
    public void completeCancelRace() {
        for (int i = 0; i < 500; i++) {
            final DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<Integer>(new TestSubscriber<Integer>());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ds.complete(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ds.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void requestClearRace() {
        for (int i = 0; i < 5000; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

            final DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<Integer>(ts);
            ts.onSubscribe(ds);
            ds.complete(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ds.request(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ds.value = null;
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            if (ts.valueCount() >= 1) {
                ts.assertValue(1);
            }
        }
    }

    @Test
    public void requestCancelRace() {
        for (int i = 0; i < 5000; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

            final DeferredScalarSubscription<Integer> ds = new DeferredScalarSubscription<Integer>(ts);
            ts.onSubscribe(ds);
            ds.complete(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ds.request(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ds.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            if (ts.valueCount() >= 1) {
                ts.assertValue(1);
            }
        }
    }
}
