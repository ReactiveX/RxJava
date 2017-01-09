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

import java.util.List;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.TestHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class SubscriptionHelperTest {

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
        AtomicReference<Subscription> s = new AtomicReference<Subscription>();

        BooleanSubscription bs1 = new BooleanSubscription();

        assertTrue(SubscriptionHelper.set(s, bs1));

        BooleanSubscription bs2 = new BooleanSubscription();

        assertTrue(SubscriptionHelper.set(s, bs2));

        assertTrue(bs1.isCancelled());

        assertFalse(bs2.isCancelled());
    }

    @Test
    public void replace() {
        AtomicReference<Subscription> s = new AtomicReference<Subscription>();

        BooleanSubscription bs1 = new BooleanSubscription();

        assertTrue(SubscriptionHelper.replace(s, bs1));

        BooleanSubscription bs2 = new BooleanSubscription();

        assertTrue(SubscriptionHelper.replace(s, bs2));

        assertFalse(bs1.isCancelled());

        assertFalse(bs2.isCancelled());
    }

    @Test
    public void cancelRace() {
        for (int i = 0; i < 500; i++) {
            final AtomicReference<Subscription> s = new AtomicReference<Subscription>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    SubscriptionHelper.cancel(s);
                }
            };

            TestHelper.race(r, r, Schedulers.single());
        }
    }

    @Test
    public void setRace() {
        for (int i = 0; i < 500; i++) {
            final AtomicReference<Subscription> s = new AtomicReference<Subscription>();

            final BooleanSubscription bs1 = new BooleanSubscription();
            final BooleanSubscription bs2 = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    SubscriptionHelper.set(s, bs1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    SubscriptionHelper.set(s, bs2);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            assertTrue(bs1.isCancelled() ^ bs2.isCancelled());
        }
    }

    @Test
    public void replaceRace() {
        for (int i = 0; i < 500; i++) {
            final AtomicReference<Subscription> s = new AtomicReference<Subscription>();

            final BooleanSubscription bs1 = new BooleanSubscription();
            final BooleanSubscription bs2 = new BooleanSubscription();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    SubscriptionHelper.replace(s, bs1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    SubscriptionHelper.replace(s, bs2);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            assertFalse(bs1.isCancelled());
            assertFalse(bs2.isCancelled());
        }
    }

    @Test
    public void cancelAndChange() {
        AtomicReference<Subscription> s = new AtomicReference<Subscription>();

        SubscriptionHelper.cancel(s);

        BooleanSubscription bs1 = new BooleanSubscription();
        assertFalse(SubscriptionHelper.set(s, bs1));
        assertTrue(bs1.isCancelled());

        assertFalse(SubscriptionHelper.set(s, null));

        BooleanSubscription bs2 = new BooleanSubscription();
        assertFalse(SubscriptionHelper.replace(s, bs2));
        assertTrue(bs2.isCancelled());

        assertFalse(SubscriptionHelper.replace(s, null));
    }

    @Test
    public void invalidDeferredRequest() {
        AtomicReference<Subscription> s = new AtomicReference<Subscription>();
        AtomicLong r = new AtomicLong();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            SubscriptionHelper.deferredRequest(s, r, -99);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class, "n > 0 required but it was -99");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void deferredRace() {
        for (int i = 0; i < 500; i++) {
            final AtomicReference<Subscription> s = new AtomicReference<Subscription>();
            final AtomicLong r = new AtomicLong();

            final AtomicLong q = new AtomicLong();

            final Subscription a = new Subscription() {
                @Override
                public void request(long n) {
                    q.addAndGet(n);
                }

                @Override
                public void cancel() {

                }
            };

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    SubscriptionHelper.deferredSetOnce(s, r, a);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    SubscriptionHelper.deferredRequest(s, r, 1);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            assertSame(a, s.get());
            assertEquals(1, q.get());
            assertEquals(0, r.get());
        }
    }
}
