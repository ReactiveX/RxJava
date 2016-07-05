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

package io.reactivex.internal.operators.flowable;

import java.util.Arrays;

import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.functions.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableFlattenIterableTest {

    @Test
    public void normal() {
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.range(1, 2)
        .reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) {
                return Math.max(a, b);
            }
        })
        .flatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(ts);
        
        ts.assertValues(2, 3)
        .assertNoErrors()
        .assertComplete();
    }
}
