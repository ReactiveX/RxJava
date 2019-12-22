/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.reactivex.rxjava3.internal.subscribers;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class DeferredScalarSubscriberTest extends RxJavaTest {

    @Test
    public void completeFirst() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ts.assertNoValues();

        ds.onComplete();

        ts.assertNoValues();

        ts.request(1);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void requestFirst() {
        TestSubscriber<Integer> ts = TestSubscriber.create(1);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ts.assertNoValues();

        ds.onComplete();

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void empty() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ts.assertNoValues();

        ds.onComplete();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void error() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ts.assertNoValues();

        ds.onError(new TestException());

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void unsubscribeComposes() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);

        pp.subscribe(ds);

        assertTrue("No subscribers?", pp.hasSubscribers());

        ts.cancel();

        ds.onNext(1);
        ds.onComplete();

        ts.request(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        assertFalse("Subscribers?", pp.hasSubscribers());
        assertTrue("Deferred not unsubscribed?", ds.isCancelled());
    }

    @Test
    public void emptySource() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        Flowable.just(1).ignoreElements().<Integer>toFlowable().subscribe(ds); // we need a producer from upstream

        ts.assertNoValues();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void justSource() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.subscribeTo(Flowable.just(1));

        ts.assertNoValues();

        ts.request(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void rangeSource() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.subscribeTo(Flowable.range(1, 10));

        ts.assertNoValues();

        ts.request(1);

        ts.assertValue(10);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void completeAfterNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
            }
        };
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();
        ds.onNext(1);

        ts.assertNoValues();

        ds.onComplete();

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void completeAfterNextViaRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
            }
        };
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();
        ds.onNext(1);
        ds.onComplete();

        ts.assertNoValues();

        ts.request(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void doubleComplete() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ts.request(1);

        ds.onComplete();
        ds.onComplete();

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void doubleComplete2() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ds.onComplete();
        ds.onComplete();

        ts.request(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void doubleRequest() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ts.request(1);
        ts.request(1);

        ds.onComplete();

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void negativeRequest() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.downstreamRequest(-99);

        RxJavaPlugins.reset();
        TestHelper.assertError(list, 0, IllegalArgumentException.class, "n > 0 required but it was -99");
    }

    @Test
    public void callsAfterUnsubscribe() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ts.cancel();

        ds.downstreamRequest(1);
        ds.onNext(1);
        ds.onComplete();
        ds.onComplete();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void emissionRequestRace() {
        Worker w = Schedulers.computation().createWorker();
        try {
            for (int i = 0; i < 10000; i++) {

                final TestSubscriber<Integer> ts = TestSubscriber.create(0L);
                TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
                ds.setupDownstream();
                ds.onNext(1);

                final AtomicInteger ready = new AtomicInteger(2);

                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        ready.decrementAndGet();
                        while (ready.get() != 0) { }

                        ts.request(1);
                    }
                });

                ready.decrementAndGet();
                while (ready.get() != 0) { }

                ds.onComplete();

                ts.awaitDone(5, TimeUnit.SECONDS);
                ts.assertValues(1);
                ts.assertNoErrors();
                ts.assertComplete();

            }
        } finally {
            w.dispose();
        }
    }

    @Test
    public void emissionRequestRace2() {
        Worker w = Schedulers.io().createWorker();
        Worker w2 = Schedulers.io().createWorker();
        int m = 10000;
        if (Runtime.getRuntime().availableProcessors() < 3) {
            m = 1000;
        }
        try {
            for (int i = 0; i < m; i++) {

                final TestSubscriber<Integer> ts = TestSubscriber.create(0L);
                TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
                ds.setupDownstream();
                ds.onNext(1);

                final AtomicInteger ready = new AtomicInteger(3);

                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        ready.decrementAndGet();
                        while (ready.get() != 0) { }

                        ts.request(1);
                    }
                });

                w2.schedule(new Runnable() {
                    @Override
                    public void run() {
                        ready.decrementAndGet();
                        while (ready.get() != 0) { }

                        ts.request(1);
                    }
                });

                ready.decrementAndGet();
                while (ready.get() != 0) { }

                ds.onComplete();

                ts.awaitDone(5, TimeUnit.SECONDS);
                ts.assertValues(1);
                ts.assertNoErrors();
                ts.assertComplete();

            }
        } finally {
            w.dispose();
            w2.dispose();
        }
    }

    static final class TestingDeferredScalarSubscriber extends DeferredScalarSubscriber<Integer, Integer> {

        private static final long serialVersionUID = 6285096158319517837L;

        TestingDeferredScalarSubscriber(Subscriber<? super Integer> downstream) {
            super(downstream);
        }

        @Override
        public void onNext(Integer t) {
            value = t;
            hasValue = true;
        }

        public void setupDownstream() {
            onSubscribe(new BooleanSubscription());
        }

        public void subscribeTo(Publisher<Integer> p) {
            p.subscribe(this);
        }

        public void downstreamRequest(long n) {
            request(n);
        }
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.doubleOnSubscribe(new DeferredScalarSubscriber<Integer, Integer>(new TestSubscriber<>()) {
            private static final long serialVersionUID = -4445381578878059054L;

            @Override
            public void onNext(Integer t) {
            }
        });
    }
}
