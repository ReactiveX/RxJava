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

package io.reactivex.rxjava4.internal.operators.maybe;

import java.util.Arrays;

import org.junit.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StandardConcurrentBufferedConfig;
import io.reactivex.rxjava4.exceptions.TestException;

public class MaybeConcatEagerTest {

    @Test
    public void iterableNormal() {
        Maybe.concatEager(Arrays.asList(
                Maybe.just(1),
                Maybe.empty(),
                Maybe.just(2)
        ))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void iterableNormalMaxConcurrency() {
        Maybe.concatEager(Arrays.asList(
                Maybe.just(1),
                Maybe.empty(),
                Maybe.just(2)
        ), new StandardConcurrentBufferedConfig(1))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void iterableError() {
        Maybe.concatEager(Arrays.asList(
                Maybe.just(1),
                Maybe.error(new TestException()),
                Maybe.empty(),
                Maybe.just(2)
        ))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void iterableErrorMaxConcurrency() {
        Maybe.concatEager(Arrays.asList(
                Maybe.just(1),
                Maybe.error(new TestException()),
                Maybe.empty(),
                Maybe.just(2)
        ), new StandardConcurrentBufferedConfig(1))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void publisherNormal() {
        Maybe.concatEager(Flowable.fromArray(
                Maybe.just(1),
                Maybe.empty(),
                Maybe.just(2)
        ))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void publisherNormalMaxConcurrency() {
        Maybe.concatEager(Flowable.fromArray(
                Maybe.just(1),
                Maybe.empty(),
                Maybe.just(2)
        ), new StandardConcurrentBufferedConfig(1))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void publisherError() {
        Maybe.concatEager(Flowable.fromArray(
                Maybe.just(1),
                Maybe.error(new TestException()),
                Maybe.empty(),
                Maybe.just(2)
        ))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void iterableDelayError() {
        Maybe.concatEager(Arrays.asList(
                Maybe.just(1),
                Maybe.error(new TestException()),
                Maybe.empty(),
                Maybe.just(2)
        ), StandardConcurrentBufferedConfig.DELAY_ERRORS)
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void iterableDelayErrorMaxConcurrency() {
        Maybe.concatEager(Arrays.asList(
                Maybe.just(1),
                Maybe.error(new TestException()),
                Maybe.empty(),
                Maybe.just(2)
        ), new StandardConcurrentBufferedConfig(true, 1))
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void publisherDelayError() {
        Maybe.concatEager(Flowable.fromArray(
                Maybe.just(1),
                Maybe.error(new TestException()),
                Maybe.empty(),
                Maybe.just(2)
        ), StandardConcurrentBufferedConfig.DELAY_ERRORS)
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void publisherDelayErrorMaxConcurrency() {
        Maybe.concatEager(Flowable.fromArray(
                Maybe.just(1),
                Maybe.error(new TestException()),
                Maybe.empty(),
                Maybe.just(2)
        ), new StandardConcurrentBufferedConfig(true, 1))
        .test()
        .assertFailure(TestException.class, 1, 2);
    }
}
