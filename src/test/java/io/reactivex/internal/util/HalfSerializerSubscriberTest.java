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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class HalfSerializerSubscriberTest {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(HalfSerializer.class);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnNext() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Subscriber[] a = { null };

        final TestSubscriber ts = new TestSubscriber();

        FlowableSubscriber s = new FlowableSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onNext(a[0], 2, wip, error);
                }
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
        };

        a[0] = s;

        s.onSubscribe(new BooleanSubscription());

        HalfSerializer.onNext(s, 1, wip, error);

        ts.assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnError() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Subscriber[] a = { null };

        final TestSubscriber ts = new TestSubscriber();

        FlowableSubscriber s = new FlowableSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onError(a[0], new TestException(), wip, error);
                }
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
        };

        a[0] = s;

        s.onSubscribe(new BooleanSubscription());

        HalfSerializer.onNext(s, 1, wip, error);

        ts.assertFailure(TestException.class, 1);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantOnNextOnComplete() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Subscriber[] a = { null };

        final TestSubscriber ts = new TestSubscriber();

        FlowableSubscriber s = new FlowableSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                if (t.equals(1)) {
                    HalfSerializer.onComplete(a[0], wip, error);
                }
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
        };

        a[0] = s;

        s.onSubscribe(new BooleanSubscription());

        HalfSerializer.onNext(s, 1, wip, error);

        ts.assertResult(1);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reentrantErrorOnError() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicThrowable error = new AtomicThrowable();

        final Subscriber[] a = { null };

        final TestSubscriber ts = new TestSubscriber();

        FlowableSubscriber s = new FlowableSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Object t) {
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
                HalfSerializer.onError(a[0], new IOException(), wip, error);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }
        };

        a[0] = s;

        s.onSubscribe(new BooleanSubscription());

        HalfSerializer.onError(s, new TestException(), wip, error);

        ts.assertFailure(TestException.class);
    }

    @Test
    public void onNextOnCompleteRace() {
        for (int i = 0; i < 500; i++) {

            final AtomicInteger wip = new AtomicInteger();
            final AtomicThrowable error = new AtomicThrowable();

            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            ts.onSubscribe(new BooleanSubscription());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onNext(ts, 1, wip, error);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onComplete(ts, wip, error);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertComplete().assertNoErrors();

            assertTrue(ts.valueCount() <= 1);
        }
    }

    @Test
    public void onErrorOnCompleteRace() {
        for (int i = 0; i < 500; i++) {

            final AtomicInteger wip = new AtomicInteger();
            final AtomicThrowable error = new AtomicThrowable();

            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            ts.onSubscribe(new BooleanSubscription());

            final TestException ex = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onError(ts, ex, wip, error);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    HalfSerializer.onComplete(ts, wip, error);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            if (ts.completions() != 0) {
                ts.assertResult();
            } else {
                ts.assertFailure(TestException.class);
            }
        }
    }

}
