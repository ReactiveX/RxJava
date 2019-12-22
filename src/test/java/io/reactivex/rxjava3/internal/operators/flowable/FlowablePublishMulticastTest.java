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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.internal.fuseable.QueueSubscription;
import io.reactivex.rxjava3.internal.operators.flowable.FlowablePublishMulticast.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.UnicastProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowablePublishMulticastTest extends RxJavaTest {

    @Test
    public void asyncFusedInput() {
        MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up.subscribe(mp);

        TestSubscriber<Integer> ts1 = mp.test();
        TestSubscriber<Integer> ts2 = mp.take(1).test();

        up.onNext(1);
        up.onNext(2);
        up.onComplete();

        ts1.assertResult(1, 2);
        ts2.assertResult(1);
    }

    @Test
    public void fusionRejectedInput() {
        MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

        mp.onSubscribe(new QueueSubscription<Integer>() {

            @Override
            public int requestFusion(int mode) {
                return 0;
            }

            @Override
            public boolean offer(Integer value) {
                return false;
            }

            @Override
            public boolean offer(Integer v1, Integer v2) {
                return false;
            }

            @Override
            public Integer poll() throws Exception {
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public void clear() {
            }

            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });

        TestSubscriber<Integer> ts = mp.test();

        mp.onNext(1);
        mp.onNext(2);
        mp.onComplete();

        ts.assertResult(1, 2);
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

            final MulticastSubscription<Integer> ms1 = new MulticastSubscription<>(null, mp);
            final MulticastSubscription<Integer> ms2 = new MulticastSubscription<>(null, mp);

            assertTrue(mp.add(ms1));

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    mp.remove(ms1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    mp.add(ms2);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void removeNotFound() {
        MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

        MulticastSubscription<Integer> ms1 = new MulticastSubscription<>(null, mp);
        assertTrue(mp.add(ms1));

        mp.remove(null);
    }

    @Test
    public void errorAllCancelled() {
        MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

        MulticastSubscription<Integer> ms1 = new MulticastSubscription<>(null, mp);
        assertTrue(mp.add(ms1));

        ms1.set(Long.MIN_VALUE);

        mp.errorAll(null);
    }

    @Test
    public void completeAllCancelled() {
        MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

        MulticastSubscription<Integer> ms1 = new MulticastSubscription<>(null, mp);
        assertTrue(mp.add(ms1));

        ms1.set(Long.MIN_VALUE);

        mp.completeAll();
    }

    @Test
    public void cancelledWhileFindingRequests() {
        final MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

        final MulticastSubscription<Integer> ms1 = new MulticastSubscription<>(null, mp);

        assertTrue(mp.add(ms1));

        mp.onSubscribe(new BooleanSubscription());

        ms1.set(Long.MIN_VALUE);

        mp.drain();
    }

    @Test
    public void negativeRequest() {
        final MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

        final MulticastSubscription<Integer> ms1 = new MulticastSubscription<>(null, mp);

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ms1.request(-1);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void outputCancellerDoubleOnSubscribe() {
        TestHelper.doubleOnSubscribe(new OutputCanceller<>(new TestSubscriber<>(), null));
    }

    @Test
    public void dontDropItemsWhenNoReadyConsumers() {
        final MulticastProcessor<Integer> mp = new MulticastProcessor<>(128, true);

        mp.onSubscribe(new BooleanSubscription());

        mp.onNext(1);

        TestSubscriber<Integer> ts = new TestSubscriber<>();
        final MulticastSubscription<Integer> ms1 = new MulticastSubscription<>(ts, mp);
        ts.onSubscribe(ms1);

        assertTrue(mp.add(ms1));

        ms1.set(Long.MIN_VALUE);

        mp.drain();

        assertFalse(mp.queue.isEmpty());
    }
}
