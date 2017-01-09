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

import org.junit.Test;

import io.reactivex.TestHelper;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.subscribers.TestSubscriber;

public class SinglePostCompleteSubscriberTest {

    @Test
    public void requestCompleteRace() {
        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

            final SinglePostCompleteSubscriber<Integer, Integer> spc = new SinglePostCompleteSubscriber<Integer, Integer>(ts) {
                private static final long serialVersionUID = -2848918821531562637L;

                @Override
                public void onNext(Integer t) {
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onComplete() {
                    complete(1);
                }
            };

            spc.onSubscribe(new BooleanSubscription());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    spc.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }
}
