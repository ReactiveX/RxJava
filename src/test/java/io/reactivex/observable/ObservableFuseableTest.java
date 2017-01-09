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
package io.reactivex.observable;

import java.util.Arrays;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.observers.ObserverFusion;

public class ObservableFuseableTest {

    @Test
    public void syncRange() {

        Observable.range(1, 10)
        .to(ObserverFusion.<Integer>test(QueueSubscription.ANY, false))
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueSubscription.SYNC))
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncArray() {

        Observable.fromArray(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
        .to(ObserverFusion.<Integer>test(QueueSubscription.ANY, false))
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueSubscription.SYNC))
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncIterable() {

        Observable.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .to(ObserverFusion.<Integer>test(QueueSubscription.ANY, false))
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueSubscription.SYNC))
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncRangeHidden() {

        Observable.range(1, 10).hide()
        .to(ObserverFusion.<Integer>test(QueueSubscription.ANY, false))
        .assertOf(ObserverFusion.<Integer>assertNotFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueSubscription.NONE))
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncArrayHidden() {
        Observable.fromArray(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
        .hide()
        .to(ObserverFusion.<Integer>test(QueueSubscription.ANY, false))
        .assertOf(ObserverFusion.<Integer>assertNotFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueSubscription.NONE))
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncIterableHidden() {
        Observable.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .hide()
        .to(ObserverFusion.<Integer>test(QueueSubscription.ANY, false))
        .assertOf(ObserverFusion.<Integer>assertNotFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueSubscription.NONE))
        .assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        .assertNoErrors()
        .assertComplete();
    }
}
