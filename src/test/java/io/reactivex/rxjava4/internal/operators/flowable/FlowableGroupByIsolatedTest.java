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

package io.reactivex.rxjava4.internal.operators.flowable;

import java.time.Duration;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import com.google.common.cache.CacheBuilder;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.core.config.StandardBufferedConfig;
import io.reactivex.rxjava4.core.config.StandardConcurrentBufferedConfig;
import io.reactivex.rxjava4.functions.Consumer;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.schedulers.Schedulers;

@Isolated
public class FlowableGroupByIsolatedTest extends RxJavaTest {

    static <T> Function<Consumer<Object>, ConcurrentMap<T, Object>> ttlCapGuava(Duration ttl) {
        return itemEvictConsumer ->
            CacheBuilder
            .newBuilder()
            .expireAfterWrite(ttl)
            .removalListener(n -> {
                if (n.getCause() != com.google.common.cache.RemovalCause.EXPLICIT) {
                    try {
                        itemEvictConsumer.accept(n.getValue());
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                }
            }).<T, Object>build().asMap();
    }

    @Test
    public void issue6982Case1() {
        final int groups = 20;

        int groupByBufferSize = 2;
        int flatMapMaxConcurrency = 200 * groups;

        // ~50% of executions - Not completed (latch = 1, values = 500000, errors = 0, completions = 0, timeout!,
        // disposed!)

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .groupBy(i -> i, i -> i, ttlCapGuava(Duration.ofMillis(10)), new StandardBufferedConfig(false, groupByBufferSize))
        .flatMap(gf -> gf.observeOn(Schedulers.computation()), new StandardConcurrentBufferedConfig(flatMapMaxConcurrency))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }
}
