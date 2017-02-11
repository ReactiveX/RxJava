/**
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.operators;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import rx.*;
import rx.CompletableTest.*;
import rx.Observable;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.util.UtilityFunctions;
import rx.observers.*;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class OnSubscribeFlatMapCompletableTest implements Action0, Action1<Object> {

    final AtomicInteger calls = new AtomicInteger();

    /** A normal Completable object. */
    final NormalCompletable normal = new NormalCompletable();

    /** An error Completable object. */
    final ErrorCompletable error = new ErrorCompletable();

    final Func1<Completable, Completable> identity = UtilityFunctions.identity();

    @Override
    public void call() {
        calls.getAndIncrement();
    }

    @Override
    public void call(Object t) {
        calls.getAndIncrement();
    }

    void assertCalls(int n) {
        assertEquals(n, calls.get());
    }

    @Test
    public void normal() {
        Observable.range(1, 10)
        .flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return Completable.complete().doOnCompleted(OnSubscribeFlatMapCompletableTest.this);
            }
        })
        .test()
        .assertResult();

        assertCalls(10);
    }

    @Test
    public void normalMaxConcurrent() {
        for (int i = 1; i < 10; i++) {
            calls.set(0);

            Observable.range(1, 10)
            .flatMapCompletable(new Func1<Integer, Completable>() {
                @Override
                public Completable call(Integer v) {
                    return Completable.complete()
                            .observeOn(Schedulers.computation())
                            .doOnCompleted(OnSubscribeFlatMapCompletableTest.this);
                }
            }, false, i)
            .test()
            .awaitTerminalEvent(5, TimeUnit.SECONDS)
            .assertResult();

            assertCalls(10);
        }
    }

    @Test
    public void error() {
        Observable.range(1, 10)
        .flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return Completable.error(new TestException()).doOnError(OnSubscribeFlatMapCompletableTest.this);
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertCalls(1);
    }

    @Test
    public void errorDelayed() {
        AssertableSubscriber<Integer> as = Observable.range(1, 10)
        .flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return Completable.error(new TestException()).doOnError(OnSubscribeFlatMapCompletableTest.this);
            }
        }, true)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> onErrorEvents = as.getOnErrorEvents();

        assertEquals(onErrorEvents.toString(), 1, onErrorEvents.size());

        onErrorEvents = ((CompositeException)onErrorEvents.get(0)).getExceptions();

        assertEquals(onErrorEvents.toString(), 10, onErrorEvents.size());

        for (Throwable ex : onErrorEvents) {
            assertTrue(ex.toString(), ex instanceof TestException);
        }

        assertCalls(10);
    }

    @Test
    public void errorDelayedMaxConcurrency() {
        AssertableSubscriber<Integer> as = Observable.range(1, 10)
        .flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return Completable.error(new TestException()).doOnError(OnSubscribeFlatMapCompletableTest.this);
            }
        }, true, 1)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> onErrorEvents = as.getOnErrorEvents();

        assertEquals(onErrorEvents.toString(), 1, onErrorEvents.size());

        onErrorEvents = ((CompositeException)onErrorEvents.get(0)).getExceptions();

        assertEquals(onErrorEvents.toString(), 10, onErrorEvents.size());

        for (Throwable ex : onErrorEvents) {
            assertTrue(ex.toString(), ex instanceof TestException);
        }

        assertCalls(10);
    }

    @Test
    public void mapperThrows() {
        Observable.range(1, 10)
        .flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperNull() {
        Observable.range(1, 10)
        .flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void paramValidation() {
        try {
            Observable.range(1, 10)
            .flatMapCompletable(null);
            fail("Should have thrown");
        } catch (NullPointerException ex) {
            assertEquals("mapper is null", ex.getMessage());
        }

        try {
            Observable.range(1, 10)
            .flatMapCompletable(new Func1<Integer, Completable>() {
                @Override
                public Completable call(Integer v) {
                    return Completable.complete();
                }
            }, false, 0);
            fail("Should have thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxConcurrency > 0 required but it was 0", ex.getMessage());
        }

        try {
            Observable.range(1, 10)
            .flatMapCompletable(new Func1<Integer, Completable>() {
                @Override
                public Completable call(Integer v) {
                    return Completable.complete();
                }
            }, true, -99);
            fail("Should have thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxConcurrency > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void mainErrorDelayed() {
        Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
        .flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return Completable.complete().doOnCompleted(OnSubscribeFlatMapCompletableTest.this);
            }
        }, true)
        .test()
        .assertFailure(TestException.class);

        assertCalls(10);
    }

    @Test
    public void innerDoubleOnSubscribe() {
        final CompletableSubscriber[] inner = { null };

        AssertableSubscriber<Integer> as = Observable.just(1)
        .flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer t) {
                return Completable.create(new Completable.OnSubscribe() {
                    @Override
                    public void call(CompletableSubscriber t) {
                        OnSubscribeFlatMapCompletableTest.this.call();
                        Subscription s1 = Subscriptions.empty();

                        t.onSubscribe(s1);

                        Subscription s2 = Subscriptions.empty();

                        t.onSubscribe(s2);

                        if (s2.isUnsubscribed()) {
                            OnSubscribeFlatMapCompletableTest.this.call();
                        }

                        t.onCompleted();

                        inner[0] = t;
                    }
                });
            }
        })
        .test()
        .assertResult();

        assertCalls(2);

        inner[0].onError(new TestException());

        as.assertResult();
    }

    @Test
    public void mainErrorUnsubscribes() {
        PublishSubject<Integer> ps0 = PublishSubject.create();
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestSubscriber<Integer> as = TestSubscriber.create();

        ps0.flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return v == 0 ? ps1.toCompletable() : ps2.toCompletable();
            }
        }).unsafeSubscribe(as);

        assertTrue(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(0);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(1);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onError(new TestException());

        assertFalse(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertError(TestException.class);
        as.assertNotCompleted();
    }

    @Test
    public void innerErrorUnsubscribes() {
        PublishSubject<Integer> ps0 = PublishSubject.create();
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestSubscriber<Integer> as = TestSubscriber.create();

        ps0.flatMapCompletable(new Func1<Integer, Completable>() {
            @Override
            public Completable call(Integer v) {
                return v == 0 ? ps1.toCompletable() : ps2.toCompletable();
            }
        }).unsafeSubscribe(as);

        assertTrue(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(0);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(1);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps1.onError(new TestException());

        assertFalse(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertError(TestException.class);
        as.assertNotCompleted();
    }


    @Test(timeout = 5000)
    public void mergeObservableEmpty() {
        Completable c = Observable.<Completable>empty().flatMapCompletable(identity).toCompletable();

        c.await();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeObservableError() {
        Completable c = Observable.<Completable>error(new TestException()).flatMapCompletable(identity).toCompletable();

        c.await();
    }

    @Test(timeout = 5000)
    public void mergeObservableSingle() {
        Completable c = Observable.just(normal.completable).flatMapCompletable(identity).toCompletable();

        c.await();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeObservableSingleThrows() {
        Completable c = Observable.just(error.completable).flatMapCompletable(identity).toCompletable();

        c.await();
    }

    @Test(timeout = 5000)
    public void mergeObservableMany() {
        Completable c = Observable.just(normal.completable).repeat(3).flatMapCompletable(identity).toCompletable();

        c.await();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeObservableManyOneThrows() {
        Completable c = Observable.just(normal.completable, error.completable).flatMapCompletable(identity).toCompletable();

        c.await();
    }

    @Test(timeout = 5000)
    public void mergeObservableMaxConcurrent() {
        final List<Long> requested = new ArrayList<Long>();
        Observable<Completable> cs = Observable
                .just(normal.completable)
                .repeat(10)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long v) {
                        requested.add(v);
                    }
                });

        Completable c = cs.flatMapCompletable(identity, false, 5).toCompletable();

        c.await();

        // FIXME this request pattern looks odd because all 10 completions trigger 1 requests
        Assert.assertEquals(Arrays.asList(5L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), requested);
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorObservableEmpty() {
        Completable c = Observable.<Completable>empty().flatMapCompletable(identity, true).toCompletable();

        c.await();
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeDelayErrorObservableError() {
        Completable c = Observable.<Completable>error(new TestException()).flatMapCompletable(identity, true).toCompletable();

        c.await();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorObservableSingle() {
        Completable c = Observable.just(normal.completable).flatMapCompletable(identity, true).toCompletable();

        c.await();

        normal.assertSubscriptions(1);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeDelayErrorObservableSingleThrows() {
        Completable c = Observable.just(error.completable).flatMapCompletable(identity, true).toCompletable();

        c.await();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorObservableMany() {
        Completable c = Observable.just(normal.completable).repeat(3).flatMapCompletable(identity, true).toCompletable();

        c.await();

        normal.assertSubscriptions(3);
    }

    @Test(timeout = 5000, expected = TestException.class)
    public void mergeDelayErrorObservableManyOneThrows() {
        Completable c = Observable.just(normal.completable, error.completable).flatMapCompletable(identity, true).toCompletable();

        c.await();
    }

    @Test(timeout = 5000)
    public void mergeDelayErrorObservableMaxConcurrent() {
        final List<Long> requested = new ArrayList<Long>();
        Observable<Completable> cs = Observable
                .just(normal.completable)
                .repeat(10)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long v) {
                        requested.add(v);
                    }
                });

        Completable c = cs.flatMapCompletable(identity, true, 5).toCompletable();

        c.await();

        // FIXME this request pattern looks odd because all 10 completions trigger 1 requests
        Assert.assertEquals(Arrays.asList(5L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), requested);
    }

    @Test
    public void asyncObservables() {

        final int[] calls = { 0 };

        Observable.range(1, 5).map(new Func1<Integer, Completable>() {
            @Override
            public Completable call(final Integer v) {
                System.out.println("Mapping " + v);
                return Completable.fromAction(new Action0() {
                    @Override
                    public void call() {
                        System.out.println("Processing " + (calls[0] + 1));
                        calls[0]++;
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        System.out.println("Inner complete " + v);
                    }
                })
                .observeOn(Schedulers.computation());
            }
        }).flatMapCompletable(identity, false, 1).toCompletable()
        .test()
        .awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(5, calls[0]);
    }

}
