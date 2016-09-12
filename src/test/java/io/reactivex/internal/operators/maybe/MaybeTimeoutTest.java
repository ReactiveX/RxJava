/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;

public class MaybeTimeoutTest {

    @Test
    public void normal() {
        Maybe.just(1)
        .timeout(1, TimeUnit.DAYS)
        .test()
        .assertResult(1);
    }

    @Test
    public void normalMaybe() {
        Maybe.just(1)
        .timeout(Maybe.timer(1, TimeUnit.DAYS))
        .test()
        .assertResult(1);
    }

    @Test
    public void never() {
        Maybe.never()
        .timeout(1, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void neverMaybe() {
        Maybe.never()
        .timeout(Maybe.timer(1, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void normalFallback() {
        Maybe.just(1)
        .timeout(1, TimeUnit.DAYS, Maybe.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void normalMaybeFallback() {
        Maybe.just(1)
        .timeout(Maybe.timer(1, TimeUnit.DAYS), Maybe.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void neverFallback() {
        Maybe.never()
        .timeout(1, TimeUnit.MILLISECONDS, Maybe.just(2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }

    @Test
    public void neverMaybeFallback() {
        Maybe.never()
        .timeout(Maybe.timer(1, TimeUnit.MILLISECONDS), Maybe.just(2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }

    @Test
    public void neverFallbackScheduler() {
        Maybe.never()
        .timeout(1, TimeUnit.MILLISECONDS, Schedulers.single(), Maybe.just(2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }

    @Test
    public void neverScheduler() {
        Maybe.never()
        .timeout(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void normalFlowableFallback() {
        Maybe.just(1)
        .timeout(Flowable.timer(1, TimeUnit.DAYS), Maybe.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void neverFlowableFallback() {
        Maybe.never()
        .timeout(Flowable.timer(1, TimeUnit.MILLISECONDS), Maybe.just(2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2);
    }

    @Test
    public void normalFlowable() {
        Maybe.just(1)
        .timeout(Flowable.timer(1, TimeUnit.DAYS))
        .test()
        .assertResult(1);
    }

    @Test
    public void neverFlowable() {
        Maybe.never()
        .timeout(Flowable.timer(1, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }
}
