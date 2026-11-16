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

package io.reactivex.rxjava4.internal.operators.completable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class CompletableCacheIsolatedTest extends RxJavaTest {

    @Test
    public void subscribeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishSubject<Integer> ps = PublishSubject.create();

            final Completable c = ps.ignoreElements().cache();

            final TestObserver<Void> to1 = new TestObserver<>();

            final TestObserver<Void> to2 = new TestObserver<>();

            Runnable r1 = () -> c.subscribe(to1);
            Runnable r2 = () -> c.subscribe(to2);

            TestHelper.race(r1, r2);

            ps.onComplete();

            to1.assertResult();
            to2.assertResult();
        }
    }

    @Test
    public void subscribeDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishSubject<Integer> ps = PublishSubject.create();

            final Completable c = ps.ignoreElements().cache();

            final TestObserver<Void> to1 = c.test();

            final TestObserver<Void> to2 = new TestObserver<>();

            Runnable r1 = to1::dispose;
            Runnable r2 = () -> c.subscribe(to2);

            TestHelper.race(r1, r2);

            ps.onComplete();

            to1.assertEmpty();
            to2.assertResult();
        }
    }
}
