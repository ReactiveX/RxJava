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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.*;

@Isolated
public class ObservableCacheIsolatedTest extends RxJavaTest {

    @Test
    public void subscribeEmitRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.<Integer>create();

            final Observable<Integer> cache = ps.cache();

            cache.test();

            final TestObserverEx<Integer> to = new TestObserverEx<>();

            Runnable r1 = () -> cache.subscribe(to);

            Runnable r2 = () -> {
                for (int j = 0; j < 500; j++) {
                    ps.onNext(j);
                }
                ps.onComplete();
            };

            TestHelper.race(r1, r2);

            to
            .awaitDone(5, TimeUnit.SECONDS)
            .assertSubscribed().assertValueCount(500).assertComplete().assertNoErrors();
        }
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            Observable<Object> o = Observable.never().cache();

            TestObserver<Object> to = o.test();

            TestHelper.race(
                    to::dispose,
                    o::test
            );
        }
    }
}
