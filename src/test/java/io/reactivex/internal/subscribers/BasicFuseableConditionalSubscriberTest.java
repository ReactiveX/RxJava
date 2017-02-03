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

package io.reactivex.internal.subscribers;

import static org.junit.Assert.*;

import io.reactivex.annotations.Nullable;
import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.TestHelper;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.ScalarSubscription;

public class BasicFuseableConditionalSubscriberTest {

    @Test
    public void offerThrows() {
        ConditionalSubscriber<Integer> cs = new ConditionalSubscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription s) {
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public boolean tryOnNext(Integer t) {
                return false;
            }
        };

        BasicFuseableConditionalSubscriber<Integer, Integer> fcs = new BasicFuseableConditionalSubscriber<Integer, Integer>(cs) {

            @Override
            public boolean tryOnNext(Integer t) {
                return false;
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public int requestFusion(int mode) {
                return 0;
            }

            @Nullable
            @Override
            public Integer poll() throws Exception {
                return null;
            }
        };

        fcs.onSubscribe(new ScalarSubscription<Integer>(fcs, 1));

        TestHelper.assertNoOffer(fcs);

        assertFalse(fcs.isEmpty());
        fcs.clear();
        assertTrue(fcs.isEmpty());
    }
}
