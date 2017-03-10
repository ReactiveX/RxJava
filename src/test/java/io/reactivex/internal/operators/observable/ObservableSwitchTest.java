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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.PublishSubject;

public class ObservableSwitchTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private Observer<String> observer;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        observer = TestHelper.mockObserver();
    }

    @Test
    public void testSwitchWhenOuterCompleteBeforeInner() {
        Observable<Observable<String>> source = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {
            @Override
            public void subscribe(Observer<? super Observable<String>> outerObserver) {
                outerObserver.onSubscribe(Disposables.empty());
                publishNext(outerObserver, 50, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 70, "one");
                        publishNext(innerObserver, 100, "two");
                        publishCompleted(innerObserver, 200);
                    }
                }));
                publishCompleted(outerObserver, 60);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(2)).onNext(anyString());
        inOrder.verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSwitchWhenInnerCompleteBeforeOuter() {
        Observable<Observable<String>> source = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {
            @Override
            public void subscribe(Observer<? super Observable<String>> outerObserver) {
                outerObserver.onSubscribe(Disposables.empty());
                publishNext(outerObserver, 10, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 0, "one");
                        publishNext(innerObserver, 10, "two");
                        publishCompleted(innerObserver, 20);
                    }
                }));

                publishNext(outerObserver, 100, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 0, "three");
                        publishNext(innerObserver, 10, "four");
                        publishCompleted(innerObserver, 20);
                    }
                }));
                publishCompleted(outerObserver, 200);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(150, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onComplete();
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        inOrder.verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSwitchWithComplete() {
        Observable<Observable<String>> source = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {
            @Override
            public void subscribe(Observer<? super Observable<String>> outerObserver) {
                outerObserver.onSubscribe(Disposables.empty());
                publishNext(outerObserver, 50, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(final Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 60, "one");
                        publishNext(innerObserver, 100, "two");
                    }
                }));

                publishNext(outerObserver, 200, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(final Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 0, "three");
                        publishNext(innerObserver, 100, "four");
                    }
                }));

                publishCompleted(outerObserver, 250);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("four");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithError() {
        Observable<Observable<String>> source = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {
            @Override
            public void subscribe(Observer<? super Observable<String>> outerObserver) {
                outerObserver.onSubscribe(Disposables.empty());
                publishNext(outerObserver, 50, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(final Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 50, "one");
                        publishNext(innerObserver, 100, "two");
                    }
                }));

                publishNext(outerObserver, 200, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 0, "three");
                        publishNext(innerObserver, 100, "four");
                    }
                }));

                publishError(outerObserver, 250, new TestException());
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testSwitchWithSubsequenceComplete() {
        Observable<Observable<String>> source = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {
            @Override
            public void subscribe(Observer<? super Observable<String>> outerObserver) {
                outerObserver.onSubscribe(Disposables.empty());
                publishNext(outerObserver, 50, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 50, "one");
                        publishNext(innerObserver, 100, "two");
                    }
                }));

                publishNext(outerObserver, 130, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishCompleted(innerObserver, 0);
                    }
                }));

                publishNext(outerObserver, 150, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 50, "three");
                    }
                }));
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithSubsequenceError() {
        Observable<Observable<String>> source = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {
            @Override
            public void subscribe(Observer<? super Observable<String>> observer) {
                observer.onSubscribe(Disposables.empty());
                publishNext(observer, 50, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> observer) {
                        observer.onSubscribe(Disposables.empty());
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> observer) {
                        observer.onSubscribe(Disposables.empty());
                        publishError(observer, 0, new TestException());
                    }
                }));

                publishNext(observer, 150, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> observer) {
                        observer.onSubscribe(Disposables.empty());
                        publishNext(observer, 50, "three");
                    }
                }));

            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext("three");
        verify(observer, never()).onComplete();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    private <T> void publishCompleted(final Observer<T> observer, long delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Observer<T> observer, long delay, final Throwable error) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Observer<T> observer, long delay, final T value) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSwitchIssue737() {
        // https://github.com/ReactiveX/RxJava/issues/737
        Observable<Observable<String>> source = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {
            @Override
            public void subscribe(Observer<? super Observable<String>> outerObserver) {
                outerObserver.onSubscribe(Disposables.empty());
                publishNext(outerObserver, 0, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 10, "1-one");
                        publishNext(innerObserver, 20, "1-two");
                        // The following events will be ignored
                        publishNext(innerObserver, 30, "1-three");
                        publishCompleted(innerObserver, 40);
                    }
                }));
                publishNext(outerObserver, 25, Observable.unsafeCreate(new ObservableSource<String>() {
                    @Override
                    public void subscribe(Observer<? super String> innerObserver) {
                        innerObserver.onSubscribe(Disposables.empty());
                        publishNext(innerObserver, 10, "2-one");
                        publishNext(innerObserver, 20, "2-two");
                        publishNext(innerObserver, 30, "2-three");
                        publishCompleted(innerObserver, 40);
                    }
                }));
                publishCompleted(outerObserver, 30);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("1-one");
        inOrder.verify(observer, times(1)).onNext("1-two");
        inOrder.verify(observer, times(1)).onNext("2-one");
        inOrder.verify(observer, times(1)).onNext("2-two");
        inOrder.verify(observer, times(1)).onNext("2-three");
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testUnsubscribe() {
        final AtomicBoolean isUnsubscribed = new AtomicBoolean();
        Observable.switchOnNext(
                Observable.unsafeCreate(new ObservableSource<Observable<Integer>>() {
                    @Override
                    public void subscribe(final Observer<? super Observable<Integer>> observer) {
                        Disposable bs = Disposables.empty();
                        observer.onSubscribe(bs);
                        observer.onNext(Observable.just(1));
                        isUnsubscribed.set(bs.isDisposed());
                    }
                })
        ).take(1).subscribe();
        assertTrue("Switch doesn't propagate 'unsubscribe'", isUnsubscribed.get());
    }
    /** The upstream producer hijacked the switch producer stopping the requests aimed at the inner observables. */
    @Test
    public void testIssue2654() {
        Observable<String> oneItem = Observable.just("Hello").mergeWith(Observable.<String>never());

        Observable<String> src = oneItem.switchMap(new Function<String, Observable<String>>() {
            @Override
            public Observable<String> apply(final String s) {
                return Observable.just(s)
                        .mergeWith(Observable.interval(10, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, String>() {
                            @Override
                            public String apply(Long i) {
                                return s + " " + i;
                            }
                        })).take(250);
            }
        })
        .share()
        ;

        TestObserver<String> ts = new TestObserver<String>() {
            @Override
            public void onNext(String t) {
                super.onNext(t);
                if (valueCount() == 250) {
                    onComplete();
                    dispose();
                }
            }
        };
        src.subscribe(ts);

        ts.awaitTerminalEvent(10, TimeUnit.SECONDS);

        System.out.println("> testIssue2654: " + ts.valueCount());

        ts.assertTerminated();
        ts.assertNoErrors();

        Assert.assertEquals(250, ts.valueCount());
    }


    @Test
    public void delayErrors() {
        PublishSubject<ObservableSource<Integer>> source = PublishSubject.create();

        TestObserver<Integer> ts = source.switchMapDelayError(Functions.<ObservableSource<Integer>>identity())
        .test();

        ts.assertNoValues()
        .assertNoErrors()
        .assertNotComplete();

        source.onNext(Observable.just(1));

        source.onNext(Observable.<Integer>error(new TestException("Forced failure 1")));

        source.onNext(Observable.just(2, 3, 4));

        source.onNext(Observable.<Integer>error(new TestException("Forced failure 2")));

        source.onNext(Observable.just(5));

        source.onError(new TestException("Forced failure 3"));

        ts.assertValues(1, 2, 3, 4, 5)
        .assertNotComplete()
        .assertError(CompositeException.class);

        List<Throwable> errors = ExceptionHelper.flatten(ts.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Forced failure 1");
        TestHelper.assertError(errors, 1, TestException.class, "Forced failure 2");
        TestHelper.assertError(errors, 2, TestException.class, "Forced failure 3");
    }

    @Test
    public void switchOnNextDelayError() {
        PublishSubject<Observable<Integer>> ps = PublishSubject.create();

        TestObserver<Integer> ts = Observable.switchOnNextDelayError(ps).test();

        ps.onNext(Observable.just(1));
        ps.onNext(Observable.range(2, 4));
        ps.onComplete();

        ts.assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void switchOnNextDelayErrorWithError() {
        PublishSubject<Observable<Integer>> ps = PublishSubject.create();

        TestObserver<Integer> ts = Observable.switchOnNextDelayError(ps).test();

        ps.onNext(Observable.just(1));
        ps.onNext(Observable.<Integer>error(new TestException()));
        ps.onNext(Observable.range(2, 4));
        ps.onComplete();

        ts.assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void switchOnNextDelayErrorBufferSize() {
        PublishSubject<Observable<Integer>> ps = PublishSubject.create();

        TestObserver<Integer> ts = Observable.switchOnNextDelayError(ps, 2).test();

        ps.onNext(Observable.just(1));
        ps.onNext(Observable.range(2, 4));
        ps.onComplete();

        ts.assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void switchMapDelayErrorEmptySource() {
        assertSame(Observable.empty(), Observable.<Object>empty()
                .switchMapDelayError(new Function<Object, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Object v) throws Exception {
                        return Observable.just(1);
                    }
                }, 16));
    }

    @Test
    public void switchMapDelayErrorJustSource() {
        Observable.just(0)
        .switchMapDelayError(new Function<Object, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Object v) throws Exception {
                return Observable.just(1);
            }
        }, 16)
        .test()
        .assertResult(1);
    }

    @Test
    public void switchMapErrorEmptySource() {
        assertSame(Observable.empty(), Observable.<Object>empty()
                .switchMap(new Function<Object, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Object v) throws Exception {
                        return Observable.just(1);
                    }
                }, 16));
    }

    @Test
    public void switchMapJustSource() {
        Observable.just(0)
        .switchMap(new Function<Object, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Object v) throws Exception {
                return Observable.just(1);
            }
        }, 16)
        .test()
        .assertResult(1);

    }


    @Test
    public void switchMapInnerCancelled() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Integer> ts = Observable.just(1)
                .switchMap(Functions.justFunction(pp))
                .test();

        assertTrue(pp.hasObservers());

        ts.cancel();

        assertFalse(pp.hasObservers());
    }

    @Test
    public void switchMapSingleJustSource() {
        Observable.just(0)
        .switchMapSingle(new Function<Object, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Object v) throws Exception {
                return Single.just(1);
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void switchMapSingleMapperReturnsNull() {
        Observable.just(0)
        .switchMapSingle(new Function<Object, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Object v) throws Exception {
                return null;
            }
        })
        .test()
        .assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void switchMapSingleMapperIsNull() {
        Observable.just(0)
        .switchMapSingle(null);
    }

    @Test
    public void switchMapSingleFunctionDoesntReturnSingle() {
        Observable.just(0)
        .switchMapSingle(new Function<Object, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Object v) throws Exception {
                return new SingleSource<Integer>() {
                    @Override
                    public void subscribe(SingleObserver<? super Integer> s) {
                        s.onSubscribe(Disposables.empty());
                        s.onSuccess(1);
                    }
                };
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void switchMapSingleDelayErrorJustSource() {
        final AtomicBoolean completed = new AtomicBoolean();
        Observable.just(0, 1)
        .switchMapSingleDelayError(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                if (v == 0) {
                    return Single.error(new RuntimeException());
                } else {
                    return Single.just(1).doOnSuccess(new Consumer<Integer>() {

                        @Override
                        public void accept(Integer n) throws Exception {
                            completed.set(true);
                        }});
                }
            }
        })
        .test()
        .assertValue(1)
        .assertError(RuntimeException.class);
        assertTrue(completed.get());
    }

    @Test
    public void scalarMap() {
        Observable.switchOnNext(Observable.just(Observable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void scalarMapDelayError() {
        Observable.switchOnNextDelayError(Observable.just(Observable.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.switchOnNext(
                Observable.just(Observable.just(1)).hide()));
    }

    @Test
    public void nextSourceErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {

                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                ps1.switchMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer v) throws Exception {
                        if (v == 1) {
                            return ps2;
                        }
                        return Observable.never();
                    }
                })
                .test();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onNext(2);
                    }
                };

                final TestException ex = new TestException();

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex);
                    }
                };

                TestHelper.race(r1, r2);

                for (Throwable e : errors) {
                    assertTrue(e.toString(), e instanceof TestException);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void outerInnerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {

                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                ps1.switchMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer v) throws Exception {
                        if (v == 1) {
                            return ps2;
                        }
                        return Observable.never();
                    }
                })
                .test();

                final TestException ex1 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };

                final TestException ex2 = new TestException();

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2);

                for (Throwable e : errors) {
                    assertTrue(e.toString(), e instanceof TestException);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void nextCancelRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Integer> ps1 = PublishSubject.create();

            final TestObserver<Integer> to = ps1.switchMap(new Function<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> apply(Integer v) throws Exception {
                    return Observable.never();
                }
            })
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps1.onNext(2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void mapperThrows() {
        Observable.just(1).hide()
        .switchMap(new Function<Integer, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badMainSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onComplete();
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .switchMap(Functions.justFunction(Observable.never()))
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void emptyInner() {
        Observable.range(1, 5)
        .switchMap(Functions.justFunction(Observable.empty()))
        .test()
        .assertResult();
    }

    @Test
    public void justInner() {
        Observable.range(1, 5)
        .switchMap(Functions.justFunction(Observable.just(1)))
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void badInnerSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.just(1).hide()
            .switchMap(Functions.justFunction(new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new TestException());
                    observer.onComplete();
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }))
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void innerCompletesReentrant() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ps.onComplete();
            }
        };

        Observable.just(1).hide()
        .switchMap(Functions.justFunction(ps))
        .subscribe(to);

        ps.onNext(1);

        to.assertResult(1);
    }

    @Test
    public void innerErrorsReentrant() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ps.onError(new TestException());
            }
        };

        Observable.just(1).hide()
        .switchMap(Functions.justFunction(ps))
        .subscribe(to);

        ps.onNext(1);

        to.assertFailure(TestException.class, 1);
    }
}
