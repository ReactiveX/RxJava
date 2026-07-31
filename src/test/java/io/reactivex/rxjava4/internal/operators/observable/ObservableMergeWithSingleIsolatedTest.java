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
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.subjects.SingleSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class ObservableMergeWithSingleIsolatedTest extends RxJavaTest {

    @Test
    public void completeRace() {
        for (int i = 0; i < 10000; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();
            final SingleSubject<Integer> cs = SingleSubject.create();

            TestObserver<Integer> to = ps.mergeWith(cs).test();

            Runnable r1 = () -> {
                ps.onNext(1);
                ps.onComplete();
            };

            Runnable r2 = () -> cs.onSuccess(1);

            TestHelper.race(r1, r2);

            to.assertResult(1, 1);
        }
    }
}
