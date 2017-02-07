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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Action;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.PublishSubject;

public class CompletableDisposeOnTest {

    @Test
    public void cancelDelayed() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> ps = PublishSubject.create();

        ps.ignoreElements()
        .unsubscribeOn(scheduler)
        .test()
        .cancel();

        assertTrue(ps.hasObservers());

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().ignoreElements().unsubscribeOn(new TestScheduler()));
    }

    @Test
    public void completeAfterCancel() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Void> to = ps.ignoreElements()
        .unsubscribeOn(scheduler)
        .test();

        to.dispose();

        ps.onComplete();

        to.assertEmpty();
    }

    @Test
    public void errorAfterCancel() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestScheduler scheduler = new TestScheduler();

            PublishSubject<Integer> ps = PublishSubject.create();

            TestObserver<Void> to = ps.ignoreElements()
            .unsubscribeOn(scheduler)
            .test();

            to.dispose();

            ps.onError(new TestException());

            to.assertEmpty();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void normal() {
        TestScheduler scheduler = new TestScheduler();

        final int[] call = { 0 };

        Completable.complete()
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .unsubscribeOn(scheduler)
        .test()
        .assertResult();

        scheduler.triggerActions();

        assertEquals(0, call[0]);
    }

    @Test
    public void error() {
        TestScheduler scheduler = new TestScheduler();

        final int[] call = { 0 };

        Completable.error(new TestException())
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .unsubscribeOn(scheduler)
        .test()
        .assertFailure(TestException.class);

        scheduler.triggerActions();

        assertEquals(0, call[0]);
    }
}
