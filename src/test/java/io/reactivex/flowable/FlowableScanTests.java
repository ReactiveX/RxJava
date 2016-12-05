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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.flowable.FlowableEventStream.Event;
import io.reactivex.functions.*;
import io.reactivex.plugins.RxJavaPlugins;

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
    public void testScanWithSeedDoesNotEmitErrorTwiceIfScanFunctionThrows() {
        final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
        Consumer<Throwable> errorConsumer = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) throws Exception {
                 list.add(t);
            }};
        try {
            RxJavaPlugins.setErrorHandler(errorConsumer);
            final RuntimeException e = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();
            Burst.items(1).error(e2)
              .scan(0, throwingBiFunction(e))
              .test()
              .assertNoValues()
              .assertError(e);
            assertEquals(Arrays.asList(e2), list);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void testScanWithSeedDoesNotEmitTerminalEventTwiceIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create()
          .scan(0, throwingBiFunction(e))
          .test()
          .assertNoValues()
          .assertError(e);
    }

    @Test
    public void testScanWithSeedDoesNotProcessOnNextAfterTerminalEventIfScanFunctionThrows() {
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
    public void testScanWithSeedCompletesNormally() {
        Flowable.just(1,2,3).scan(0, SUM)
          .test()
          .assertValues(0, 1, 3, 6)
          .assertComplete();
    }

    @Test
    public void testScanWithSeedWhenScanSeedProviderThrows() {
        final RuntimeException e = new RuntimeException();
        Flowable.just(1,2,3).scanWith(throwingCallable(e),
            SUM)
          .test()
          .assertError(e)
          .assertNoValues();
    }

    @Test
    public void testScanNoSeed() {
        Flowable.just(1, 2, 3)
           .scan(SUM)
           .test()
           .assertValues(1, 3, 6)
           .assertComplete();
    }

    @Test
    public void testScanNoSeedDoesNotEmitErrorTwiceIfScanFunctionThrows() {
        final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
        Consumer<Throwable> errorConsumer = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) throws Exception {
                 list.add(t);
            }};
        try {
            RxJavaPlugins.setErrorHandler(errorConsumer);
            final RuntimeException e = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();
            Burst.items(1, 2).error(e2)
              .scan(throwingBiFunction(e))
              .test()
              .assertValue(1)
              .assertError(e);
            assertEquals(Arrays.asList(e2), list);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void testScanNoSeedDoesNotEmitTerminalEventTwiceIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        Burst.items(1, 2).create()
          .scan(throwingBiFunction(e))
          .test()
          .assertValue(1)
          .assertError(e);
    }

    @Test
    public void testScanNoSeedDoesNotProcessOnNextAfterTerminalEventIfScanFunctionThrows() {
        final RuntimeException e = new RuntimeException();
        final AtomicInteger count = new AtomicInteger();
        Burst.items(1, 2, 3).create().scan(new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer n1, Integer n2) throws Exception {
                count.incrementAndGet();
                throw e;
            }})
          .test()
          .assertValue(1)
          .assertError(e);
        assertEquals(1, count.get());
    }

    private static BiFunction<Integer,Integer, Integer> throwingBiFunction(final RuntimeException e) {
        return new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer n1, Integer n2) throws Exception {
                throw e;
            }
        };
    }

    private static final BiFunction<Integer, Integer, Integer> SUM = new BiFunction<Integer, Integer, Integer>() {

        @Override
        public Integer apply(Integer t1, Integer t2) throws Exception {
            return t1 + t2;
        }
    };

    private static Callable<Integer> throwingCallable(final RuntimeException e) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw e;
            }
        };
    }
}