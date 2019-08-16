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

package io.reactivex.rxjava3.parallel;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ParallelFilterTest extends RxJavaTest {

    @Test
    public void subscriberCount() {
        ParallelFlowableTest.checkSubscriberCount(Flowable.range(1, 5).parallel()
        .filter(Functions.alwaysTrue()));
    }

    @Test
    public void doubleFilter() {
        Flowable.range(1, 10)
        .parallel()
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
    public void doubleError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
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
    public void doubleError2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new ParallelInvalid()
            .filter(Functions.alwaysTrue())
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
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void predicateThrows() {
        Flowable.just(1)
        .parallel()
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .filter(Functions.alwaysTrue())
        .sequential()
        .test()
        .assertFailure(TestException.class);
    }
}
