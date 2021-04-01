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

package io.reactivex.rxjava3.single;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.functions.Functions;

public class SingleRetryTest extends RxJavaTest {
    @Test
    public void retryTimesPredicateWithMatchingPredicate() {
        final AtomicInteger atomicInteger = new AtomicInteger(3);
        final AtomicInteger numberOfSubscribeCalls = new AtomicInteger(0);

        Single.fromCallable(new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
                numberOfSubscribeCalls.incrementAndGet();

                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }

                throw new IllegalArgumentException();
            }
        })
            .retry(Integer.MAX_VALUE, new Predicate<Throwable>() {
                @Override public boolean test(final Throwable throwable) throws Exception {
                    return !(throwable instanceof IllegalArgumentException);
                }
            })
            .test()
            .assertFailure(IllegalArgumentException.class);

        assertEquals(3, numberOfSubscribeCalls.get());
    }

    @Test
    public void retryTimesPredicateWithMatchingRetryAmount() {
        final AtomicInteger atomicInteger = new AtomicInteger(3);
        final AtomicInteger numberOfSubscribeCalls = new AtomicInteger(0);

        Single.fromCallable(new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
                numberOfSubscribeCalls.incrementAndGet();

                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }

                return true;
            }
        })
            .retry(2, Functions.alwaysTrue())
            .test()
            .assertResult(true);

        assertEquals(3, numberOfSubscribeCalls.get());
    }

    @Test
    public void retryTimesPredicateWithNotMatchingRetryAmount() {
        final AtomicInteger atomicInteger = new AtomicInteger(3);
        final AtomicInteger numberOfSubscribeCalls = new AtomicInteger(0);

        Single.fromCallable(new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
                numberOfSubscribeCalls.incrementAndGet();

                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }

                return true;
            }
        })
            .retry(1, Functions.alwaysTrue())
            .test()
            .assertFailure(RuntimeException.class);

        assertEquals(2, numberOfSubscribeCalls.get());
    }

    @Test
    public void retryTimesPredicateWithZeroRetries() {
        final AtomicInteger atomicInteger = new AtomicInteger(2);
        final AtomicInteger numberOfSubscribeCalls = new AtomicInteger(0);

        Single.fromCallable(new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
                numberOfSubscribeCalls.incrementAndGet();

                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }

                return true;
            }
        })
            .retry(0, Functions.alwaysTrue())
            .test()
            .assertFailure(RuntimeException.class);

        assertEquals(1, numberOfSubscribeCalls.get());
    }

    @Test
    public void untilTrueJust() {
        Single.just(1)
        .retryUntil(() -> true)
        .test()
        .assertResult(1);
    }

    @Test
    public void untilFalseJust() {
        Single.just(1)
        .retryUntil(() -> false)
        .test()
        .assertResult(1);
    }

    @Test
    public void untilTrueError() {
        Single.error(new TestException())
        .retryUntil(() -> true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void untilFalseError() {
        AtomicInteger counter = new AtomicInteger();
        Single.defer(() -> {
            if (counter.getAndIncrement() == 0) {
                return Single.error(new TestException());
            }
            return Single.just(1);
        })
        .retryUntil(() -> false)
        .test()
        .assertResult(1);
    }
}
