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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.parallel.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class ParallelMapTryOptionalTest extends RxJavaTest implements Consumer<Object> {

    volatile int calls;

    @Override
    public void accept(Object t) throws Exception {
        calls++;
    }

    @Test
    public void mapNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .mapOptional(Optional::of, e)
            .sequential()
            .test()
            .assertResult(1);
        }
    }

    @Test
    public void mapErrorNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .mapOptional(Optional::of, e)
            .sequential()
            .test()
            .assertFailure(TestException.class);
        }
    }

    @Test
    public void mapConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .mapOptional(Optional::of, e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertResult(1);
        }
    }

    @Test
    public void mapErrorConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .mapOptional(Optional::of, e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertFailure(TestException.class);
        }
    }

    @Test
    public void mapFailWithError() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), ParallelFailureHandling.ERROR)
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void mapFailWithStop() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), ParallelFailureHandling.STOP)
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void mapFailWithRetry() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(new Function<Integer, Optional<? extends Integer>>() {
            int count;
            @Override
            public Optional<? extends Integer> apply(Integer v) throws Exception {
                if (count++ == 1) {
                    return Optional.of(-1);
                }
                return Optional.of(1 / v);
            }
        }, ParallelFailureHandling.RETRY)
        .sequential()
        .test()
        .assertResult(-1, 1);
    }

    @Test
    public void mapFailWithRetryLimited() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), new BiFunction<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling apply(Long n, Throwable e) throws Exception {
                return n < 5 ? ParallelFailureHandling.RETRY : ParallelFailureHandling.SKIP;
            }
        })
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void mapFailWithSkip() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), ParallelFailureHandling.SKIP)
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void mapFailHandlerThrows() {
        TestSubscriberEx<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), new BiFunction<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling apply(Long n, Throwable e) throws Exception {
                throw new TestException();
            }
        })
        .sequential()
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts, ArithmeticException.class, TestException.class);
    }

    @Test
    public void mapInvalidSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .mapOptional(Optional::of, ParallelFailureHandling.ERROR)
            .sequential()
            .test();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void mapFailWithErrorConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), ParallelFailureHandling.ERROR)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void mapFailWithStopConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), ParallelFailureHandling.STOP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void mapFailWithRetryConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(new Function<Integer, Optional<? extends Integer>>() {
            int count;
            @Override
            public Optional<? extends Integer> apply(Integer v) throws Exception {
                if (count++ == 1) {
                    return Optional.of(-1);
                }
                return Optional.of(1 / v);
            }
        }, ParallelFailureHandling.RETRY)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(-1, 1);
    }

    @Test
    public void mapFailWithRetryLimitedConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), new BiFunction<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling apply(Long n, Throwable e) throws Exception {
                return n < 5 ? ParallelFailureHandling.RETRY : ParallelFailureHandling.SKIP;
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void mapFailWithSkipConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), ParallelFailureHandling.SKIP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(1);
    }

    @Test
    public void mapFailHandlerThrowsConditional() {
        TestSubscriberEx<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
        .mapOptional(v -> Optional.of(1 / v), new BiFunction<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling apply(Long n, Throwable e) throws Exception {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts, ArithmeticException.class, TestException.class);
    }

    @Test
    public void mapWrongParallelismConditional() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .mapOptional(Optional::of, ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
        );
    }

    @Test
    public void mapInvalidSourceConditional() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .mapOptional(Optional::of, ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void failureHandlingEnum() {
        TestHelper.checkEnum(ParallelFailureHandling.class);
    }

    @Test
    public void allNone() {
        Flowable.range(1, 1000)
        .parallel()
        .mapOptional(v -> Optional.empty(), ParallelFailureHandling.SKIP)
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void allNoneConditional() {
        Flowable.range(1, 1000)
        .parallel()
        .mapOptional(v -> Optional.empty(), ParallelFailureHandling.SKIP)
        .filter(v -> true)
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void mixed() {
        Flowable.range(1, 1000)
        .parallel()
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v) : Optional.empty(), ParallelFailureHandling.SKIP)
        .sequential()
        .test()
        .assertValueCount(500)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void mixedConditional() {
        Flowable.range(1, 1000)
        .parallel()
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v) : Optional.empty(), ParallelFailureHandling.SKIP)
        .filter(v -> true)
        .sequential()
        .test()
        .assertValueCount(500)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void mixedConditional2() {
        Flowable.range(1, 1000)
        .parallel()
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v) : Optional.empty(), ParallelFailureHandling.SKIP)
        .filter(v -> v % 4 == 0)
        .sequential()
        .test()
        .assertValueCount(250)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void invalidSubscriberCount() {
        TestHelper.checkInvalidParallelSubscribers(
                Flowable.range(1, 10).parallel()
                .mapOptional(Optional::of, ParallelFailureHandling.SKIP)
            );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeParallel(
                p -> p.mapOptional(Optional::of, ParallelFailureHandling.ERROR)
            );

        TestHelper.checkDoubleOnSubscribeParallel(
                p -> p.mapOptional(Optional::of, ParallelFailureHandling.ERROR)
                .filter(v -> true)
            );
    }
}
