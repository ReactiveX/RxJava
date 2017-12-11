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

package io.reactivex.processors;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.subscribers.TestSubscriber;

public class RefCountProcessorTest {

    @Test
    public void normal() {
        FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        PublishProcessor<Integer> source = PublishProcessor.create();

        assertFalse(source.hasSubscribers());

        source.subscribe(rcp);

        assertTrue(source.hasSubscribers());

        TestSubscriber<Integer> ts = rcp.test(1);

        source.onNext(1);

        ts.assertValue(1);

        ts.requestMore(3);

        source.onNext(2);

        ts.assertValues(1, 2);

        ts.cancel();

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void complete() {
        FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        PublishProcessor<Integer> source = PublishProcessor.create();

        assertFalse(source.hasSubscribers());

        source.subscribe(rcp);

        assertTrue(source.hasSubscribers());

        TestSubscriber<Integer> ts = rcp.test(1);

        assertFalse(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertTrue(rcp.hasSubscribers());

        source.onNext(1);

        ts.assertValue(1);

        ts.requestMore(3);

        source.onNext(2);

        ts.assertValues(1, 2);

        source.onComplete();

        assertFalse(source.hasSubscribers());

        ts.assertResult(1, 2);

        rcp.test().assertResult();

        assertTrue(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertFalse(rcp.hasSubscribers());
    }

    @Test
    public void error() {
        FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        PublishProcessor<Integer> source = PublishProcessor.create();

        assertFalse(source.hasSubscribers());

        source.subscribe(rcp);

        assertTrue(source.hasSubscribers());

        TestSubscriber<Integer> ts = rcp.test(1);

        assertFalse(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertTrue(rcp.hasSubscribers());

        source.onNext(1);

        ts.assertValue(1);

        ts.requestMore(3);

        source.onNext(2);

        ts.assertValues(1, 2);

        source.onError(new TestException());

        assertFalse(source.hasSubscribers());

        ts.assertFailure(TestException.class, 1, 2);

        rcp.test().assertFailure(TestException.class);

        assertFalse(rcp.hasComplete());
        assertTrue(rcp.hasThrowable());
        assertNotNull(rcp.getThrowable());
        assertFalse(rcp.hasSubscribers());
    }

    @Test
    public void multipleSubscribers() {
        FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        PublishProcessor<Integer> source = PublishProcessor.create();

        source.subscribe(rcp);

        assertTrue(source.hasSubscribers());

        TestSubscriber<Integer> ts1 = rcp.test();
        TestSubscriber<Integer> ts2 = rcp.test();

        ts1.cancel();

        assertTrue(source.hasSubscribers());

        ts2.cancel();

        assertFalse(source.hasSubscribers());

        rcp.test().assertFailureAndMessage(IllegalStateException.class, "RefCountProcessor terminated");
    }

    @Test
    public void immediatelyCancelled() {
        FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        PublishProcessor<Integer> source = PublishProcessor.create();

        source.subscribe(rcp);

        assertTrue(source.hasSubscribers());

        rcp.test(1L, true);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void cancelTwice() {
        FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        PublishProcessor<Integer> source = PublishProcessor.create();

        source.subscribe(rcp);

        assertTrue(source.hasSubscribers());

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        rcp.subscribe(new FlowableSubscriber<Integer>() {

            @Override
            public void onNext(Integer t) {
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }

            @Override
            public void onSubscribe(Subscription s) {
                ts.onSubscribe(s);
                s.cancel();
                s.cancel();
            }
        });

        assertFalse(source.hasSubscribers());

        ts.assertEmpty();
    }

    @Test
    public void cancelTwiceDontCancelUp() {
        FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        PublishProcessor<Integer> source = PublishProcessor.create();

        source.subscribe(rcp);

        assertTrue(source.hasSubscribers());

        TestSubscriber<Integer> ts0 = rcp.test();

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        rcp.subscribe(new FlowableSubscriber<Integer>() {

            @Override
            public void onNext(Integer t) {
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }

            @Override
            public void onSubscribe(Subscription s) {
                ts.onSubscribe(s);
                s.cancel();
                s.cancel();
            }
        });

        assertTrue(source.hasSubscribers());

        ts.assertEmpty();

        source.onNext(1);
        source.onComplete();

        ts0.assertResult(1);
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 1000; i++) {
            final FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

            PublishProcessor<Integer> source = PublishProcessor.create();

            source.subscribe(rcp);

            assertTrue(source.hasSubscribers());

            final TestSubscriber<Integer> ts1 = rcp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    rcp.test().cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void doubleOnSubscribe() {
        final FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        BooleanSubscription bs1 = new BooleanSubscription();

        rcp.onSubscribe(bs1);

        BooleanSubscription bs2 = new BooleanSubscription();

        rcp.onSubscribe(bs2);

        assertFalse(bs1.isCancelled());
        assertTrue(bs2.isCancelled());
    }

    @Test
    public void doubleRefCount() {
        final FlowableProcessor<Integer> rcp = PublishProcessor.<Integer>create().refCount();

        assertSame(rcp, rcp.refCount());
    }
}
