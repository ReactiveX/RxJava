/**
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

package io.reactivex.rxjava3.internal.operators.single;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleCacheTest extends RxJavaTest {

    @Test
    public void cancelImmediately() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Single<Integer> cached = pp.single(-99).cache();

        TestObserver<Integer> to = cached.test(true);

        pp.onNext(1);
        pp.onComplete();

        to.assertEmpty();

        cached.test().assertResult(1);
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            final Single<Integer> cached = pp.single(-99).cache();

            final TestObserver<Integer> to1 = cached.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to1.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cached.test();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void doubleDispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final Single<Integer> cached = pp.single(-99).cache();

        SingleObserver<Integer> doubleDisposer = new SingleObserver<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                d.dispose();
                d.dispose();
            }

            @Override
            public void onSuccess(Integer value) {

            }

            @Override
            public void onError(Throwable e) {

            }
        };
        cached.subscribe(doubleDisposer);

        cached.test();

        cached.subscribe(doubleDisposer);
    }
}
