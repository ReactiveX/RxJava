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

package io.reactivex.rxjava4.internal.operators.observable;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class ObservableFlatMapIsolatedTest extends RxJavaTest {

    @Test
    public void innerCompleteCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = Observable.merge(Observable.just(ps)).test();

            Runnable r1 = ps::onComplete;

            Runnable r2 = to::dispose;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void cancelScalarDrainRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {

                final PublishSubject<Observable<Integer>> ps = PublishSubject.create();

                final TestObserver<Integer> to = ps.flatMap(Functions.<Observable<Integer>>identity()).test();

                Runnable r1 = to::dispose;
                Runnable r2 = ps::onComplete;

                TestHelper.race(r1, r2);

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void cancelDrainRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            for (int j = 1; j < 50; j += 5) {
                List<Throwable> errors = TestHelper.trackPluginErrors();
                try {

                    final PublishSubject<Observable<Integer>> ps = PublishSubject.create();

                    final TestObserver<Integer> to = ps.flatMap(Functions.<Observable<Integer>>identity()).test();

                    final PublishSubject<Integer> just = PublishSubject.create();
                    final PublishSubject<Integer> just2 = PublishSubject.create();
                    ps.onNext(just);
                    ps.onNext(just2);

                    Runnable r1 = () -> {
                        just2.onNext(1);
                        to.dispose();
                    };
                    Runnable r2 = () -> just.onNext(1);

                    TestHelper.race(r1, r2);

                    assertTrue(errors.isEmpty(), errors.toString());
                } finally {
                    RxJavaPlugins.reset();
                }
            }
        }
    }
}
