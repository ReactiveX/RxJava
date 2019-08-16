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
package io.reactivex.rxjava3.observable;

import java.util.Arrays;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableFuseableTest extends RxJavaTest {

    @Test
    public void syncRange() {

        Observable.range(1, 10)
        .to(TestHelper.<Integer>testConsumer(QueueFuseable.ANY, false))
        .assertFusionMode(QueueFuseable.SYNC)
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncArray() {

        Observable.fromArray(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
        .to(TestHelper.<Integer>testConsumer(QueueFuseable.ANY, false))
        .assertFusionMode(QueueFuseable.SYNC)
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncIterable() {

        Observable.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .to(TestHelper.<Integer>testConsumer(QueueFuseable.ANY, false))
        .assertFusionMode(QueueFuseable.SYNC)
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncRangeHidden() {

        Observable.range(1, 10).hide()
        .to(TestHelper.<Integer>testConsumer(QueueFuseable.ANY, false))
        .assertNotFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncArrayHidden() {
        Observable.fromArray(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
        .hide()
        .to(TestHelper.<Integer>testConsumer(QueueFuseable.ANY, false))
        .assertNotFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncIterableHidden() {
        Observable.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .hide()
        .to(TestHelper.<Integer>testConsumer(QueueFuseable.ANY, false))
        .assertNotFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }
}
