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

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.parallel.ParallelInvalid;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class ParallelCollectorTest extends RxJavaTest {

    static Set<Integer> set(int count) {
        return IntStream.rangeClosed(1, count)
                .boxed()
                .collect(Collectors.toSet());
    }

    @Test
    public void basic() {
        TestSubscriberEx<List<Integer>> ts = Flowable.range(1, 5)
        .parallel()
        .collect(Collectors.toList())
        .subscribeWith(new TestSubscriberEx<>());

        ts
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        assertEquals(5, ts.values().get(0).size());
        assertTrue(ts.values().get(0).containsAll(set(5)));
    }

    @Test
    public void empty() {
        Flowable.empty()
        .parallel()
        .collect(Collectors.toList())
        .test()
        .assertResult(Collections.emptyList());
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .parallel()
        .collect(Collectors.toList())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorSupplierCrash() {
        Flowable.range(1, 5)
        .parallel()
        .collect(new Collector<Integer, Integer, Integer>() {

            @Override
            public Supplier<Integer> supplier() {
                throw new TestException();
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (a, b) -> { };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return (a, b) -> a + b;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return a -> a;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorAccumulatorCrash() {
        BehaviorProcessor<Integer> source = BehaviorProcessor.createDefault(1);

        source
        .parallel()
        .collect(new Collector<Integer, Integer, Integer>() {

            @Override
            public Supplier<Integer> supplier() {
                return () -> 1;
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (a, b) -> { throw new TestException(); };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return (a, b) -> a + b;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return a -> a;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertFalse(source.hasSubscribers());
    }

    @Test
    @SuppressUndeliverable
    public void collectorCombinerCrash() {
        Flowable.range(1, 5)
        .parallel()
        .collect(new Collector<Integer, Integer, Integer>() {

            @Override
            public Supplier<Integer> supplier() {
                return () -> 1;
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (a, b) -> {  };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return (a, b) -> { throw new TestException(); };
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return a -> a;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectorFinisherCrash() {
        Flowable.range(1, 5)
        .parallel()
        .collect(new Collector<Integer, Integer, Integer>() {

            @Override
            public Supplier<Integer> supplier() {
                return () -> 1;
            }

            @Override
            public BiConsumer<Integer, Integer> accumulator() {
                return (a, b) -> {  };
            }

            @Override
            public BinaryOperator<Integer> combiner() {
                return (a, b) -> a + b;
            }

            @Override
            public Function<Integer, Integer> finisher() {
                return a -> { throw new TestException(); };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void async() {
        for (int i = 1; i < 32; i++) {
            TestSubscriber<List<Integer>> ts = Flowable.range(1, 1000)
            .parallel(i)
            .runOn(Schedulers.computation())
            .collect(Collectors.toList())
            .test()
            .withTag("Parallelism: " + i)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(1)
            .assertNoErrors()
            .assertComplete();

            assertEquals(1000, ts.values().get(0).size());

            assertTrue(ts.values().get(0).containsAll(set(1000)));
        }
    }

    @Test
    public void asyncHidden() {
        for (int i = 1; i < 32; i++) {
            TestSubscriber<List<Integer>> ts = Flowable.range(1, 1000)
            .hide()
            .parallel(i)
            .runOn(Schedulers.computation())
            .collect(Collectors.toList())
            .test()
            .withTag("Parallelism: " + i)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(1)
            .assertNoErrors()
            .assertComplete();

            assertEquals(1000, ts.values().get(0).size());

            assertTrue(ts.values().get(0).containsAll(set(1000)));
        }
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .collect(Collectors.toList())
            .test()
            .assertFailure(TestException.class);

            assertFalse(errors.isEmpty());
            for (Throwable ex : errors) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void asyncSum() {
        long n = 1_000;
        for (int i = 1; i < 32; i++) {
            Flowable.rangeLong(1, n)
            .parallel(i)
            .runOn(Schedulers.computation())
            .collect(Collectors.summingLong(v -> v))
            .test()
            .withTag("Parallelism: " + i)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(n * (n + 1) / 2);
        }
    }

    @Test
    public void asyncSumLong() {
        long n = 1_000_000;
        Flowable.rangeLong(1, n)
        .parallel()
        .runOn(Schedulers.computation())
        .collect(Collectors.summingLong(v -> v))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(n * (n + 1) / 2);
    }
}
