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

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.parallel.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ParallelMapOptionalTest extends RxJavaTest {

    @Test
    public void doubleFilter() {
        Flowable.range(1, 10)
        .parallel()
        .mapOptional(Optional::of)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 3 == 0;
            }
        })
        .sequential()
        .test()
        .assertResult(6);
    }

    @Test
    public void doubleFilterAsync() {
        Flowable.range(1, 10)
        .parallel()
        .runOn(Schedulers.computation())
        .mapOptional(Optional::of)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 3 == 0;
            }
        })
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(6);
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .mapOptional(Optional::of)
            .sequential()
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
    public void doubleError2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .mapOptional(Optional::of)
            .filter(Functions.alwaysTrue())
            .sequential()
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
    public void error() {
        Flowable.error(new TestException())
        .parallel()
        .mapOptional(Optional::of)
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapCrash() {
        Flowable.just(1)
        .parallel()
        .mapOptional(v -> { throw new TestException(); })
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapCrashConditional() {
        Flowable.just(1)
        .parallel()
        .mapOptional(v -> { throw new TestException(); })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapCrashConditional2() {
        Flowable.just(1)
        .parallel()
        .runOn(Schedulers.computation())
        .mapOptional(v -> { throw new TestException(); })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void allNone() {
        Flowable.range(1, 1000)
        .parallel()
        .mapOptional(v -> Optional.empty())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void allNoneConditional() {
        Flowable.range(1, 1000)
        .parallel()
        .mapOptional(v -> Optional.empty())
        .filter(v -> true)
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void mixed() {
        Flowable.range(1, 1000)
        .parallel()
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v) : Optional.empty())
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
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v) : Optional.empty())
        .filter(v -> true)
        .sequential()
        .test()
        .assertValueCount(500)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void invalidSubscriberCount() {
        TestHelper.checkInvalidParallelSubscribers(
                Flowable.range(1, 10).parallel()
                .mapOptional(Optional::of)
            );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeParallel(
                p -> p.mapOptional(Optional::of)
            );

        TestHelper.checkDoubleOnSubscribeParallel(
                p -> p.mapOptional(Optional::of)
                .filter(v -> true)
            );
    }
}
