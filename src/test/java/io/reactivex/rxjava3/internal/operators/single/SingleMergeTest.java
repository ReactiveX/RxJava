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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.assertTrue;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleMergeTest extends RxJavaTest {

    @Test
    public void mergeSingleSingle() {

        Single.merge(Single.just(Single.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void merge2() {
        Single.merge(Single.just(1), Single.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void merge3() {
        Single.merge(Single.just(1), Single.just(2), Single.just(3))
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void merge4() {
        Single.merge(Single.just(1), Single.just(2), Single.just(3), Single.just(4))
        .test()
        .assertResult(1, 2, 3, 4);
    }

    @Test
    public void mergeErrors() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Single<Integer> source1 = Single.error(new TestException("First"));
            Single<Integer> source2 = Single.error(new TestException("Second"));

            Single.merge(source1, source2)
            .to(TestHelper.<Integer>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void mergeDelayErrorIterable() {
        Single.mergeDelayError(Arrays.asList(
                Single.just(1),
                Single.<Integer>error(new TestException()),
                Single.just(2))
        )
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void mergeDelayErrorPublisher() {
        Single.mergeDelayError(Flowable.just(
                Single.just(1),
                Single.<Integer>error(new TestException()),
                Single.just(2))
        )
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void mergeDelayError2() {
        Single.mergeDelayError(
                Single.just(1),
                Single.<Integer>error(new TestException())
        )
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void mergeDelayError2ErrorFirst() {
        Single.mergeDelayError(
                Single.<Integer>error(new TestException()),
                Single.just(1)
        )
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void mergeDelayError3() {
        Single.mergeDelayError(
                Single.just(1),
                Single.<Integer>error(new TestException()),
                Single.just(2)
        )
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void mergeDelayError4() {
        Single.mergeDelayError(
                Single.just(1),
                Single.<Integer>error(new TestException()),
                Single.just(2),
                Single.just(3)
        )
        .test()
        .assertFailure(TestException.class, 1, 2, 3);
    }
}
