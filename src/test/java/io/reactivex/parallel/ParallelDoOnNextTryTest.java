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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;

public class ParallelDoOnNextTryTest implements Consumer<Object> {

    volatile int calls;

    @Override
    public void accept(Object t) throws Exception {
        calls++;
    }

    @Test
    public void doOnNextNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .doOnNext(this, e)
            .sequential()
            .test()
            .assertResult(1);

            assertEquals(calls, 1);
            calls = 0;
        }
    }
    @Test
    public void doOnNextErrorNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .doOnNext(this, e)
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertEquals(calls, 0);
        }
    }

    @Test
    public void doOnNextConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.just(1)
            .parallel(1)
            .doOnNext(this, e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertResult(1);

            assertEquals(calls, 1);
            calls = 0;
        }
    }

    @Test
    public void doOnNextErrorConditionalNoError() {
        for (ParallelFailureHandling e : ParallelFailureHandling.values()) {
            Flowable.<Integer>error(new TestException())
            .parallel(1)
            .doOnNext(this, e)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test()
            .assertFailure(TestException.class);

            assertEquals(calls, 0);
        }
    }

    @Test
    public void doOnNextFailWithError() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, ParallelFailureHandling.ERROR)
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void doOnNextFailWithStop() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, ParallelFailureHandling.STOP)
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void doOnNextFailWithRetry() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            int count;
            @Override
            public void accept(Integer v) throws Exception {
                if (count++ == 1) {
                    return;
                }
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, ParallelFailureHandling.RETRY)
        .sequential()
        .test()
        .assertResult(0, 1);
    }

    @Test
    public void doOnNextFailWithRetryLimited() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, new BiFunction<Long, Throwable, ParallelFailureHandling>() {
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
    public void doOnNextFailWithSkip() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, ParallelFailureHandling.SKIP)
        .sequential()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doOnNextFailHandlerThrows() {
        TestSubscriber<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, new BiFunction<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling apply(Long n, Throwable e) throws Exception {
                throw new TestException();
            }
        })
        .sequential()
        .test()
        .assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts, ArithmeticException.class, TestException.class);
    }

    @Test
    public void doOnNextWrongParallelism() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .doOnNext(Functions.emptyConsumer(), ParallelFailureHandling.ERROR)
        );
    }

    @Test
    public void filterInvalidSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .doOnNext(Functions.emptyConsumer(), ParallelFailureHandling.ERROR)
            .sequential()
            .test();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doOnNextFailWithErrorConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, ParallelFailureHandling.ERROR)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(ArithmeticException.class);
    }

    @Test
    public void doOnNextFailWithStopConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, ParallelFailureHandling.STOP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void doOnNextFailWithRetryConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            int count;
            @Override
            public void accept(Integer v) throws Exception {
                if (count++ == 1) {
                    return;
                }
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, ParallelFailureHandling.RETRY)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(0, 1);
    }

    @Test
    public void doOnNextFailWithRetryLimitedConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, new BiFunction<Long, Throwable, ParallelFailureHandling>() {
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
    public void doOnNextFailWithSkipConditional() {
        Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, ParallelFailureHandling.SKIP)
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doOnNextFailHandlerThrowsConditional() {
        TestSubscriber<Integer> ts = Flowable.range(0, 2)
        .parallel(1)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (1 / v < 0) {
                    System.out.println("Should not happen!");
                }
            }
        }, new BiFunction<Long, Throwable, ParallelFailureHandling>() {
            @Override
            public ParallelFailureHandling apply(Long n, Throwable e) throws Exception {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts, ArithmeticException.class, TestException.class);
    }

    @Test
    public void doOnNextWrongParallelismConditional() {
        TestHelper.checkInvalidParallelSubscribers(
            Flowable.just(1).parallel(1)
            .doOnNext(Functions.emptyConsumer(), ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
        );
    }

    @Test
    public void filterInvalidSourceConditional() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .doOnNext(Functions.emptyConsumer(), ParallelFailureHandling.ERROR)
            .filter(Functions.alwaysTrue())
            .sequential()
            .test();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
