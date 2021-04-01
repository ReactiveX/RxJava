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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.testsupport.*;

public class CompletableMergeTest extends RxJavaTest {
    @Test
    public void invalidPrefetch() {
        try {
            Completable.merge(Flowable.just(Completable.complete()), -99);
            fail("Should have thrown IllegalArgumentExceptio");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxConcurrency > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void cancelAfterFirst() {
        final TestObserver<Void> to = new TestObserver<>();

        Completable.mergeArray(new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onComplete();
                to.dispose();
            }
        }, Completable.complete())
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void cancelAfterFirstDelayError() {
        final TestObserver<Void> to = new TestObserver<>();

        Completable.mergeArrayDelayError(new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onComplete();
                to.dispose();
            }
        }, Completable.complete())
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void onErrorAfterComplete() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final CompletableObserver[] co = { null };

            Completable.mergeArrayDelayError(Completable.complete(), new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onComplete();
                    co[0] = observer;
                }
            })
            .test()
            .assertResult();

            co[0].onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void completeAfterMain() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeArray(Completable.complete(), pp.ignoreElements())
        .test();

        pp.onComplete();

        to.assertResult();
    }

    @Test
    public void completeAfterMainDelayError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeArrayDelayError(Completable.complete(), pp.ignoreElements())
        .test();

        pp.onComplete();

        to.assertResult();
    }

    @Test
    public void errorAfterMainDelayError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeArrayDelayError(Completable.complete(), pp.ignoreElements())
        .test();

        pp.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Completable.merge(Flowable.just(Completable.complete())));
    }

    @Test
    public void disposePropagates() {

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.merge(Flowable.just(pp.ignoreElements())).test();

        assertTrue(pp.hasSubscribers());

        to.dispose();

        assertFalse(pp.hasSubscribers());

        to.assertEmpty();
    }

    @Test
    public void innerComplete() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.merge(Flowable.just(pp.ignoreElements())).test();

        pp.onComplete();

        to.assertResult();
    }

    @Test
    public void innerError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.merge(Flowable.just(pp.ignoreElements())).test();

        pp.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void innerErrorDelayError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeDelayError(Flowable.just(pp.ignoreElements())).test();

        pp.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorInnerErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestObserverEx<Void> to = Completable.merge(pp1.map(new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer v) throws Exception {
                        return pp2.ignoreElements();
                    }
                })).to(TestHelper.<Void>testConsumer());

                pp1.onNext(1);

                final Throwable ex1 = new TestException();
                final Throwable ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2);

                Throwable ex = to.errors().get(0);
                if (ex instanceof CompositeException) {
                    to.assertSubscribed().assertNoValues().assertNotComplete();

                    errors = TestHelper.compositeList(ex);
                    TestHelper.assertError(errors, 0, TestException.class);
                    TestHelper.assertError(errors, 1, TestException.class);
                } else {
                    to.assertFailure(TestException.class);

                    if (!errors.isEmpty()) {
                        TestHelper.assertUndeliverable(errors, 0, TestException.class);
                    }
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void mainErrorInnerErrorDelayedRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestObserverEx<Void> to = Completable.mergeDelayError(pp1.map(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    return pp2.ignoreElements();
                }
            })).to(TestHelper.<Void>testConsumer());

            pp1.onNext(1);

            final Throwable ex1 = new TestException();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onError(ex1);
                }
            };

            final Throwable ex2 = new TestException();
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onError(ex2);
                }
            };

            TestHelper.race(r1, r2);

            to.assertFailure(CompositeException.class);

            List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

            TestHelper.assertError(errors, 0, TestException.class);
            TestHelper.assertError(errors, 1, TestException.class);
        }
    }

    @Test
    public void maxConcurrencyOne() {
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Void> to = Completable.merge(Flowable.just(pp1.ignoreElements(), pp2.ignoreElements()), 1)
        .test();

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        pp1.onComplete();

        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        to.assertResult();
    }

    @Test
    public void maxConcurrencyOneDelayError() {
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeDelayError(Flowable.just(pp1.ignoreElements(), pp2.ignoreElements()), 1)
        .test();

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        pp1.onComplete();

        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        to.assertResult();
    }

    @Test
    public void maxConcurrencyOneDelayErrorFirst() {
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeDelayError(Flowable.just(pp1.ignoreElements(), pp2.ignoreElements()), 1)
        .test();

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        pp1.onError(new TestException());

        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void maxConcurrencyOneDelayMainErrors() {
        final PublishProcessor<PublishProcessor<Integer>> pp0 = PublishProcessor.create();
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Void> to = Completable.mergeDelayError(
        pp0.map(new Function<PublishProcessor<Integer>, Completable>() {
            @Override
            public Completable apply(PublishProcessor<Integer> v) throws Exception {
                return v.ignoreElements();
            }
        }), 1)
        .test();

        pp0.onNext(pp1);

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        pp1.onComplete();

        pp0.onNext(pp2);
        pp0.onError(new TestException());

        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void mainDoubleOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Completable.mergeDelayError(new Flowable<Completable>() {
                @Override
                protected void subscribeActual(Subscriber<? super Completable> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(Completable.complete());
                    s.onError(new TestException("First"));
                    s.onError(new TestException("Second"));
                }
            })
            .to(TestHelper.<Void>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void innerDoubleOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final CompletableObserver[] o = { null };
            Completable.mergeDelayError(Flowable.just(new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onError(new TestException("First"));
                    o[0] = observer;
                }
            }))
            .to(TestHelper.<Void>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            o[0].onError(new TestException("Second"));

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void innerIsDisposed() {
        final TestObserver<Void> to = new TestObserver<>();

        Completable.mergeDelayError(Flowable.just(new Completable() {
            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onSubscribe(Disposable.empty());
                assertFalse(((Disposable)observer).isDisposed());

                to.dispose();

                assertTrue(((Disposable)observer).isDisposed());
            }
        }))
        .subscribe(to);
    }

    @Test
    public void mergeArrayInnerErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestObserver<Void> to = Completable.mergeArray(pp1.ignoreElements(), pp2.ignoreElements()).test();

                pp1.onNext(1);

                final Throwable ex1 = new TestException();
                final Throwable ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp2.onError(ex2);
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
    public void delayErrorIterableCancel() {
        Completable.mergeDelayError(Arrays.asList(Completable.complete()))
        .test(true)
        .assertEmpty();
    }

    @Test
    public void delayErrorIterableCancelAfterHasNext() {
        final TestObserver<Void> to = new TestObserver<>();

        Completable.mergeDelayError(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return new Iterator<Completable>() {
                    @Override
                    public boolean hasNext() {
                        to.dispose();
                        return true;
                    }

                    @Override
                    public Completable next() {
                        return Completable.complete();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        })
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void delayErrorIterableCancelAfterNext() {
        final TestObserver<Void> to = new TestObserver<>();

        Completable.mergeDelayError(new Iterable<Completable>() {
            @Override
            public Iterator<Completable> iterator() {
                return new Iterator<Completable>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Completable next() {
                        to.dispose();
                        return Completable.complete();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        })
        .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void arrayUndeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Completable>() {
            @Override
            public Completable apply(Flowable<Integer> upstream) {
                return Completable.mergeArray(upstream.ignoreElements(), Completable.complete().hide());
            }
        });
    }

    @Test
    public void iterableUndeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Completable>() {
            @Override
            public Completable apply(Flowable<Integer> upstream) {
                return Completable.merge(Arrays.asList(upstream.ignoreElements(), Completable.complete().hide()));
            }
        });
    }

    @Test
    public void arrayUndeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Completable>() {
            @Override
            public Completable apply(Flowable<Integer> upstream) {
                return Completable.mergeArrayDelayError(upstream.ignoreElements(), Completable.complete().hide());
            }
        });
    }

    @Test
    public void iterableUndeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Completable>() {
            @Override
            public Completable apply(Flowable<Integer> upstream) {
                return Completable.mergeDelayError(Arrays.asList(upstream.ignoreElements(), Completable.complete().hide()));
            }
        });
    }

    @Test
    public void iterableCompleteLater() {
        CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = Completable.mergeDelayError(Arrays.asList(cs, cs, cs))
        .test();

        to.assertEmpty();

        cs.onComplete();

        to.assertResult();
    }

    @Test
    public void terminalDisposed() {
        TestHelper.checkDisposed(new CompletableMergeArrayDelayError.TryTerminateAndReportDisposable(new AtomicThrowable()));
    }

    @Test
    public void innerDisposed() {
        TestHelper.checkDisposed(new CompletableMergeArray.InnerCompletableObserver(new TestObserver<Void>(), new AtomicBoolean(), new CompositeDisposable(), 1));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.<Completable>checkDoubleOnSubscribeFlowableToCompletable(f -> Completable.merge(f));
    }
}
