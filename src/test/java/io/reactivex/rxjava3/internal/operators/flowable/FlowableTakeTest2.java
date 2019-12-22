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

import java.util.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

// moved tests from FlowableLimitTest to here (limit removed as operator)
public class FlowableTakeTest2 extends RxJavaTest implements LongConsumer, Action {

    final List<Long> requests = new ArrayList<>();

    static final Long CANCELLED = -100L;

    @Override
    public void accept(long t) throws Exception {
        requests.add(t);
    }

    @Override
    public void run() throws Exception {
        requests.add(CANCELLED);
    }

    @Test
    public void shorterSequence() {
        Flowable.range(1, 5)
        .doOnRequest(this)
        .take(6)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(6, requests.get(0).intValue());
    }

    @Test
    public void exactSequence() {
        Flowable.range(1, 5)
        .doOnRequest(this)
        .doOnCancel(this)
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(2, requests.size());
        assertEquals(5, requests.get(0).intValue());
        assertEquals(CANCELLED, requests.get(1));
    }

    @Test
    public void longerSequence() {
        Flowable.range(1, 6)
        .doOnRequest(this)
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, requests.get(0).intValue());
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .take(5)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void takeZero() {
        Flowable.range(1, 5)
        .doOnCancel(this)
        .doOnRequest(this)
        .take(0)
        .test()
        .assertResult();

        assertEquals(1, requests.size());
        assertEquals(CANCELLED, requests.get(0));
    }

    @Test
    public void takeStep() {
        TestSubscriber<Integer> ts = Flowable.range(1, 6)
        .doOnRequest(this)
        .take(5)
        .test(0L);

        assertEquals(0, requests.size());

        ts.request(1);
        ts.assertValue(1);

        ts.request(2);
        ts.assertValues(1, 2, 3);

        ts.request(3);
        ts.assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(1L, 2L, 2L), requests);
    }

    @Test
    public void takeThenTake() {
        Flowable.range(1, 5)
        .doOnCancel(this)
        .doOnRequest(this)
        .take(6)
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(Arrays.asList(5L, CANCELLED), requests);
    }

    @Test
    public void noOverrequest() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
                .doOnRequest(this)
                .take(5)
                .test(0L);

        ts.request(5);
        ts.request(10);

        assertTrue(pp.offer(1));
        pp.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void cancelIgnored() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    BooleanSubscription bs = new BooleanSubscription();
                    s.onSubscribe(bs);

                    assertTrue(bs.isCancelled());

                    s.onNext(1);
                    s.onComplete();
                    s.onError(new TestException());

                    s.onSubscribe(null);
                }
            }
            .take(0)
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
            TestHelper.assertError(errors, 1, NullPointerException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.range(1, 5).take(3));
    }

    @Test
    public void requestRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = Flowable.range(1, 10)
                    .take(5)
                    .test(0L);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ts.request(3);
                }
            };

            TestHelper.race(r, r);

            ts.assertResult(1, 2, 3, 4, 5);
        }
    }

    @Test
    public void errorAfterLimitReached() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.error(new TestException())
            .take(0)
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
