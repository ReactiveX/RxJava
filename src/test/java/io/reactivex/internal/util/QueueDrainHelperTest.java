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

package io.reactivex.internal.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.TestHelper;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.subscribers.TestSubscriber;

public class QueueDrainHelperTest {

    @Test
    public void isCancelled() {
        assertTrue(QueueDrainHelper.isCancelled(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                throw new IOException();
            }
        }));
    }

    @Test
    public void requestMaxInt() {
        QueueDrainHelper.request(new Subscription() {
            @Override
            public void request(long n) {
                assertEquals(Integer.MAX_VALUE, n);
            }

            @Override
            public void cancel() {
            }
        }, Integer.MAX_VALUE);
    }

    @Test
    public void requestMinInt() {
        QueueDrainHelper.request(new Subscription() {
            @Override
            public void request(long n) {
                assertEquals(Long.MAX_VALUE, n);
            }

            @Override
            public void cancel() {
            }
        }, Integer.MIN_VALUE);
    }

    @Test
    public void requestAlmostMaxInt() {
        QueueDrainHelper.request(new Subscription() {
            @Override
            public void request(long n) {
                assertEquals(Integer.MAX_VALUE - 1, n);
            }

            @Override
            public void cancel() {
            }
        }, Integer.MAX_VALUE - 1);
    }

    @Test
    public void postCompleteEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        AtomicLong state = new AtomicLong();
        BooleanSupplier isCancelled = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        };

        ts.onSubscribe(new BooleanSubscription());

        QueueDrainHelper.postComplete(ts, queue, state, isCancelled);

        ts.assertResult();
    }

    @Test
    public void postCompleteWithRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        AtomicLong state = new AtomicLong();
        BooleanSupplier isCancelled = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        };

        ts.onSubscribe(new BooleanSubscription());
        queue.offer(1);
        state.getAndIncrement();

        QueueDrainHelper.postComplete(ts, queue, state, isCancelled);

        ts.assertResult(1);
    }

    @Test
    public void completeRequestRace() {
        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            final ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
            final AtomicLong state = new AtomicLong();
            final BooleanSupplier isCancelled = new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() throws Exception {
                    return false;
                }
            };

            ts.onSubscribe(new BooleanSubscription());
            queue.offer(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    QueueDrainHelper.postCompleteRequest(1, ts, queue, state, isCancelled);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    QueueDrainHelper.postComplete(ts, queue, state, isCancelled);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }

    @Test
    public void postCompleteCancelled() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        AtomicLong state = new AtomicLong();
        BooleanSupplier isCancelled = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return ts.isCancelled();
            }
        };

        ts.onSubscribe(new BooleanSubscription());
        queue.offer(1);
        state.getAndIncrement();
        ts.cancel();

        QueueDrainHelper.postComplete(ts, queue, state, isCancelled);

        ts.assertEmpty();
    }

    @Test
    public void postCompleteCancelledAfterOne() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
            }
        };
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        AtomicLong state = new AtomicLong();
        BooleanSupplier isCancelled = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return ts.isCancelled();
            }
        };

        ts.onSubscribe(new BooleanSubscription());
        queue.offer(1);
        state.getAndIncrement();

        QueueDrainHelper.postComplete(ts, queue, state, isCancelled);

        ts.assertValue(1).assertNoErrors().assertNotComplete();
    }
}
