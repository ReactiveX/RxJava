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

import org.junit.Test;

import io.reactivex.internal.operators.flowable.FlowableConcatMap.WeakScalarSubscription;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableConcatMapTest {

    @Test
    public void weakSubscriptionRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        WeakScalarSubscription<Integer> ws = new WeakScalarSubscription<Integer>(1, ts);
        ts.onSubscribe(ws);

        ws.request(0);

        ts.assertEmpty();

        ws.request(1);

        ts.assertResult(1);

        ws.request(1);

        ts.assertResult(1);
    }

}
