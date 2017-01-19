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

package io.reactivex.parallel;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.subscribers.TestSubscriber;

public class ParallelJoinTest {

    @Test
    public void overflowFastpath() {
        new ParallelFlowable<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer>[] subscribers) {
                subscribers[0].onSubscribe(new BooleanSubscription());
                subscribers[0].onNext(1);
                subscribers[0].onNext(2);
                subscribers[0].onNext(3);
            }

            @Override
            public int parallelism() {
                return 1;
            }
        }
        .sequential(1)
        .test(0)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void overflowSlowpath() {
        @SuppressWarnings("unchecked")
        final Subscriber<? super Integer>[] subs = new Subscriber[1];

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                subs[0].onNext(2);
                subs[0].onNext(3);
            }
        };

        new ParallelFlowable<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer>[] subscribers) {
                subs[0] = subscribers[0];
                subscribers[0].onSubscribe(new BooleanSubscription());
                subscribers[0].onNext(1);
            }

            @Override
            public int parallelism() {
                return 1;
            }
        }
        .sequential(1)
        .subscribe(ts);

        ts.assertFailure(MissingBackpressureException.class, 1);
    }

    @Test
    public void emptyBackpressured() {
        Flowable.empty()
        .parallel()
        .sequential()
        .test(0)
        .assertResult();
    }
}
