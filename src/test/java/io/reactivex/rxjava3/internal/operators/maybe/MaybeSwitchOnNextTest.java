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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.MaybeSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class MaybeSwitchOnNextTest extends RxJavaTest {

    @Test
    public void normal() {
        Maybe.switchOnNext(
                Flowable.range(1, 10)
                .map(v -> {
                    if (v % 2 == 0) {
                        return Maybe.just(v);
                    }
                    return Maybe.empty();
                })
        )
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void normalDelayError() {
        Maybe.switchOnNextDelayError(
                Flowable.range(1, 10)
                .map(v -> {
                    if (v % 2 == 0) {
                        return Maybe.just(v);
                    }
                    return Maybe.empty();
                })
        )
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void noDelaySwitch() {
        PublishProcessor<Maybe<Integer>> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Maybe.switchOnNext(pp).test();

        assertTrue(pp.hasSubscribers());

        ts.assertEmpty();

        MaybeSubject<Integer> ms1 = MaybeSubject.create();
        MaybeSubject<Integer> ms2 = MaybeSubject.create();

        pp.onNext(ms1);

        assertTrue(ms1.hasObservers());

        pp.onNext(ms2);

        assertFalse(ms1.hasObservers());
        assertTrue(ms2.hasObservers());

        pp.onComplete();

        assertTrue(ms2.hasObservers());

        ms2.onSuccess(1);

        ts.assertResult(1);
    }

    @Test
    public void delaySwitch() {
        PublishProcessor<Maybe<Integer>> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Maybe.switchOnNextDelayError(pp).test();

        assertTrue(pp.hasSubscribers());

        ts.assertEmpty();

        MaybeSubject<Integer> ms1 = MaybeSubject.create();
        MaybeSubject<Integer> ms2 = MaybeSubject.create();

        pp.onNext(ms1);

        assertTrue(ms1.hasObservers());

        pp.onNext(ms2);

        assertFalse(ms1.hasObservers());
        assertTrue(ms2.hasObservers());

        assertTrue(ms2.hasObservers());

        ms2.onError(new TestException());

        assertTrue(pp.hasSubscribers());

        ts.assertEmpty();

        pp.onComplete();

        ts.assertFailure(TestException.class);
    }
}
