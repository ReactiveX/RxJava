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

package io.reactivex.rxjava3.internal.subscribers;

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class BlockingSubscriberTest extends RxJavaTest {

    @Test
    public void doubleOnSubscribe() {
        TestHelper.doubleOnSubscribe(new BlockingSubscriber<Integer>(new ArrayDeque<>()));
    }

    @Test
    public void cancel() {
        BlockingSubscriber<Integer> bq = new BlockingSubscriber<>(new ArrayDeque<>());

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

    @Test
    public void blockingFirstTimeout() {
        BlockingFirstSubscriber<Integer> bf = new BlockingFirstSubscriber<>();

        Thread.currentThread().interrupt();

        try {
            bf.blockingGet();
            fail("Should have thrown!");
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test
    public void blockingFirstTimeout2() {
        BlockingFirstSubscriber<Integer> bf = new BlockingFirstSubscriber<>();

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

        final BlockingFirstSubscriber<Integer> bf = new BlockingFirstSubscriber<>();

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

        final BlockingFirstSubscriber<Integer> bf = new BlockingFirstSubscriber<>();

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
