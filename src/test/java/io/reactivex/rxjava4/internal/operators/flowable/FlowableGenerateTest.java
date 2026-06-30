/*
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

package io.reactivex.rxjava4.internal.operators.flowable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.BiConsumer;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class FlowableGenerateTest extends RxJavaTest {

    @Test
    public void statefulBiconsumer() {
        Flowable.generate(() -> 10,
                (BiConsumer<Object, Emitter<Object>>) (s, e) -> e.onNext(s),
                        _ -> { })
        .take(5)
        .test()
        .assertResult(10, 10, 10, 10, 10);
    }

    @Test
    public void stateSupplierThrows() {
        Flowable.generate(() -> {
            throw new TestException();
        }, (BiConsumer<Object, Emitter<Object>>) (s, e) -> e.onNext(s), Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void generatorThrows() {
        Flowable.generate(() -> 1, (BiConsumer<Object, Emitter<Object>>) (_, _) -> {
            throw new TestException();
        }, Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void disposerThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.generate(() -> 1,
                    (BiConsumer<Object, Emitter<Object>>) (_, e) -> e.onComplete(),
                            _ -> {
                                throw new TestException();
                            })
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.generate(() -> 1,
                (BiConsumer<Object, Emitter<Object>>) (_, e) -> e.onComplete(), Functions.emptyConsumer()));
    }

    @Test
    public void nullError() {
        final int[] call = { 0 };
        Flowable.generate(Functions.justSupplier(1),
                        (_, e) -> {
                            try {
                                e.onError(null);
                            } catch (NullPointerException ex) {
                                call[0]++;
                            }
                        }, Functions.emptyConsumer())
        .test()
        .assertFailure(NullPointerException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.generate(() -> 1,
                (BiConsumer<Object, Emitter<Object>>) (_, e) -> e.onComplete(), Functions.emptyConsumer()));
    }

    @Test
    public void rebatchAndTake() {
        Flowable.generate(() -> 1,
                (BiConsumer<Object, Emitter<Object>>) (_, e) -> e.onNext(1), Functions.emptyConsumer())
        .rebatchRequests(1)
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void backpressure() {
        Flowable.generate(() -> 1,
                (BiConsumer<Object, Emitter<Object>>) (_, e) -> e.onNext(1), Functions.emptyConsumer())
        .rebatchRequests(1)
        .to(TestHelper.<Object>testSubscriber(5L))
        .assertSubscribed()
        .assertValues(1, 1, 1, 1, 1)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void requestRace() {
        Flowable<Object> source = Flowable.generate(() -> 1,
                (BiConsumer<Object, Emitter<Object>>) (_, e) -> e.onNext(1), Functions.emptyConsumer());

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Object> ts = source.test(0L);

            Runnable r = () -> {
                for (int j = 0; j < 500; j++) {
                    ts.request(1);
                }
            };

            TestHelper.race(r, r);

            ts.assertValueCount(1000);
        }
    }

    @Test
    public void multipleOnNext() {
        Flowable.generate(e -> {
            e.onNext(1);
            e.onNext(2);
        })
        .test(1)
        .assertFailure(IllegalStateException.class, 1);
    }

    @Test
    public void multipleOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.generate(e -> {
                e.onError(new TestException("First"));
                e.onError(new TestException("Second"));
            })
            .test(1)
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void multipleOnComplete() {
        Flowable.generate(e -> {
            e.onComplete();
            e.onComplete();
        })
        .test(1)
        .assertResult();
    }

    @Test
    public void onNextAfterOnComplete() {
        Flowable.generate(e -> {
            e.onComplete();
            e.onNext(1);
        })
        .test()
        .assertResult();
    }
}
