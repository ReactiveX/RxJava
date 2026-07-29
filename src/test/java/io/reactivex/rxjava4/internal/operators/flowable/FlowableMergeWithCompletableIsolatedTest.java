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
import io.reactivex.rxjava4.subjects.CompletableSubject;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class FlowableMergeWithCompletableIsolatedTest extends RxJavaTest {

    @Test
    public void completeRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();
            final CompletableSubject cs = CompletableSubject.create();

            TestSubscriber<Integer> ts = pp.mergeWith(cs).test();

            Runnable r1 = () -> {
                pp.onNext(1);
                pp.onComplete();
            };

            Runnable r2 = cs::onComplete;

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }
}
