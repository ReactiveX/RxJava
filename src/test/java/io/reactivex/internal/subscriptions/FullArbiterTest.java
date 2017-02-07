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

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;

public class FullArbiterTest {

    @Test
    public void initialRequested() {
        FullArbiter.INITIAL.request(-99);
    }

    @Test
    public void initialCancel() {
        FullArbiter.INITIAL.cancel();
    }

    @Test
    public void invalidDeferredRequest() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new FullArbiter<Integer>(new TestSubscriber<Integer>(), null, 128).request(-99);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class, "n > 0 required but it was -99");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void setSubscriptionAfterCancel() {
        FullArbiter<Integer> fa = new FullArbiter<Integer>(new TestSubscriber<Integer>(), null, 128);

        fa.cancel();

        BooleanSubscription bs = new BooleanSubscription();

        assertFalse(fa.setSubscription(bs));

        assertFalse(fa.setSubscription(null));
    }

    @Test
    public void cancelAfterPoll() {
        FullArbiter<Integer> fa = new FullArbiter<Integer>(new TestSubscriber<Integer>(), null, 128);

        BooleanSubscription bs = new BooleanSubscription();

        fa.queue.offer(fa.s, NotificationLite.subscription(bs));

        fa.cancel();

        fa.drain();

        assertTrue(bs.isCancelled());
    }

    @Test
    public void errorAfterCancel() {
        FullArbiter<Integer> fa = new FullArbiter<Integer>(new TestSubscriber<Integer>(), null, 128);

        BooleanSubscription bs = new BooleanSubscription();

        fa.cancel();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            fa.onError(new TestException(), bs);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelAfterError() {
        FullArbiter<Integer> fa = new FullArbiter<Integer>(new TestSubscriber<Integer>(), null, 128);

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            fa.queue.offer(fa.s, NotificationLite.error(new TestException()));

            fa.cancel();

            fa.drain();
            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void offerDifferentSubscription() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        FullArbiter<Integer> fa = new FullArbiter<Integer>(ts, null, 128);

        BooleanSubscription bs = new BooleanSubscription();

        fa.queue.offer(bs, NotificationLite.next(1));

        fa.drain();

        ts.assertNoValues();
    }
}
