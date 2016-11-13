/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.operators;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.*;
import rx.Scheduler.Worker;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class DeferredScalarSubscriberTest {

    @Test
    public void completeFirst() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ts.assertNoValues();

        ds.onCompleted();

        ts.assertNoValues();

        ts.requestMore(1);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void requestFirst() {
        TestSubscriber<Integer> ts = TestSubscriber.create(1);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ts.assertNoValues();

        ds.onCompleted();

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void empty() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ts.assertNoValues();

        ds.onCompleted();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
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
        ts.assertNotCompleted();
    }

    @Test
    public void unsubscribeComposes() {
        PublishSubject<Integer> ps = PublishSubject.create();
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);

        ds.subscribeTo(ps);

        assertTrue("No subscribers?", ps.hasObservers());

        ts.unsubscribe();

        ds.onNext(1);
        ds.onCompleted();

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        assertFalse("Subscribers?", ps.hasObservers());
        assertTrue("Deferred not unsubscribed?", ds.isUnsubscribed());
    }

    @Test
    public void emptySource() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.subscribeTo(Observable.just(1).ignoreElements()); // we need a producer from upstream

        ts.assertNoValues();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void justSource() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.subscribeTo(Observable.just(1));

        ts.assertNoValues();

        ts.requestMore(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void rangeSource() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.subscribeTo(Observable.range(1, 10));

        ts.assertNoValues();

        ts.requestMore(1);

        ts.assertValue(10);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void completeAfterNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                unsubscribe();
            }
        };
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();
        ds.onNext(1);

        ts.assertNoValues();

        ds.onCompleted();

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void completeAfterNextViaRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                unsubscribe();
            }
        };
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();
        ds.onNext(1);
        ds.onCompleted();

        ts.assertNoValues();

        ts.requestMore(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void doubleComplete() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ts.requestMore(1);

        ds.onCompleted();
        ds.onCompleted();


        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void doubleComplete2() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ds.onCompleted();
        ds.onCompleted();

        ts.requestMore(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void doubleRequest() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ds.onNext(1);

        ts.requestMore(1);
        ts.requestMore(1);

        ds.onCompleted();

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void negativeRequest() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        try {
            ds.downstreamRequest(-99);
            fail("Failed to throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("n >= 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void callsAfterUnsubscribe() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        TestingDeferredScalarSubscriber ds = new TestingDeferredScalarSubscriber(ts);
        ds.setupDownstream();

        ts.unsubscribe();

        ds.downstreamRequest(1);
        ds.onNext(1);
        ds.onCompleted();
        ds.onCompleted();

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
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

                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        ready.decrementAndGet();
                        while (ready.get() != 0) { }

                        ts.requestMore(1);
                    }
                });

                ready.decrementAndGet();
                while (ready.get() != 0) { }

                ds.onCompleted();

                ts.awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS);
                ts.assertValues(1);
                ts.assertNoErrors();
                ts.assertCompleted();

            }
        } finally {
            w.unsubscribe();
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

                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        ready.decrementAndGet();
                        while (ready.get() != 0) { }

                        ts.requestMore(1);
                    }
                });

                w2.schedule(new Action0() {
                    @Override
                    public void call() {
                        ready.decrementAndGet();
                        while (ready.get() != 0) { }

                        ts.requestMore(1);
                    }
                });

                ready.decrementAndGet();
                while (ready.get() != 0) { }

                ds.onCompleted();

                ts.awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS);
                ts.assertValues(1);
                ts.assertNoErrors();
                ts.assertCompleted();

            }
        } finally {
            w.unsubscribe();
            w2.unsubscribe();
        }
    }

    static final class TestingDeferredScalarSubscriber extends DeferredScalarSubscriber<Integer, Integer> {

        public TestingDeferredScalarSubscriber(Subscriber<? super Integer> actual) {
            super(actual);
        }

        @Override
        public void onNext(Integer t) {
            value = t;
            hasValue = true;
        }
    }
}
