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

package io.reactivex.rxjava4.internal.operators.single;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.subjects.*;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class SingleFlatMapIterableFlowableIsolatedTest extends RxJavaTest {

    @Test
    public void requestCreateInnerRace() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.onNext(1);

            final TestSubscriber<Integer> ts = ps.singleElement().flattenAsFlowable(
                            (Function<Integer, Iterable<Integer>>) _ -> Arrays.asList(a))
            .test(0L);

            Runnable r1 = () -> {
                ps.onComplete();
                for (int i1 = 0; i1 < 500; i1++) {
                    ts.request(1);
                }
            };

            Runnable r2 = () -> {
                for (int i2 = 0; i2 < 500; i2++) {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void cancelCreateInnerRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            ps.onNext(1);

            final TestSubscriber<Integer> ts = ps.singleElement().flattenAsFlowable(
                            (Function<Integer, Iterable<Integer>>) _ -> Arrays.asList(1, 2, 3))
            .test(0L);

            Runnable r1 = ps::onComplete;

            Runnable r2 = ts::cancel;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void requestIteratorRace() {
        final Integer[] a = new Integer[1000];
        Arrays.fill(a, 1);

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestSubscriber<Integer> ts = ps.singleOrError().flattenAsFlowable((Function<Integer, Iterable<Integer>>) _ -> Arrays.asList(a)).test();

            Runnable r1 = () -> {
                for (int i1 = 0; i1 < 1000; i1++) {
                    ts.request(1);
                }
            };

            Runnable r2 = () -> {
                ps.onNext(1);
                ps.onComplete();
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void onSuccessRequestRace() {
        List<Object> list = List.of(1);
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            SingleSubject<Integer> ss = SingleSubject.create();

            TestSubscriber<Object> ts = ss.flattenAsFlowable(_ -> list)
            .test(0L);

            TestHelper.race(
                    () -> ss.onSuccess(1),
                    () -> ts.request(1)
            );

            ts.assertResult(1);
        }
    }
}
