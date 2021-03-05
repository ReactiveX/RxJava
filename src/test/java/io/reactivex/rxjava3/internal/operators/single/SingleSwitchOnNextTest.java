/*
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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class SingleSwitchOnNextTest extends RxJavaTest {

    @Test
    public void normal() {
        Single.switchOnNext(
                Flowable.range(1, 5)
                .map(v -> {
                    if (v % 2 == 0) {
                        return Single.just(v);
                    }
                    return Single.just(10 + v);
                })
        )
        .test()
        .assertResult(11, 2, 13, 4, 15);
    }

    @Test
    public void normalDelayError() {
        Single.switchOnNextDelayError(
                Flowable.range(1, 5)
                .map(v -> {
                    if (v % 2 == 0) {
                        return Single.just(v);
                    }
                    return Single.just(10 + v);
                })
        )
        .test()
        .assertResult(11, 2, 13, 4, 15);
    }

    @Test
    public void noDelaySwitch() {
        PublishProcessor<Single<Integer>> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Single.switchOnNext(pp).test();

        assertTrue(pp.hasSubscribers());

        ts.assertEmpty();

        SingleSubject<Integer> ss1 = SingleSubject.create();
        SingleSubject<Integer> ss2 = SingleSubject.create();

        pp.onNext(ss1);

        assertTrue(ss1.hasObservers());

        pp.onNext(ss2);

        assertFalse(ss1.hasObservers());
        assertTrue(ss2.hasObservers());

        pp.onComplete();

        assertTrue(ss2.hasObservers());

        ss2.onSuccess(1);

        ts.assertResult(1);
    }

    @Test
    public void delaySwitch() {
        PublishProcessor<Single<Integer>> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Single.switchOnNextDelayError(pp).test();

        assertTrue(pp.hasSubscribers());

        ts.assertEmpty();

        SingleSubject<Integer> ss1 = SingleSubject.create();
        SingleSubject<Integer> ss2 = SingleSubject.create();

        pp.onNext(ss1);

        assertTrue(ss1.hasObservers());

        pp.onNext(ss2);

        assertFalse(ss1.hasObservers());
        assertTrue(ss2.hasObservers());

        assertTrue(ss2.hasObservers());

        ss2.onError(new TestException());

        assertTrue(pp.hasSubscribers());

        ts.assertEmpty();

        pp.onComplete();

        ts.assertFailure(TestException.class);
    }
}
