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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableGenerateTest extends RxJavaTest {

    @Test
    public void statefulBiconsumer() {
        Flowable.generate((Supplier<Object>) () -> 10, (s, e) -> {
            e.onNext(s);
        }, d -> {

        })
        .take(5)
        .test()
        .assertResult(10, 10, 10, 10, 10);
    }

    @Test
    public void stateSupplierThrows() {
        Flowable.generate(() -> {
            throw new TestException();
        }, (s, e) -> {
            e.onNext(s);
        }, Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void generatorThrows() {
        Flowable.generate(() -> 1, (BiConsumer<Object, Emitter<Object>>) (s, e) -> {
            throw new TestException();
        }, Functions.emptyConsumer())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void disposerThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.generate((Supplier<Object>) () -> 1, (s, e) -> {
                e.onComplete();
            }, d -> {
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
        TestHelper.checkDisposed(Flowable.generate((Supplier<Object>) () -> 1, (s, e) -> {
            e.onComplete();
        }, Functions.emptyConsumer()));
    }

    @Test
    public void nullError() {
        final int[] call = { 0 };
        Flowable.generate(Functions.justSupplier(1),
                (s, e) -> {
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
        TestHelper.assertBadRequestReported(Flowable.generate((Supplier<Object>) () -> 1, (s, e) -> {
            e.onComplete();
        }, Functions.emptyConsumer()));
    }

    @Test
    public void rebatchAndTake() {
        Flowable.generate((Supplier<Object>) () -> 1, (s, e) -> {
            e.onNext(1);
        }, Functions.emptyConsumer())
        .rebatchRequests(1)
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void backpressure() {
        Flowable.generate((Supplier<Object>) () -> 1, (s, e) -> {
            e.onNext(1);
        }, Functions.emptyConsumer())
        .rebatchRequests(1)
        .to(TestHelper.<Object>testSubscriber(5L))
        .assertSubscribed()
        .assertValues(1, 1, 1, 1, 1)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void requestRace() {
        Flowable<Object> source = Flowable.generate((Supplier<Object>) () -> 1, (s, e) -> {
            e.onNext(1);
        }, Functions.emptyConsumer());

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
