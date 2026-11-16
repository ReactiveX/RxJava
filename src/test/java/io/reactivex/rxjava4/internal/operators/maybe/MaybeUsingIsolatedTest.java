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

package io.reactivex.rxjava4.internal.operators.maybe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.*;

@Isolated
public class MaybeUsingIsolatedTest extends RxJavaTest {

    @Test
    public void successDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = Maybe.using(() -> 1, _ -> ps.lastElement(), _ -> {
            }, true)
            .test();

            ps.onNext(1);

            Runnable r1 = to::dispose;

            Runnable r2 = ps::onComplete;

            TestHelper.race(r1, r2);
        }
    }

    @Test
    @SuppressUndeliverable
    public void errorDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = Maybe.using(() -> 1, _ -> ps.firstElement(), _ -> {
            }, true)
            .test();

            final TestException ex = new TestException();

            Runnable r1 = to::dispose;

            Runnable r2 = () -> ps.onError(ex);

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void emptyDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = Maybe.using(() -> 1, _ -> ps.firstElement(), _ -> {

            }, true)
            .test();

            Runnable r1 = to::dispose;

            Runnable r2 = ps::onComplete;

            TestHelper.race(r1, r2);
        }
    }
}
