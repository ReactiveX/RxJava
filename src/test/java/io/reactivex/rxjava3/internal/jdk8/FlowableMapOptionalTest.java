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

package io.reactivex.rxjava3.internal.jdk8;

import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableMapOptionalTest extends RxJavaTest {

    static final Function<? super Integer, Optional<? extends Integer>> MODULO = v -> v % 2 == 0 ? Optional.of(v) : Optional.<Integer>empty();

    @Test
    public void allPresent() {
        Flowable.range(1, 5)
        .mapOptional(Optional::of)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void allEmpty() {
        Flowable.range(1, 5)
        .mapOptional(v -> Optional.<Integer>empty())
        .test()
        .assertResult();
    }

    @Test
    public void mixed() {
        Flowable.range(1, 10)
        .mapOptional(MODULO)
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void mapperChash() {
        BehaviorProcessor<Integer> source = BehaviorProcessor.createDefault(1);

        source
        .mapOptional(v -> { throw new TestException(); })
        .test()
        .assertFailure(TestException.class);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void mapperNull() {
        BehaviorProcessor<Integer> source = BehaviorProcessor.createDefault(1);

        source
        .mapOptional(v -> null)
        .test()
        .assertFailure(NullPointerException.class);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void crashDropsOnNexts() {
        Flowable<Integer> source = new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
            }
        };

        source
        .mapOptional(v -> { throw new TestException(); })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void backpressureAll() {
        Flowable.range(1, 5)
        .mapOptional(Optional::of)
        .test(0L)
        .assertEmpty()
        .requestMore(2)
        .assertValuesOnly(1, 2)
        .requestMore(2)
        .assertValuesOnly(1, 2, 3, 4)
        .requestMore(1)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void backpressureNone() {
        Flowable.range(1, 5)
        .mapOptional(v -> Optional.empty())
        .test(1L)
        .assertResult();
    }

    @Test
    public void backpressureMixed() {
        Flowable.range(1, 10)
        .mapOptional(MODULO)
        .test(0L)
        .assertEmpty()
        .requestMore(2)
        .assertValuesOnly(2, 4)
        .requestMore(2)
        .assertValuesOnly(2, 4, 6, 8)
        .requestMore(1)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void syncFusedAll() {
        Flowable.range(1, 5)
        .mapOptional(Optional::of)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void asyncFusedAll() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .mapOptional(Optional::of)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void boundaryFusedAll() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .mapOptional(Optional::of)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void syncFusedNone() {
        Flowable.range(1, 5)
        .mapOptional(v -> Optional.empty())
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult();
    }

    @Test
    public void asyncFusedNone() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .mapOptional(v -> Optional.empty())
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void boundaryFusedNone() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .mapOptional(v -> Optional.empty())
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult();
    }

    @Test
    public void syncFusedMixed() {
        Flowable.range(1, 10)
        .mapOptional(MODULO)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void asyncFusedMixed() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        up
        .mapOptional(MODULO)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void boundaryFusedMixed() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        up
        .mapOptional(MODULO)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void allPresentConditional() {
        Flowable.range(1, 5)
        .mapOptional(Optional::of)
        .filter(v -> true)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void allEmptyConditional() {
        Flowable.range(1, 5)
        .mapOptional(v -> Optional.<Integer>empty())
        .filter(v -> true)
        .test()
        .assertResult();
    }

    @Test
    public void mixedConditional() {
        Flowable.range(1, 10)
        .mapOptional(MODULO)
        .filter(v -> true)
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void mapperChashConditional() {
        BehaviorProcessor<Integer> source = BehaviorProcessor.createDefault(1);

        source
        .mapOptional(v -> { throw new TestException(); })
        .filter(v -> true)
        .test()
        .assertFailure(TestException.class);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void mapperNullConditional() {
        BehaviorProcessor<Integer> source = BehaviorProcessor.createDefault(1);

        source
        .mapOptional(v -> null)
        .filter(v -> true)
        .test()
        .assertFailure(NullPointerException.class);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void crashDropsOnNextsConditional() {
        Flowable<Integer> source = new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
            }
        };

        source
        .mapOptional(v -> { throw new TestException(); })
        .filter(v -> true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void backpressureAllConditional() {
        Flowable.range(1, 5)
        .mapOptional(Optional::of)
        .filter(v -> true)
        .test(0L)
        .assertEmpty()
        .requestMore(2)
        .assertValuesOnly(1, 2)
        .requestMore(2)
        .assertValuesOnly(1, 2, 3, 4)
        .requestMore(1)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void backpressureNoneConditional() {
        Flowable.range(1, 5)
        .mapOptional(v -> Optional.empty())
        .filter(v -> true)
        .test(1L)
        .assertResult();
    }

    @Test
    public void backpressureMixedConditional() {
        Flowable.range(1, 10)
        .mapOptional(MODULO)
        .filter(v -> true)
        .test(0L)
        .assertEmpty()
        .requestMore(2)
        .assertValuesOnly(2, 4)
        .requestMore(2)
        .assertValuesOnly(2, 4, 6, 8)
        .requestMore(1)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void syncFusedAllConditional() {
        Flowable.range(1, 5)
        .mapOptional(Optional::of)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void asyncFusedAllConditional() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .mapOptional(Optional::of)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void boundaryFusedAllConditiona() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .mapOptional(Optional::of)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void syncFusedNoneConditional() {
        Flowable.range(1, 5)
        .mapOptional(v -> Optional.empty())
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult();
    }

    @Test
    public void asyncFusedNoneConditional() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .mapOptional(v -> Optional.empty())
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void boundaryFusedNoneConditional() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .mapOptional(v -> Optional.empty())
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult();
    }

    @Test
    public void syncFusedMixedConditional() {
        Flowable.range(1, 10)
        .mapOptional(MODULO)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void asyncFusedMixedConditional() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        up
        .mapOptional(MODULO)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void boundaryFusedMixedConditional() {
        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        up
        .mapOptional(MODULO)
        .filter(v -> true)
        .to(TestHelper.testConsumer(false, QueueFuseable.ASYNC | QueueFuseable.BOUNDARY))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(2, 4, 6, 8, 10);
    }
}
