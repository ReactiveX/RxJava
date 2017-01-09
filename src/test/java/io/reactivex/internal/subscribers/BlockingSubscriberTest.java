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

package io.reactivex.internal.subscribers;

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.TestHelper;
import io.reactivex.internal.subscriptions.BooleanSubscription;

public class BlockingSubscriberTest {

    @Test
    public void doubleOnSubscribe() {
        TestHelper.doubleOnSubscribe(new BlockingSubscriber<Integer>(new ArrayDeque<Object>()));
    }

    @Test
    public void cancel() {
        BlockingSubscriber<Integer> bq = new BlockingSubscriber<Integer>(new ArrayDeque<Object>());

        assertFalse(bq.isCancelled());

        bq.cancel();

        assertTrue(bq.isCancelled());

        bq.cancel();

        assertTrue(bq.isCancelled());
    }

    @Test
    public void blockingFirstDoubleOnSubscribe() {
        TestHelper.doubleOnSubscribe(new BlockingFirstSubscriber<Integer>());
    }

    @Test(timeout = 5000)
    public void blockingFirstTimeout() {
        BlockingFirstSubscriber<Integer> bf = new BlockingFirstSubscriber<Integer>();

        Thread.currentThread().interrupt();

        try {
            bf.blockingGet();
            fail("Should have thrown!");
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test(timeout = 5000)
    public void blockingFirstTimeout2() {
        BlockingFirstSubscriber<Integer> bf = new BlockingFirstSubscriber<Integer>();

        bf.onSubscribe(new BooleanSubscription());

        Thread.currentThread().interrupt();

        try {
            bf.blockingGet();
            fail("Should have thrown!");
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test
    public void cancelOnRequest() {

        final BlockingFirstSubscriber<Integer> bf = new BlockingFirstSubscriber<Integer>();

        final AtomicBoolean b = new AtomicBoolean();

        Subscription s = new Subscription() {
            @Override
            public void request(long n) {
                bf.cancelled = true;
            }
            @Override
            public void cancel() {
                b.set(true);
            }
        };

        bf.onSubscribe(s);

        assertTrue(b.get());
    }

    @Test
    public void cancelUpfront() {

        final BlockingFirstSubscriber<Integer> bf = new BlockingFirstSubscriber<Integer>();

        final AtomicBoolean b = new AtomicBoolean();

        bf.cancelled = true;

        Subscription s = new Subscription() {
            @Override
            public void request(long n) {
                b.set(true);
            }
            @Override
            public void cancel() {
            }
        };

        bf.onSubscribe(s);

        assertFalse(b.get());
    }
}
