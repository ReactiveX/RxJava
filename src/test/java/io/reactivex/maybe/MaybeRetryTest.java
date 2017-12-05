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

package io.reactivex.maybe;

import io.reactivex.Maybe;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.Functions;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class MaybeRetryTest {
    @Test
    public void retryTimesPredicateWithMatchingPredicate() {
        final AtomicInteger atomicInteger = new AtomicInteger(3);

        Maybe.fromCallable(new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
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
    }

    @Test
    public void retryTimesPredicateWithMatchingRetryAmount() {
        final AtomicInteger atomicInteger = new AtomicInteger(3);

        Maybe.fromCallable(new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }

                return true;
            }
        })
            .retry(2, Functions.alwaysTrue())
            .test()
            .assertResult(true);
    }

    @Test
    public void retryTimesPredicateWithNotMatchingRetryAmount() {
        final AtomicInteger atomicInteger = new AtomicInteger(3);

        Maybe.fromCallable(new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }

                return true;
            }
        })
            .retry(1, Functions.alwaysTrue())
            .test()
            .assertFailure(RuntimeException.class);
    }

    @Test
    public void retryTimesPredicateWithZeroRetries() {
        final AtomicInteger atomicInteger = new AtomicInteger(2);

        Maybe.fromCallable(new Callable<Boolean>() {
            @Override public Boolean call() throws Exception {
                if (atomicInteger.decrementAndGet() != 0) {
                    throw new RuntimeException();
                }

                return true;
            }
        })
            .retry(0, Functions.alwaysTrue())
            .test()
            .assertFailure(RuntimeException.class);
    }
}
