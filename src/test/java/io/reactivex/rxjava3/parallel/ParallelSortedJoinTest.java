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

package io.reactivex.rxjava3.parallel;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.parallel.ParallelSortedJoin;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ParallelSortedJoinTest extends RxJavaTest {

    @Test
    public void cancel() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .parallel()
        .sorted(Functions.<Integer>naturalComparator())
        .test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void error() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.<Integer>error(new TestException())
            .parallel()
            .sorted(Functions.<Integer>naturalComparator())
            .test()
            .assertFailure(TestException.class);

            assertTrue(errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void error3() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Flowable.<Integer>error(new TestException())
            .parallel()
            .sorted(Functions.<Integer>naturalComparator())
            .test(0)
            .assertFailure(TestException.class);

            assertTrue(errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void error2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            ParallelFlowable.fromArray(Flowable.<Integer>error(new IOException()), Flowable.<Integer>error(new TestException()))
            .sorted(Functions.<Integer>naturalComparator())
            .test()
            .assertFailure(IOException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void comparerCrash() {
        Flowable.fromArray(4, 3, 2, 1)
        .parallel(2)
        .sorted(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (o1 == 4 && o2 == 3) {
                    throw new TestException();
                }
                return o1.compareTo(o2);
            }
        })
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .parallel()
        .sorted(Functions.<Integer>naturalComparator())
        .test()
        .assertResult();
    }

    @Test
    public void asyncDrain() {
        Integer[] values = new Integer[100 * 1000];
        for (int i = 0; i < values.length; i++) {
            values[i] = values.length - i;
        }

        TestSubscriber<Integer> ts = Flowable.fromArray(values)
        .parallel(2)
        .runOn(Schedulers.computation())
        .sorted(Functions.naturalComparator())
        .observeOn(Schedulers.single())
        .test();

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(values.length)
        .assertNoErrors()
        .assertComplete();

        List<Integer> list = ts.values();
        for (int i = 0; i < values.length; i++) {
            assertEquals(i + 1, list.get(i).intValue());
        }
    }

    @Test
    public void sortCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ReplayProcessor<Integer> pp = ReplayProcessor.create();
            pp.onNext(1);
            pp.onNext(2);

            final TestSubscriber<Integer> ts = pp.parallel(2)
            .sorted(Functions.naturalComparator())
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void sortCancelRace2() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final ReplayProcessor<Integer> pp = ReplayProcessor.create();
            pp.onNext(1);
            pp.onNext(2);

            final TestSubscriber<Integer> ts = pp.parallel(2)
            .sorted(Functions.naturalComparator())
            .test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(PublishProcessor.<Integer>create().parallel().sorted(Functions.naturalComparator()));
    }

    @Test
    public void comparatorCrashWhileMainOnError() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            PublishProcessor<List<Integer>> pp1 = PublishProcessor.create();
            PublishProcessor<List<Integer>> pp2 = PublishProcessor.create();

            new ParallelSortedJoin<>(ParallelFlowable.fromArray(pp1, pp2)
            , (a, b) -> {
                pp1.onError(new IOException());
                throw new TestException();
            })
            .test();

            pp1.onNext(Arrays.asList(1));
            pp2.onNext(Arrays.asList(2));

            pp1.onComplete();
            pp2.onComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }
}
