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

package io.reactivex.flowable;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.flowable.FlowableEventStream.Event;
import io.reactivex.functions.*;

public class FlowableScanTests {

    @Test
    public void testUnsubscribeScan() {

        FlowableEventStream.getEventStream("HTTP-ClusterB", 20)
        .scan(new HashMap<String, String>(), new BiFunction<HashMap<String, String>, Event, HashMap<String, String>>() {
            @Override
            public HashMap<String, String> apply(HashMap<String, String> accum, Event perInstanceEvent) {
                accum.put("instance", perInstanceEvent.instanceId);
                return accum;
            }
        })
        .take(10)
        .blockingForEach(new Consumer<HashMap<String, String>>() {
            @Override
            public void accept(HashMap<String, String> v) {
                System.out.println(v);
            }
        });
    }
    
    @Test
    public void testFlowableScanSeedDoesNotEmitErrorTwiceIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).error(e).scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer n1, Integer n2) throws Exception {
                throw e;
            }})
          .test()
          .assertNoValues()
          .assertError(e);
    }
    
    @Test
    public void testFlowableScanSeedDoesNotEmitTerminalEventTwiceIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create().scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer n1, Integer n2) throws Exception {
                throw e;
            }})
          .test()
          .assertNoValues()
          .assertError(e);
    }
    
    @Test
    public void testFlowableScanSeedDoesNotProcessOnNextAfterTerminalEventIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        final AtomicInteger count = new AtomicInteger();
        Burst.items(1, 2).create().scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer n1, Integer n2) throws Exception {
                count.incrementAndGet();
                throw e;
            }})
          .test()
          .assertNoValues()
          .assertError(e);
        assertEquals(1, count.get());
    }
    
    @Test
    public void testFlowableScanSeedCompletesNormally() {
        Flowable.just(1,2,3).scan(0, new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) throws Exception {
                return t1 + t2;
            }})
          .test()
          .assertValues(0, 1, 3, 6)
          .assertComplete();
    }
    
    @Test
    public void testFlowableScanSeedWhenScanSeedProviderThrows() {
        final RuntimeException e = new RuntimeException();
        Flowable.just(1,2,3).scanWith(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw e;
            }
        },
            new BiFunction<Integer, Integer, Integer>() {

                @Override
                public Integer apply(Integer t1, Integer t2) throws Exception {
                    return t1 + t2;
                }
           })
          .test()
          .assertError(e)
          .assertNoValues();
    }
}