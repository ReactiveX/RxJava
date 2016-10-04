/**
 * Copyright 2016 Netflix, Inc.
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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableScalarXMapTest {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(FlowableScalarXMap.class);
    }

    static final class CallablePublisher implements Publisher<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            EmptySubscription.error(new TestException(), s);
        }

        @Override
        public Integer call() throws Exception {
            throw new TestException();
        }
    }

    static final class EmptyCallablePublisher implements Publisher<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            EmptySubscription.complete(s);
        }

        @Override
        public Integer call() throws Exception {
            return null;
        }
    }

    static final class OneCallablePublisher implements Publisher<Integer>, Callable<Integer> {
        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            s.onSubscribe(new ScalarSubscription<Integer>(s, 1));
        }

        @Override
        public Integer call() throws Exception {
            return 1;
        }
    }

    @Test
    public void tryScalarXMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new CallablePublisher(), ts, new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer f) throws Exception {
                return Flowable.just(1);
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void emptyXMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new EmptyCallablePublisher(), ts, new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer f) throws Exception {
                return Flowable.just(1);
            }
        }));

        ts.assertResult();
    }

    @Test
    public void mapperCrashes() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer f) throws Exception {
                throw new TestException();
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void mapperToJust() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer f) throws Exception {
                return Flowable.just(1);
            }
        }));

        ts.assertResult(1);
    }

    @Test
    public void mapperToEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer f) throws Exception {
                return Flowable.empty();
            }
        }));

        ts.assertResult();
    }

    @Test
    public void mapperToCrashingCallable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        assertTrue(FlowableScalarXMap.tryScalarXMapSubscribe(new OneCallablePublisher(), ts, new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer f) throws Exception {
                return new CallablePublisher();
            }
        }));

        ts.assertFailure(TestException.class);
    }

    @Test
    public void scalarMapToEmpty() {
        FlowableScalarXMap.scalarXMap(1, new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.empty();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void scalarMapToCrashingCallable() {
        FlowableScalarXMap.scalarXMap(1, new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return new CallablePublisher();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
}
