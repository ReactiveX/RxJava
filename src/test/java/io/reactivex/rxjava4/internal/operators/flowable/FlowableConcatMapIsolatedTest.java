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

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StandardBufferedConfig;
import io.reactivex.rxjava4.core.ErrorMode;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class FlowableConcatMapIsolatedTest extends RxJavaTest {

    @Test
    public void innerScalarRequestRace() {
        Flowable<Integer> just = Flowable.just(1);
        int n = 1000;
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Flowable<Integer>> source = PublishProcessor.create();

            TestSubscriber<Integer> ts = source
                    .concatMap(v -> v, new StandardBufferedConfig(n + 1))
                    .test(1L);

            TestHelper.race(() -> {
                for (int j = 0; j < n; j++) {
                    source.onNext(just);
                }
            }, () -> {
                for (int j = 0; j < n; j++) {
                    ts.request(1);
                }
            });

            ts.assertValueCount(n);
        }
    }

    @Test
    public void innerScalarRequestRaceDelayError() {
        Flowable<Integer> just = Flowable.just(1);
        int n = 1000;
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Flowable<Integer>> source = PublishProcessor.create();

            TestSubscriber<Integer> ts = source
                    .concatMap(v -> v, new StandardBufferedConfig(ErrorMode.END, n + 1))
                    .test(1L);

            TestHelper.race(() -> {
                for (int j = 0; j < n; j++) {
                    source.onNext(just);
                }
            }, () -> {
                for (int j = 0; j < n; j++) {
                    ts.request(1);
                }
            });

            ts.assertValueCount(n);
        }
    }
}
