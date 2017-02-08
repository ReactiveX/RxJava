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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.*;

public class CompletableAmbTest {

    @Test
    public void ambLots() {
        List<Completable> ms = new ArrayList<Completable>();

        for (int i = 0; i < 32; i++) {
            ms.add(Completable.never());
        }

        ms.add(Completable.complete());

        Completable.amb(ms)
        .test()
        .assertResult();
    }

    @Test
    public void ambFirstDone() {
        Completable.amb(Arrays.asList(Completable.complete(), Completable.complete()))
        .test()
        .assertResult();
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Void> to = Completable.amb(Arrays.asList(pp1.ignoreElements(), pp2.ignoreElements()))
        .test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        to.dispose();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());
    }

    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp0 = PublishProcessor.create();
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();

                final TestObserver<Void> to = Completable.amb(Arrays.asList(pp0.ignoreElements(), pp1.ignoreElements()))
                .test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp0.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex);
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void nullSourceSuccessRace() {
        for (int i = 0; i < 1000; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {

                final Subject<Integer> ps = ReplaySubject.create();
                ps.onNext(1);

                final Completable source = Completable.ambArray(ps.ignoreElements(), Completable.never(), Completable.never(), null);

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        source.test();
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps.onComplete();
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());

                if (!errors.isEmpty()) {
                    TestHelper.assertError(errors, 0, NullPointerException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void ambWithOrder() {
        Completable error = Completable.error(new RuntimeException());
        Completable.complete().ambWith(error).test().assertComplete();
    }

    @Test
    public void ambIterableOrder() {
        Completable error = Completable.error(new RuntimeException());
        Completable.amb(Arrays.asList(Completable.complete(), error)).test().assertComplete();
    }

    @Test
    public void ambArrayOrder() {
        Completable error = Completable.error(new RuntimeException());
        Completable.ambArray(Completable.complete(), error).test().assertComplete();
    }

}
