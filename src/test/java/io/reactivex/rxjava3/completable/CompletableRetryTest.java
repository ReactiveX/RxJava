/**
 * Copyright (c) 2017-present, RxJava Contributors.
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

package io.reactivex.rxjava3.completable;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;

public class CompletableRetryTest extends RxJavaTest {
    @Test
    public void retryTimesPredicateWithMatchingPredicate() {
        final AtomicInteger atomicInteger = new AtomicInteger(3);
        final AtomicInteger numberOfSubscribeCalls = new AtomicInteger(0);

        Completable.fromAction(new Action() {
            @Override public void run() throws Exception {
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

        Completable.fromAction(new Action() {
            @Override public void run() throws Exception {
                numberOfSubscribeCalls.incrementAndGet();

                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }
            }
        })
            .retry(2, Functions.alwaysTrue())
            .test()
            .assertResult();

        assertEquals(3, numberOfSubscribeCalls.get());
    }

    @Test
    public void retryTimesPredicateWithNotMatchingRetryAmount() {
        final AtomicInteger atomicInteger = new AtomicInteger(3);
        final AtomicInteger numberOfSubscribeCalls = new AtomicInteger(0);

        Completable.fromAction(new Action() {
            @Override public void run() throws Exception {
                numberOfSubscribeCalls.incrementAndGet();

                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }
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

        Completable.fromAction(new Action() {
            @Override public void run() throws Exception {
                numberOfSubscribeCalls.incrementAndGet();

                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }
            }
        })
            .retry(0, Functions.alwaysTrue())
            .test()
            .assertFailure(RuntimeException.class);

        assertEquals(1, numberOfSubscribeCalls.get());
    }
}
