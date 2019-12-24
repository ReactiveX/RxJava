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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeAmbTest extends RxJavaTest {

    @Test
    public void ambLots() {
        List<Maybe<Integer>> ms = new ArrayList<>();

        for (int i = 0; i < 32; i++) {
            ms.add(Maybe.<Integer>never());
        }

        ms.add(Maybe.just(1));

        Maybe.amb(ms)
        .test()
        .assertResult(1);
    }

    @Test
    public void ambFirstDone() {
        Maybe.amb(Arrays.asList(Maybe.just(1), Maybe.just(2)))
        .test()
        .assertResult(1);
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
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

                final TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp0.singleElement(), pp1.singleElement()))
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
    public void disposeNoFurtherSignals() {
        TestObserver<Integer> to = Maybe.ambArray(new Maybe<Integer>() {
            @Override
            protected void subscribeActual(
                    MaybeObserver<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onSuccess(1);
                observer.onSuccess(2);
                observer.onComplete();
            }
        }, Maybe.<Integer>never())
        .test();

        to.dispose();

        to.assertResult(1);
    }

    @Test
    public void noWinnerSuccessDispose() throws Exception {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Maybe.ambArray(
                Maybe.just(1)
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Maybe.never()
            )
            .subscribe(new Consumer<Object>() {
                @Override
                public void accept(Object v) throws Exception {
                    interrupted.set(Thread.currentThread().isInterrupted());
                    cdl.countDown();
                }
            });

            assertTrue(cdl.await(500, TimeUnit.SECONDS));
            assertFalse("Interrupted!", interrupted.get());
        }
    }

    @Test
    public void noWinnerErrorDispose() throws Exception {
        final TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final AtomicBoolean interrupted = new AtomicBoolean();
            final CountDownLatch cdl = new CountDownLatch(1);

            Maybe.ambArray(
                Maybe.error(ex)
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Maybe.never()
            )
            .subscribe(Functions.emptyConsumer(), new Consumer<Throwable>() {
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

            Maybe.ambArray(
                Maybe.empty()
                    .subscribeOn(Schedulers.single())
                    .observeOn(Schedulers.computation()),
                Maybe.never()
            )
            .subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), new Action() {
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
    public void nullSourceSuccessRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {

                final Subject<Integer> ps = ReplaySubject.create();
                ps.onNext(1);

                final Maybe<Integer> source = Maybe.ambArray(ps.singleElement(),
                        Maybe.<Integer>never(), Maybe.<Integer>never(), null);

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
    public void maybeSourcesInIterable() {
        MaybeSource<Integer> source = new MaybeSource<Integer>() {
            @Override
            public void subscribe(MaybeObserver<? super Integer> observer) {
                Maybe.just(1).subscribe(observer);
            }
        };

        Maybe.amb(Arrays.asList(source, source))
        .test()
        .assertResult(1);
    }
}
