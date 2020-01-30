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

import java.util.Arrays;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;

public class SingleConcatEagerTest {

    @Test
    public void iterableNormal() {
        Single.concatEager(Arrays.asList(
                Single.just(1),
                Single.just(2)
        ))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void iterableNormalMaxConcurrency() {
        Single.concatEager(Arrays.asList(
                Single.just(1),
                Single.just(2)
        ), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void iterableError() {
        Single.concatEager(Arrays.asList(
                Single.just(1),
                Single.error(new TestException()),
                Single.just(2)
        ))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void iterableErrorMaxConcurrency() {
        Single.concatEager(Arrays.asList(
                Single.just(1),
                Single.error(new TestException()),
                Single.just(2)
        ), 1)
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void publisherNormal() {
        Single.concatEager(Flowable.fromArray(
                Single.just(1),
                Single.just(2)
        ))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void publisherNormalMaxConcurrency() {
        Single.concatEager(Flowable.fromArray(
                Single.just(1),
                Single.just(2)
        ), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void publisherError() {
        Single.concatEager(Flowable.fromArray(
                Single.just(1),
                Single.error(new TestException()),
                Single.just(2)
        ))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void iterableDelayError() {
        Single.concatEagerDelayError(Arrays.asList(
                Single.just(1),
                Single.error(new TestException()),
                Single.just(2)
        ))
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void iterableDelayErrorMaxConcurrency() {
        Single.concatEagerDelayError(Arrays.asList(
                Single.just(1),
                Single.error(new TestException()),
                Single.just(2)
        ), 1)
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void publisherDelayError() {
        Single.concatEagerDelayError(Flowable.fromArray(
                Single.just(1),
                Single.error(new TestException()),
                Single.just(2)
        ))
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void publisherDelayErrorMaxConcurrency() {
        Single.concatEagerDelayError(Flowable.fromArray(
                Single.just(1),
                Single.error(new TestException()),
                Single.just(2)
        ), 1)
        .test()
        .assertFailure(TestException.class, 1, 2);
    }
}
