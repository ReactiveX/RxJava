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

package io.reactivex.rxjava3.flowable;

import static io.reactivex.rxjava3.internal.util.TestingHelper.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public final class FlowableCollectTest extends RxJavaTest {

    @Test
    public void collectToListFlowable() {
        Flowable<List<Integer>> f = Flowable.just(1, 2, 3)
        .collect((Supplier<List<Integer>>) ArrayList::new, List::add).toFlowable();

        List<Integer> list =  f.blockingLast();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(2, list.get(1).intValue());
        assertEquals(3, list.get(2).intValue());

        // test multiple subscribe
        List<Integer> list2 =  f.blockingLast();

        assertEquals(3, list2.size());
        assertEquals(1, list2.get(0).intValue());
        assertEquals(2, list2.get(1).intValue());
        assertEquals(3, list2.get(2).intValue());
    }

    @Test
    public void collectToStringFlowable() {
        String value = Flowable.just(1, 2, 3)
            .collect(
                    StringBuilder::new,
                    (sb, v) -> {
                    if (sb.length() > 0) {
                        sb.append("-");
                    }
                    sb.append(v);
                }).toFlowable().blockingLast().toString();

        assertEquals("1-2-3", value);
    }

    @Test
    public void factoryFailureResultsInErrorEmissionFlowable() {
        final RuntimeException e = new RuntimeException();
        Flowable.just(1).collect((Supplier<List<Integer>>) () -> {
            throw e;
        }, List::add)
        .test()
        .assertNoValues()
        .assertError(e)
        .assertNotComplete();
    }

    @Test
    public void collectorFailureDoesNotResultInTwoErrorEmissionsFlowable() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<>();
            RxJavaPlugins.setErrorHandler(addToList(list));
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();

            Burst.items(1).error(e2) //
                    .collect(supplierListCreator(), biConsumerThrows(e1))
                    .toFlowable()
                    .test() //
                    .assertError(e1) //
                    .assertNotComplete();

            assertEquals(1, list.size());
            assertEquals(e2, list.get(0).getCause());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void collectorFailureDoesNotResultInErrorAndCompletedEmissionsFlowable() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create() //
                .collect(supplierListCreator(), biConsumerThrows(e)) //
                .toFlowable()
                .test() //
                .assertError(e) //
                .assertNotComplete();
    }

    @Test
    public void collectorFailureDoesNotResultInErrorAndOnNextEmissionsFlowable() {
        final RuntimeException e = new RuntimeException();
        final AtomicBoolean added = new AtomicBoolean();
        BiConsumer<Object, Integer> throwOnFirstOnly = new BiConsumer<Object, Integer>() {

            boolean once = true;

            @Override
            public void accept(Object o, Integer t) {
                if (once) {
                    once = false;
                    throw e;
                } else {
                    added.set(true);
                }
            }
        };
        Burst.items(1, 2).create() //
                .collect(supplierListCreator(), throwOnFirstOnly)//
                .toFlowable()
                .test() //
                .assertError(e) //
                .assertNoValues() //
                .assertNotComplete();
        assertFalse(added.get());
    }

    @Test
    public void collectIntoFlowable() {
        Flowable.just(1, 1, 1, 1, 2)
        .collectInto(new HashSet<>(), (BiConsumer<HashSet<Integer>, Integer>) HashSet::add)
        .toFlowable()
        .test()
        .assertResult(new HashSet<>(Arrays.asList(1, 2)));
    }

    @Test
    public void collectToList() {
        Single<List<Integer>> o = Flowable.just(1, 2, 3)
        .collect((Supplier<List<Integer>>) ArrayList::new, List::add);

        List<Integer> list =  o.blockingGet();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(2, list.get(1).intValue());
        assertEquals(3, list.get(2).intValue());

        // test multiple subscribe
        List<Integer> list2 =  o.blockingGet();

        assertEquals(3, list2.size());
        assertEquals(1, list2.get(0).intValue());
        assertEquals(2, list2.get(1).intValue());
        assertEquals(3, list2.get(2).intValue());
    }

    @Test
    public void collectToString() {
        String value = Flowable.just(1, 2, 3)
            .collect(
                    StringBuilder::new,
                    (sb, v) -> {
                    if (sb.length() > 0) {
                        sb.append("-");
                    }
                    sb.append(v);
                }).blockingGet().toString();

        assertEquals("1-2-3", value);
    }

    @Test
    public void factoryFailureResultsInErrorEmission() {
        final RuntimeException e = new RuntimeException();
        Flowable.just(1).collect((Supplier<List<Integer>>) () -> {
            throw e;
        }, List::add)
        .test()
        .assertNoValues()
        .assertError(e)
        .assertNotComplete();
    }

    @Test
    public void collectorFailureDoesNotResultInTwoErrorEmissions() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<>();
            RxJavaPlugins.setErrorHandler(addToList(list));
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();

            Burst.items(1).error(e2) //
                    .collect(supplierListCreator(), biConsumerThrows(e1)) //
                    .test() //
                    .assertError(e1) //
                    .assertNotComplete();

            assertEquals(1, list.size());
            assertEquals(e2, list.get(0).getCause());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void collectorFailureDoesNotResultInErrorAndCompletedEmissions() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create() //
                .collect(supplierListCreator(), biConsumerThrows(e)) //
                .test() //
                .assertError(e) //
                .assertNotComplete();
    }

    @Test
    public void collectorFailureDoesNotResultInErrorAndOnNextEmissions() {
        final RuntimeException e = new RuntimeException();
        final AtomicBoolean added = new AtomicBoolean();
        BiConsumer<Object, Integer> throwOnFirstOnly = new BiConsumer<Object, Integer>() {

            boolean once = true;

            @Override
            public void accept(Object o, Integer t) {
                if (once) {
                    once = false;
                    throw e;
                } else {
                    added.set(true);
                }
            }
        };
        Burst.items(1, 2).create() //
                .collect(supplierListCreator(), throwOnFirstOnly)//
                .test() //
                .assertError(e) //
                .assertNoValues() //
                .assertNotComplete();
        assertFalse(added.get());
    }

    @Test
    public void collectInto() {
        Flowable.just(1, 1, 1, 1, 2)
        .collectInto(new HashSet<>(), (BiConsumer<HashSet<Integer>, Integer>) HashSet::add)
        .test()
        .assertResult(new HashSet<>(Arrays.asList(1, 2)));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1, 2)
            .collect(Functions.justSupplier(new ArrayList<>()), (BiConsumer<ArrayList<Integer>, Integer>) ArrayList::add));

        TestHelper.checkDisposed(Flowable.just(1, 2)
                .collect(Functions.justSupplier(new ArrayList<>()), (BiConsumer<ArrayList<Integer>, Integer>) ArrayList::add).toFlowable());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable((Function<Flowable<Integer>, Flowable<ArrayList<Integer>>>) f -> f.collect(Functions.justSupplier(new ArrayList<>()),
                (BiConsumer<ArrayList<Integer>, Integer>) ArrayList::add).toFlowable());
        TestHelper.checkDoubleOnSubscribeFlowableToSingle((Function<Flowable<Integer>, Single<ArrayList<Integer>>>) f -> f.collect(Functions.justSupplier(new ArrayList<>()),
                ArrayList::add));
    }
}
