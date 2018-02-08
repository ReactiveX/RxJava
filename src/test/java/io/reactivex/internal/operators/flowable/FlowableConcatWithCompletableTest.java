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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Action;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.subjects.CompletableSubject;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableConcatWithCompletableTest {

    @Test
    public void normal() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5)
        .concatWith(Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                ts.onNext(100);
            }
        }))
        .subscribe(ts);

        ts.assertResult(1, 2, 3, 4, 5, 100);
    }

    @Test
    public void mainError() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.<Integer>error(new TestException())
        .concatWith(Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                ts.onNext(100);
            }
        }))
        .subscribe(ts);

        ts.assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5)
        .concatWith(Completable.error(new TestException()))
        .subscribe(ts);

        ts.assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void takeMain() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5)
        .concatWith(Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                ts.onNext(100);
            }
        }))
        .take(3)
        .subscribe(ts);

        ts.assertResult(1, 2, 3);
    }

    @Test
    public void cancelOther() {
        CompletableSubject other = CompletableSubject.create();

        TestSubscriber<Object> ts = Flowable.empty()
                .concatWith(other)
                .test();

        assertTrue(other.hasObservers());

        ts.cancel();

        assertFalse(other.hasObservers());
    }

    @Test
    public void badSource() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                BooleanSubscription bs1 = new BooleanSubscription();
                s.onSubscribe(bs1);

                BooleanSubscription bs2 = new BooleanSubscription();
                s.onSubscribe(bs2);

                assertFalse(bs1.isCancelled());
                assertTrue(bs2.isCancelled());

                s.onComplete();
            }
        }.concatWith(Completable.complete())
        .test()
        .assertResult();
    }
}
