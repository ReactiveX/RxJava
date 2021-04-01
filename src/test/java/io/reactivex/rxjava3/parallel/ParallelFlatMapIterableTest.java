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

package io.reactivex.rxjava3.parallel;

import java.util.Arrays;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;

public class ParallelFlatMapIterableTest extends RxJavaTest {

    @Test
    public void subscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .flatMapIterable(v -> Arrays.asList(1, 2, 3)));
    }

    @Test
    public void normal() {
        for (int i = 1; i < 32; i++) {
            Flowable.range(1, 1000)
            .parallel(i)
            .flatMapIterable(v -> Arrays.asList(v, v + 1))
            .sequential()
            .test()
            .withTag("Parallelism: " + i)
            .assertValueCount(2000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test
    public void none() {
        for (int i = 1; i < 32; i++) {
            Flowable.range(1, 1000)
            .parallel(i)
            .flatMapIterable(v -> Arrays.asList())
            .sequential()
            .test()
            .withTag("Parallelism: " + i)
            .assertResult();
        }
    }

    @Test
    public void mixed() {
        for (int i = 1; i < 32; i++) {
            Flowable.range(1, 1000)
            .parallel(i)
            .flatMapIterable(v -> v % 2 == 0 ? Arrays.asList(v) : Arrays.asList())
            .sequential()
            .test()
            .withTag("Parallelism: " + i)
            .assertValueCount(500)
            .assertNoErrors()
            .assertComplete();
        }
    }
}
