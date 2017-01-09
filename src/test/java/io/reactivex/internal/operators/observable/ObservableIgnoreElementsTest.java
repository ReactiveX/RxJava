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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;

public class ObservableIgnoreElementsTest {

    @Test
    public void testWithEmptyObservable() {
        assertTrue(Observable.empty().ignoreElements().toObservable().isEmpty().blockingGet());
    }

    @Test
    public void testWithNonEmptyObservable() {
        assertTrue(Observable.just(1, 2, 3).ignoreElements().toObservable().isEmpty().blockingGet());
    }

    @Test
    public void testUpstreamIsProcessedButIgnoredObservable() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        long count = Observable.range(1, num)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                .ignoreElements()
                .toObservable()
                .count().blockingGet();
        assertEquals(num, upstreamCount.get());
        assertEquals(0, count);
    }

    @Test
    public void testCompletedOkObservable() {
        TestObserver<Object> ts = new TestObserver<Object>();
        Observable.range(1, 10).ignoreElements().toObservable().subscribe(ts);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
    }

    @Test
    public void testErrorReceivedObservable() {
        TestObserver<Object> ts = new TestObserver<Object>();
        TestException ex = new TestException("boo");
        Observable.error(ex).ignoreElements().toObservable().subscribe(ts);
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
        ts.assertError(TestException.class);
        ts.assertErrorMessage("boo");
    }

    @Test
    public void testUnsubscribesFromUpstreamObservable() {
        final AtomicBoolean unsub = new AtomicBoolean();
        Observable.range(1, 10).concatWith(Observable.<Integer>never())
        .doOnDispose(new Action() {
            @Override
            public void run() {
                unsub.set(true);
            }})
            .ignoreElements()
            .toObservable()
            .subscribe()
            .dispose();
        assertTrue(unsub.get());
    }

    @Test
    public void testWithEmpty() {
        assertNull(Observable.empty().ignoreElements().blockingGet());
    }

    @Test
    public void testWithNonEmpty() {
        assertNull(Observable.just(1, 2, 3).ignoreElements().blockingGet());
    }

    @Test
    public void testUpstreamIsProcessedButIgnored() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        Object count = Observable.range(1, num)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                .ignoreElements()
                .blockingGet();
        assertEquals(num, upstreamCount.get());
        assertNull(count);
    }

    @Test
    public void testCompletedOk() {
        TestObserver<Object> ts = new TestObserver<Object>();
        Observable.range(1, 10).ignoreElements().subscribe(ts);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
    }

    @Test
    public void testErrorReceived() {
        TestObserver<Object> ts = new TestObserver<Object>();
        TestException ex = new TestException("boo");
        Observable.error(ex).ignoreElements().subscribe(ts);
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
        ts.assertError(TestException.class);
        ts.assertErrorMessage("boo");
    }

    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsub = new AtomicBoolean();
        Observable.range(1, 10).concatWith(Observable.<Integer>never())
        .doOnDispose(new Action() {
            @Override
            public void run() {
                unsub.set(true);
            }})
            .ignoreElements()
            .subscribe()
            .dispose();
        assertTrue(unsub.get());
    }

    @Test
    public void cancel() {

        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Integer> ts = pp.ignoreElements().<Integer>toObservable().test();

        assertTrue(pp.hasObservers());

        ts.cancel();

        assertFalse(pp.hasObservers());

        TestHelper.checkDisposed(pp.ignoreElements().<Integer>toObservable());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).ignoreElements());

        TestHelper.checkDisposed(Observable.just(1).ignoreElements().toObservable());
    }
}
