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

import static org.junit.Assert.*;
import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.UnicastProcessor;

public class ParallelFromPublisherTest {

    @Test
    public void sourceOverflow() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < 10; i++) {
                    s.onNext(i);
                }
            }
        }
        .parallel(1, 1)
        .sequential(1)
        .test(0)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void fusedFilterBecomesEmpty() {
        Flowable.just(1)
        .filter(Functions.alwaysFalse())
        .parallel()
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void syncFusedMapCrash() {
        Flowable.just(1)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .parallel()
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void asyncFusedMapCrash() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up.onNext(1);

        up
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .parallel()
        .sequential()
        .test()
        .assertFailure(TestException.class);

        assertFalse(up.hasSubscribers());
    }
}
