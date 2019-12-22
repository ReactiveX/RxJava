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

package io.reactivex.rxjava3.internal.subscribers;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.internal.subscriptions.ScalarSubscription;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class BasicFuseableSubscriberTest extends RxJavaTest {

    @Test
    public void offerThrows() {
        BasicFuseableSubscriber<Integer, Integer> fcs = new BasicFuseableSubscriber<Integer, Integer>(new TestSubscriber<>(0L)) {

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

        fcs.onSubscribe(new ScalarSubscription<>(fcs, 1));

        TestHelper.assertNoOffer(fcs);

        assertFalse(fcs.isEmpty());
        fcs.clear();
        assertTrue(fcs.isEmpty());
    }
}
