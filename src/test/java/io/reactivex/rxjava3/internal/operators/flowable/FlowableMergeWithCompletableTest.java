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

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableMergeWithCompletableTest extends RxJavaTest {

    @Test
    public void normal() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.range(1, 5).mergeWith(
                Completable.fromAction(new Action() {
                    @Override
                    public void run() throws Exception {
                        ts.onNext(100);
                    }
                })
        )
        .subscribe(ts);

        ts.assertResult(1, 2, 3, 4, 5, 100);
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .mergeWith(Completable.complete())
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void cancel() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();
        final CompletableSubject cs = CompletableSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs).test();

        assertTrue(pp.hasSubscribers());
        assertTrue(cs.hasObservers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
        assertFalse(cs.hasObservers());
    }

    @Test
    public void normalBackpressured() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>(0L);

        Flowable.range(1, 5).mergeWith(
                Completable.fromAction(new Action() {
                    @Override
                    public void run() throws Exception {
                        ts.onNext(100);
                    }
                })
        )
        .subscribe(ts);

        ts
        .assertValue(100)
        .requestMore(2)
        .assertValues(100, 1, 2)
        .requestMore(2)
        .assertValues(100, 1, 2, 3, 4)
        .requestMore(1)
        .assertResult(100, 1, 2, 3, 4, 5);
    }

    @Test
    public void mainError() {
        Flowable.error(new TestException())
        .mergeWith(Completable.complete())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        Flowable.never()
        .mergeWith(Completable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void completeRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final CompletableSubject cs = CompletableSubject.create();

            TestSubscriber<Integer> ts = pp.mergeWith(cs).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cs.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }

    @Test
    public void cancelOtherOnMainError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        CompletableSubject cs = CompletableSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs).test();

        assertTrue(pp.hasSubscribers());
        assertTrue(cs.hasObservers());

        pp.onError(new TestException());

        ts.assertFailure(TestException.class);

        assertFalse("main has observers!", pp.hasSubscribers());
        assertFalse("other has observers", cs.hasObservers());
    }

    @Test
    public void cancelMainOnOtherError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        CompletableSubject cs = CompletableSubject.create();

        TestSubscriber<Integer> ts = pp.mergeWith(cs).test();

        assertTrue(pp.hasSubscribers());
        assertTrue(cs.hasObservers());

        cs.onError(new TestException());

        ts.assertFailure(TestException.class);

        assertFalse("main has observers!", pp.hasSubscribers());
        assertFalse("other has observers", cs.hasObservers());
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> upstream) {
                return upstream.mergeWith(Completable.complete().hide());
            }
        });
    }
}
