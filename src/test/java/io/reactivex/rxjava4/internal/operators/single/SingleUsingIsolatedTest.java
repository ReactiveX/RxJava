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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.core.Single;
import io.reactivex.rxjava4.core.SingleSource;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.Consumer;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.testsupport.SuppressUndeliverable;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class SingleUsingIsolatedTest extends RxJavaTest {

    final Consumer<Disposable> disposer = Disposable::dispose;

    @Test
    public void successDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            Disposable d = Disposable.empty();

            final TestObserver<Integer> to = Single.using(Functions.justSupplier(d), (Function<Disposable, SingleSource<Integer>>) _ -> pp.single(-99), disposer)
            .test();

            pp.onNext(1);

            Runnable r1 = pp::onComplete;
            Runnable r2 = to::dispose;

            TestHelper.race(r1, r2);

            assertTrue(d.isDisposed());
        }
    }

    @Test
    @SuppressUndeliverable
    public void errorDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            Disposable d = Disposable.empty();

            final TestObserver<Integer> to = Single.using(Functions.justSupplier(d), (Function<Disposable, SingleSource<Integer>>) _ -> pp.single(-99), disposer)
            .test();

            final TestException ex = new TestException();

            Runnable r1 = () -> pp.onError(ex);
            Runnable r2 = to::dispose;

            TestHelper.race(r1, r2);

            assertTrue(d.isDisposed());
        }
    }
}
