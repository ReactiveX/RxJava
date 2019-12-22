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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.completable.CompletableAmb.Amb;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableAmbTest extends RxJavaTest {

    @Test
    public void ambLots() {
        List<Completable> ms = new ArrayList<>();

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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
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

                TestHelper.race(r1, r2);

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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
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

                TestHelper.race(r1, r2);

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

    @Test
    public void ambRace() {
        TestObserver<Void> to = new TestObserver<>();
        to.onSubscribe(Disposable.empty());

        CompositeDisposable cd = new CompositeDisposable();
        AtomicBoolean once = new AtomicBoolean();
        Amb a = new Amb(once, cd, to);
        a.onSubscribe(Disposable.empty());

        a.onComplete();
        a.onComplete();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            a.onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void untilCompletableMainComplete() {
        CompletableSubject main = CompletableSubject.create();
        CompletableSubject other = CompletableSubject.create();

        TestObserver<Void> to = main.ambWith(other).test();

        assertTrue("Main no observers?", main.hasObservers());
        assertTrue("Other no observers?", other.hasObservers());

        main.onComplete();

        assertFalse("Main has observers?", main.hasObservers());
        assertFalse("Other has observers?", other.hasObservers());

        to.assertResult();
    }

    @Test
    public void untilCompletableMainError() {
        CompletableSubject main = CompletableSubject.create();
        CompletableSubject other = CompletableSubject.create();

        TestObserver<Void> to = main.ambWith(other).test();

        assertTrue("Main no observers?", main.hasObservers());
        assertTrue("Other no observers?", other.hasObservers());

        main.onError(new TestException());

        assertFalse("Main has observers?", main.hasObservers());
        assertFalse("Other has observers?", other.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void untilCompletableOtherOnComplete() {
        CompletableSubject main = CompletableSubject.create();
        CompletableSubject other = CompletableSubject.create();

        TestObserver<Void> to = main.ambWith(other).test();

        assertTrue("Main no observers?", main.hasObservers());
        assertTrue("Other no observers?", other.hasObservers());

        other.onComplete();

        assertFalse("Main has observers?", main.hasObservers());
        assertFalse("Other has observers?", other.hasObservers());

        to.assertResult();
    }

    @Test
    public void untilCompletableOtherError() {
        CompletableSubject main = CompletableSubject.create();
        CompletableSubject other = CompletableSubject.create();

        TestObserver<Void> to = main.ambWith(other).test();

        assertTrue("Main no observers?", main.hasObservers());
        assertTrue("Other no observers?", other.hasObservers());

        other.onError(new TestException());

        assertFalse("Main has observers?", main.hasObservers());
        assertFalse("Other has observers?", other.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void noWinnerErrorDispose() throws Exception {
        final TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Completable.ambArray(
                    Completable.error(ex)
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                    Completable.never()
            )
            .subscribe(Functions.EMPTY_ACTION, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void noWinnerCompleteDispose() throws Exception {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Completable.ambArray(
                Completable.complete()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Completable.never()
            )
            .subscribe(new Action() {
                @Override
                public void run() throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void completableSourcesInIterable() {
        CompletableSource source = new CompletableSource() {
            @Override
            public void subscribe(CompletableObserver observer) {
                Completable.complete().subscribe(observer);
            }
        };

        Completable.amb(Arrays.asList(source, source))
        .test()
        .assertResult();
    }
}
