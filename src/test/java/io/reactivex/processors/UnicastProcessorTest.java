/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.processors;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.subscribers.*;

public class UnicastProcessorTest {

    @Test
    public void fusionLive() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create();

        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC));

        ts.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onNext(1);

        ts.assertValue(1).assertNoErrors().assertNotComplete();

        ap.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void fusionOfflie() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create();
        ap.onNext(1);
        ap.onComplete();

        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

        ap.subscribe(ts);

        ts
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
        .assertResult(1);
    }

    @Test
    public void onTerminateCalledWhenOnError() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastProcessor<Integer> us = UnicastProcessor.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        assertEquals(false, didRunOnTerminate.get());
        us.onError(new RuntimeException("some error"));
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test
    public void onTerminateCalledWhenOnComplete() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastProcessor<Integer> us = UnicastProcessor.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        assertEquals(false, didRunOnTerminate.get());
        us.onComplete();
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test
    public void onTerminateCalledWhenCanceled() {
        final AtomicBoolean didRunOnTerminate = new AtomicBoolean();

        UnicastProcessor<Integer> us = UnicastProcessor.create(Observable.bufferSize(), new Runnable() {
            @Override public void run() {
                didRunOnTerminate.set(true);
            }
        });

        final Disposable subscribe = us.subscribe();

        assertEquals(false, didRunOnTerminate.get());
        subscribe.dispose();
        assertEquals(true, didRunOnTerminate.get());
    }

    @Test(expected = NullPointerException.class)
    public void nullOnTerminate() {
        UnicastProcessor.create(5, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCapacityHint() {
        UnicastProcessor.create(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroCapacityHint() {
        UnicastProcessor.create(0);
    }

    @Test
    public void onNextNull() {
        final UnicastProcessor<Object> p = UnicastProcessor.create();

        p.onNext(null);

        p.test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNull() {
        final UnicastProcessor<Object> p = UnicastProcessor.create();

        p.onError(null);

        p.test()
            .assertNoValues()
            .assertError(NullPointerException.class)
            .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }
}
