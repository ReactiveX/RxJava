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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class ParallelFlowableTest {

    @Test
    public void sequentialMode() {
        Flowable<Integer> source = Flowable.range(1, 1000000).hide();
        for (int i = 1; i < 33; i++) {
            Flowable<Integer> result = ParallelFlowable.from(source, i)
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {
                    return v + 1;
                }
            })
            .sequential()
            ;

            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            result.subscribe(ts);

            ts
            .assertSubscribed()
            .assertValueCount(1000000)
            .assertComplete()
            .assertNoErrors()
            ;
        }

    }

    @Test
    public void sequentialModeFused() {
        Flowable<Integer> source = Flowable.range(1, 1000000);
        for (int i = 1; i < 33; i++) {
            Flowable<Integer> result = ParallelFlowable.from(source, i)
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {
                    return v + 1;
                }
            })
            .sequential()
            ;

            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            result.subscribe(ts);

            ts
            .assertSubscribed()
            .assertValueCount(1000000)
            .assertComplete()
            .assertNoErrors()
            ;
        }

    }

    @Test
    public void parallelMode() {
        Flowable<Integer> source = Flowable.range(1, 1000000).hide();
        int ncpu = Math.max(8, Runtime.getRuntime().availableProcessors());
        for (int i = 1; i < ncpu + 1; i++) {

            ExecutorService exec = Executors.newFixedThreadPool(i);

            Scheduler scheduler = Schedulers.from(exec);

            try {
                Flowable<Integer> result = ParallelFlowable.from(source, i)
                .runOn(scheduler)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        return v + 1;
                    }
                })
                .sequential()
                ;

                TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

                result.subscribe(ts);

                ts.awaitDone(10, TimeUnit.SECONDS);

                ts
                .assertSubscribed()
                .assertValueCount(1000000)
                .assertComplete()
                .assertNoErrors()
                ;
            } finally {
                exec.shutdown();
            }
        }

    }

    @Test
    public void parallelModeFused() {
        Flowable<Integer> source = Flowable.range(1, 1000000);
        int ncpu = Math.max(8, Runtime.getRuntime().availableProcessors());
        for (int i = 1; i < ncpu + 1; i++) {

            ExecutorService exec = Executors.newFixedThreadPool(i);

            Scheduler scheduler = Schedulers.from(exec);

            try {
                Flowable<Integer> result = ParallelFlowable.from(source, i)
                .runOn(scheduler)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        return v + 1;
                    }
                })
                .sequential()
                ;

                TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

                result.subscribe(ts);

                ts.awaitDone(10, TimeUnit.SECONDS);

                ts
                .assertSubscribed()
                .assertValueCount(1000000)
                .assertComplete()
                .assertNoErrors()
                ;
            } finally {
                exec.shutdown();
            }
        }

    }

    @Test
    public void reduceFull() {
        for (int i = 1; i <= Runtime.getRuntime().availableProcessors() * 2; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            Flowable.range(1, 10)
            .parallel(i)
            .reduce(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Exception {
                    return a + b;
                }
            })
            .subscribe(ts);

            ts.assertResult(55);
        }
    }

    @Test
    public void parallelReduceFull() {
        int m = 100000;
        for (int n = 1; n <= m; n *= 10) {
//            System.out.println(n);
            for (int i = 1; i <= Runtime.getRuntime().availableProcessors(); i++) {
//                System.out.println("  " + i);

                ExecutorService exec = Executors.newFixedThreadPool(i);

                Scheduler scheduler = Schedulers.from(exec);

                try {
                    TestSubscriber<Long> ts = new TestSubscriber<Long>();

                    Flowable.range(1, n)
                    .map(new Function<Integer, Long>() {
                        @Override
                        public Long apply(Integer v) throws Exception {
                            return (long)v;
                        }
                    })
                    .parallel(i)
                    .runOn(scheduler)
                    .reduce(new BiFunction<Long, Long, Long>() {
                        @Override
                        public Long apply(Long a, Long b) throws Exception {
                            return a + b;
                        }
                    })
                    .subscribe(ts);

                    ts.awaitDone(500, TimeUnit.SECONDS);

                    long e = ((long)n) * (1 + n) / 2;

                    ts.assertResult(e);
                } finally {
                    exec.shutdown();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toSortedList() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        Flowable.fromArray(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
        .parallel()
        .toSortedList(Functions.naturalComparator())
        .subscribe(ts);

        ts.assertResult(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void sorted() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);

        Flowable.fromArray(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
        .parallel()
        .sorted(Functions.naturalComparator())
        .subscribe(ts);

        ts.assertNoValues();

        ts.request(2);

        ts.assertValues(1, 2);

        ts.request(5);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7);

        ts.request(3);

        ts.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void collect() {
        Callable<List<Integer>> as = new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                return new ArrayList<Integer>();
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, 10)
        .parallel()
        .collect(as, new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> a, Integer b) throws Exception {
                a.add(b);
            }
        })
        .sequential()
        .flatMapIterable(new Function<List<Integer>, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(List<Integer> v) throws Exception {
                return v;
            }
        })
        .subscribe(ts);

        ts.assertValueSet(new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void from() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        ParallelFlowable.fromArray(Flowable.range(1, 5), Flowable.range(6, 5))
        .sequential()
        .subscribe(ts);

        ts.assertValueSet(new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void concatMapUnordered() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5)
        .parallel()
        .concatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(v * 10 + 1, 3);
            }
        })
        .sequential()
        .subscribe(ts);

        ts.assertValueSet(new HashSet<Integer>(Arrays.asList(11, 12, 13, 21, 22, 23, 31, 32, 33, 41, 42, 43, 51, 52, 53)))
        .assertNoErrors()
        .assertComplete();

    }

    @Test
    public void flatMapUnordered() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5)
        .parallel()
        .flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(v * 10 + 1, 3);
            }
        })
        .sequential()
        .subscribe(ts);

        ts.assertValueSet(new HashSet<Integer>(Arrays.asList(11, 12, 13, 21, 22, 23, 31, 32, 33, 41, 42, 43, 51, 52, 53)))
        .assertNoErrors()
        .assertComplete();

    }

    @Test
    public void collectAsyncFused() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000)
            .parallel(3)
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void collectAsync() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000).hide()
            .parallel(3)
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }


    @Test
    public void collectAsync2() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000).hide()
            .observeOn(s)
            .parallel(3)
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void collectAsync3() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000).hide()
            .observeOn(s)
            .parallel(3)
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }


    @Test
    public void collectAsync3Fused() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000)
            .observeOn(s)
            .parallel(3)
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void collectAsync3Take() {
        ExecutorService exec = Executors.newFixedThreadPool(4);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000)
            .take(1000)
            .observeOn(s)
            .parallel(3)
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(1000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void collectAsync4Take() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            UnicastProcessor<Integer> up = UnicastProcessor.create();

            for (int i = 0; i < 1000; i++) {
                up.onNext(i);
            }

            up
            .take(1000)
            .observeOn(s)
            .parallel(3)
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(1000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void emptySourceZeroRequest() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>(0);

        Flowable.range(1, 3).parallel(3).sequential().subscribe(ts);

        ts.request(1);

        ts.assertValue(1);
    }

    @Test
    public void parallelismAndPrefetch() {
        for (int parallelism = 1; parallelism <= 8; parallelism++) {
            for (int prefetch = 1; prefetch <= 1024; prefetch *= 2) {
                Flowable.range(1, 1024 * 1024)
                .parallel(parallelism, prefetch)
                .map(Functions.<Integer>identity())
                .sequential()
                .test()
                .assertSubscribed()
                .assertValueCount(1024 * 1024)
                .assertNoErrors()
                .assertComplete();
            }
        }
    }

    @Test
    public void parallelismAndPrefetchAsync() {
        for (int parallelism = 1; parallelism <= 8; parallelism *= 2) {
            for (int prefetch = 1; prefetch <= 1024; prefetch *= 2) {
                System.out.println("parallelismAndPrefetchAsync >> " + parallelism + ", " + prefetch);

                Flowable.range(1, 1024 * 1024)
                .parallel(parallelism, prefetch)
                .runOn(Schedulers.computation())
                .map(Functions.<Integer>identity())
                .sequential(prefetch)
                .test()
                .withTag("parallelism = " + parallelism + ", prefetch = " + prefetch)
                .awaitDone(30, TimeUnit.SECONDS)
                .assertSubscribed()
                .assertValueCount(1024 * 1024)
                .assertNoErrors()
                .assertComplete();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void badParallelismStage() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 10)
        .parallel(2)
        .subscribe(new Subscriber[] { ts });

        ts.assertFailure(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void badParallelismStage2() {
        TestSubscriber<Integer> ts1 = new TestSubscriber<Integer>();
        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();
        TestSubscriber<Integer> ts3 = new TestSubscriber<Integer>();

        Flowable.range(1, 10)
        .parallel(2)
        .subscribe(new Subscriber[] { ts1, ts2, ts3 });

        ts1.assertFailure(IllegalArgumentException.class);
        ts2.assertFailure(IllegalArgumentException.class);
        ts3.assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void filter() {
        Flowable.range(1, 20)
        .parallel()
        .runOn(Schedulers.computation())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueSet(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20))
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void filterThrows() throws Exception {
        final boolean[] cancelled = { false };
        Flowable.range(1, 20).concatWith(Flowable.<Integer>never())
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                cancelled[0] = true;
            }
        })
        .parallel()
        .runOn(Schedulers.computation())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                if (v == 10) {
                    throw new TestException();
                }
                return v % 2 == 0;
            }
        })
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertError(TestException.class)
        .assertNotComplete();

        Thread.sleep(100);

        assertTrue(cancelled[0]);
    }

    @Test
    public void doAfterNext() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel()
        .doAfterNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                count[0]++;
            }
        })
        .sequential()
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void doOnNextThrows() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel()
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (v == 3) {
                    throw new TestException();
                } else {
                    count[0]++;
                }
            }
        })
        .sequential()
        .test()
        .assertError(TestException.class)
        .assertNotComplete();

        assertTrue("" + count[0], count[0] < 5);
    }

    @Test
    public void doAfterNextThrows() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel()
        .doAfterNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (v == 3) {
                    throw new TestException();
                } else {
                    count[0]++;
                }
            }
        })
        .sequential()
        .test()
        .assertError(TestException.class)
        .assertNotComplete();

        assertTrue("" + count[0], count[0] < 5);
    }

    @Test
    public void errorNotRepeating() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.error(new TestException())
            .parallel()
            .runOn(Schedulers.computation())
            .sequential()
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class)
            ;

            Thread.sleep(300);

            for (Throwable ex : errors) {
                ex.printStackTrace();
            }
            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doOnError() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                if (v == 3) {
                    throw new TestException();
                }
                return v;
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                if (e instanceof TestException) {
                    count[0]++;
                }
            }
        })
        .sequential()
        .test()
        .assertError(TestException.class)
        .assertNotComplete();

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnErrorThrows() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .parallel(2)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                if (v == 3) {
                    throw new TestException();
                }
                return v;
            }
        })
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                if (e instanceof TestException) {
                    throw new IOException();
                }
            }
        })
        .sequential()
        .test()
        .assertError(CompositeException.class)
        .assertNotComplete();

        List<Throwable> errors = TestHelper.errorList(ts);
        TestHelper.assertError(errors, 0, TestException.class);
        TestHelper.assertError(errors, 1, IOException.class);
    }

    @Test
    public void doOnComplete() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                count[0]++;
            }
        })
        .sequential()
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(2, count[0]);
    }

    @Test
    public void doAfterTerminate() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .doAfterTerminated(new Action() {
            @Override
            public void run() throws Exception {
                count[0]++;
            }
        })
        .sequential()
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(2, count[0]);
    }

    @Test
    public void doOnSubscribe() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                count[0]++;
            }
        })
        .sequential()
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(2, count[0]);
    }

    @Test
    public void doOnRequest() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .doOnRequest(new LongConsumer() {
            @Override
            public void accept(long s) throws Exception {
                count[0]++;
            }
        })
        .sequential()
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(2, count[0]);
    }

    @Test
    public void doOnCancel() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                count[0]++;
            }
        })
        .sequential()
        .take(2)
        .test()
        .assertResult(1, 2);

        assertEquals(2, count[0]);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void fromPublishers() {
        ParallelFlowable.fromArray(new Publisher[0]);
    }

    @Test
    public void to() {
        Flowable.range(1, 5)
        .parallel()
        .to(new Function<ParallelFlowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(ParallelFlowable<Integer> pf) throws Exception {
                return pf.sequential();
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test(expected = TestException.class)
    public void toThrows() {
        Flowable.range(1, 5)
        .parallel()
        .to(new Function<ParallelFlowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(ParallelFlowable<Integer> pf) throws Exception {
                throw new TestException();
            }
        });
    }

    @Test
    public void compose() {
        Flowable.range(1, 5)
        .parallel()
        .compose(new ParallelTransformer<Integer, Integer>() {
            @Override
            public ParallelFlowable<Integer> apply(ParallelFlowable<Integer> pf) {
                return pf.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        return v + 1;
                    }
                });
            }
        })
        .sequential()
        .test()
        .assertResult(2, 3, 4, 5, 6);
    }

    @Test
    public void flatMapDelayError() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .flatMap(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                if (v == 3) {
                   return Flowable.error(new TestException());
                }
                return Flowable.just(v);
            }
        }, true)
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                if (e instanceof TestException) {
                    count[0]++;
                }
            }
        })
        .sequential()
        .test()
        .assertValues(1, 2, 4, 5)
        .assertError(TestException.class)
        .assertNotComplete();

        assertEquals(1, count[0]);
    }

    @Test
    public void flatMapDelayErrorMaxConcurrency() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .flatMap(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                if (v == 3) {
                   return Flowable.error(new TestException());
                }
                return Flowable.just(v);
            }
        }, true, 1)
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                if (e instanceof TestException) {
                    count[0]++;
                }
            }
        })
        .sequential()
        .test()
        .assertValues(1, 2, 4, 5)
        .assertError(TestException.class)
        .assertNotComplete();

        assertEquals(1, count[0]);
    }

    @Test
    public void concatMapDelayError() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                if (v == 3) {
                   return Flowable.error(new TestException());
                }
                return Flowable.just(v);
            }
        }, true)
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                if (e instanceof TestException) {
                    count[0]++;
                }
            }
        })
        .sequential()
        .test()
        .assertValues(1, 2, 4, 5)
        .assertError(TestException.class)
        .assertNotComplete();

        assertEquals(1, count[0]);
    }

    @Test
    public void concatMapDelayErrorPrefetch() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                if (v == 3) {
                   return Flowable.error(new TestException());
                }
                return Flowable.just(v);
            }
        }, 1, true)
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                if (e instanceof TestException) {
                    count[0]++;
                }
            }
        })
        .sequential()
        .test()
        .assertValues(1, 2, 4, 5)
        .assertError(TestException.class)
        .assertNotComplete();

        assertEquals(1, count[0]);
    }

    @Test
    public void concatMapDelayErrorBoundary() {
        final int[] count = { 0 };

        Flowable.range(1, 5)
        .parallel(2)
        .concatMapDelayError(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                if (v == 3) {
                   return Flowable.error(new TestException());
                }
                return Flowable.just(v);
            }
        }, false)
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                if (e instanceof TestException) {
                    count[0]++;
                }
            }
        })
        .sequential()
        .test()
        .assertValues(1, 2)
        .assertError(TestException.class)
        .assertNotComplete();

        assertEquals(1, count[0]);
    }

    public static void checkSubscriberCount(ParallelFlowable<?> source) {
        int n = source.parallelism();

        @SuppressWarnings("unchecked")
        TestSubscriber<Object>[] consumers = new TestSubscriber[n + 1];

        for (int i = 0; i <= n; i++) {
            consumers[i] = new TestSubscriber<Object>();
        }

        source.subscribe(consumers);

        for (int i = 0; i <= n; i++) {
            consumers[i].awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(IllegalArgumentException.class);
        }
    }

    @Test
    public void checkAddBiConsumer() {
        TestHelper.checkEnum(ListAddBiConsumer.class);
    }

    @Test
    public void mergeBiFunction() throws Exception {
        MergerBiFunction<Integer> f = new MergerBiFunction<Integer>(Functions.<Integer>naturalComparator());

        assertEquals(0, f.apply(Collections.<Integer>emptyList(), Collections.<Integer>emptyList()).size());

        assertEquals(Arrays.asList(1, 2), f.apply(Collections.<Integer>emptyList(), Arrays.asList(1, 2)));

        for (int i = 0; i < 4; i++) {
            int k = 0;
            List<Integer> list1 = new ArrayList<Integer>();
            for (int j = 0; j < i; j++) {
                list1.add(k++);
            }

            List<Integer> list2 = new ArrayList<Integer>();
            for (int j = i; j < 4; j++) {
                list2.add(k++);
            }

            assertEquals(Arrays.asList(0, 1, 2, 3), f.apply(list1, list2));
        }
    }

    @Test
    public void concatMapSubscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .concatMap(Functions.justFunction(Flowable.just(1))));
    }

    @Test
    public void flatMapSubscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .flatMap(Functions.justFunction(Flowable.just(1))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fromArraySubscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(ParallelFlowable.fromArray(new Publisher[] { Flowable.just(1) }));
    }
}
