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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.subjects.SingleSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class ObservableFlatMapSingleIsolatedTest extends RxJavaTest {

    @Test
    public void innerErrorOuterCompleteRace() {
        TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishSubject<Integer> ps1 = PublishSubject.create();
            SingleSubject<Integer> ps2 = SingleSubject.create();

            TestObserver<Integer> to = ps1.flatMapSingle(_ -> ps2)
            .test();

            ps1.onNext(1);

            TestHelper.race(
                    ps1::onComplete,
                    () -> ps2.onError(ex)
            );

            to.assertFailure(TestException.class);
        }
    }
}
