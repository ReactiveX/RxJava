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

package io.reactivex.rxjava4.internal.operators.flowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.subjects.MaybeSubject;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class FlowableMergeWithMaybeIsolatedTest extends RxJavaTest {

    @Test
    public void completeRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final MaybeSubject<Integer> cs = MaybeSubject.create();

            TestSubscriber<Integer> ts = pp.mergeWith(cs).test();

            Runnable r1 = () -> {
                pp.onNext(1);
                pp.onComplete();
            };

            Runnable r2 = () -> cs.onSuccess(1);

            TestHelper.race(r1, r2);

            ts.assertResult(1, 1);
        }
    }

    @Test
    public void onSuccessFastPathBackpressuredRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final MaybeSubject<Integer> cs = MaybeSubject.create();

            final TestSubscriber<Integer> ts = pp.mergeWith(cs).subscribeWith(new TestSubscriber<>(0));

            Runnable r1 = () -> cs.onSuccess(1);

            Runnable r2 = () -> ts.request(2);

            TestHelper.race(r1, r2);

            pp.onNext(2);
            pp.onComplete();

            ts.assertResult(1, 2);
        }
    }

    @Test
    public void onNextRequestRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final MaybeSubject<Integer> cs = MaybeSubject.create();

            final TestSubscriber<Integer> ts = pp.mergeWith(cs).test(0);

            pp.onNext(0);

            Runnable r1 = () -> pp.onNext(1);

            Runnable r2 = () -> ts.request(3);

            TestHelper.race(r1, r2);

            cs.onSuccess(1);
            pp.onComplete();

            ts.assertResult(0, 1, 1);
        }
    }
}
