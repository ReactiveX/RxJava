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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableWindowWithStartEndObservableTest extends RxJavaTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void observableBasedOpenerAndCloser() {
        final List<String> list = new ArrayList<>();
        final List<List<String>> lists = new ArrayList<>();

        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> innerObserver) {
                innerObserver.onSubscribe(Disposable.empty());
                push(innerObserver, "one", 10);
                push(innerObserver, "two", 60);
                push(innerObserver, "three", 110);
                push(innerObserver, "four", 160);
                push(innerObserver, "five", 210);
                complete(innerObserver, 500);
            }
        });

        Observable<Object> openings = Observable.unsafeCreate(new ObservableSource<Object>() {
            @Override
            public void subscribe(Observer<? super Object> innerObserver) {
                innerObserver.onSubscribe(Disposable.empty());
                push(innerObserver, new Object(), 50);
                push(innerObserver, new Object(), 200);
                complete(innerObserver, 250);
            }
        });

        Function<Object, Observable<Object>> closer = new Function<Object, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Object opening) {
                return Observable.unsafeCreate(new ObservableSource<Object>() {
                    @Override
                    public void subscribe(Observer<? super Object> innerObserver) {
                        innerObserver.onSubscribe(Disposable.empty());
                        push(innerObserver, new Object(), 100);
                        complete(innerObserver, 101);
                    }
                });
            }
        };

        Observable<Observable<String>> windowed = source.window(openings, closer);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        assertEquals(2, lists.size());
        assertEquals(lists.get(0), list("two", "three"));
        assertEquals(lists.get(1), list("five"));
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    private <T> void push(final Observer<T> observer, final T value, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Observer<?> observer, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private Consumer<Observable<String>> observeWindow(final List<String> list, final List<List<String>> lists) {
        return new Consumer<Observable<String>>() {
            @Override
            public void accept(Observable<String> stringObservable) {
                stringObservable.subscribe(new DefaultObserver<String>() {
                    @Override
                    public void onComplete() {
                        lists.add(new ArrayList<>(list));
                        list.clear();
                    }

                    @Override
                    public void onError(Throwable e) {
                        fail(e.getMessage());
                    }

                    @Override
                    public void onNext(String args) {
                        list.add(args);
                    }
                });
            }
        };
    }

    @Test
    public void noUnsubscribeAndNoLeak() {
        PublishSubject<Integer> source = PublishSubject.create();

        PublishSubject<Integer> open = PublishSubject.create();
        final PublishSubject<Integer> close = PublishSubject.create();

        TestObserver<Observable<Integer>> to = new TestObserver<>();

        source.window(open, new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                return close;
            }
        })
        .doOnNext(new Consumer<Observable<Integer>>() {
            @Override
            public void accept(Observable<Integer> w) throws Throwable {
                w.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()); // avoid abandonment
            }
        })
        .subscribe(to);

        open.onNext(1);
        source.onNext(1);

        assertTrue(open.hasObservers());
        assertTrue(close.hasObservers());

        close.onNext(1);

        assertFalse(close.hasObservers());

        source.onComplete();

        to.assertComplete();
        to.assertNoErrors();
        to.assertValueCount(1);

        // 2.0.2 - not anymore
//        assertTrue("Not cancelled!", ts.isCancelled());
        assertFalse(open.hasObservers());
        assertFalse(close.hasObservers());
    }

    @Test
    public void unsubscribeAll() {
        PublishSubject<Integer> source = PublishSubject.create();

        PublishSubject<Integer> open = PublishSubject.create();
        final PublishSubject<Integer> close = PublishSubject.create();

        TestObserver<Observable<Integer>> to = new TestObserver<>();

        source.window(open, new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                return close;
            }
        })
        .doOnNext(new Consumer<Observable<Integer>>() {
            @Override
            public void accept(Observable<Integer> w) throws Throwable {
                w.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()); // avoid abandonment
            }
        })
        .subscribe(to);

        open.onNext(1);

        assertTrue(open.hasObservers());
        assertTrue(close.hasObservers());

        to.dispose();

        // Disposing the outer sequence stops the opening of new windows
        assertFalse(open.hasObservers());
        // FIXME subject has subscribers because of the open window
        assertTrue(close.hasObservers());
    }

    @Test
    public void boundarySelectorNormal() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> start = PublishSubject.create();
        final PublishSubject<Integer> end = PublishSubject.create();

        TestObserver<Integer> to = source.window(start, new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return end;
            }
        })
        .flatMap(Functions.<Observable<Integer>>identity())
        .test();

        start.onNext(0);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onNext(4);

        start.onNext(1);

        source.onNext(5);
        source.onNext(6);

        end.onNext(1);

        start.onNext(2);

        TestHelper.emit(source, 7, 8);

        to.assertResult(1, 2, 3, 4, 5, 5, 6, 6, 7, 8);
    }

    @Test
    public void startError() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> start = PublishSubject.create();
        final PublishSubject<Integer> end = PublishSubject.create();

        TestObserver<Integer> to = source.window(start, new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return end;
            }
        })
        .flatMap(Functions.<Observable<Integer>>identity())
        .test();

        start.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse("Source has observers!", source.hasObservers());
        assertFalse("Start has observers!", start.hasObservers());
        assertFalse("End has observers!", end.hasObservers());
    }

    @Test
    public void endError() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> start = PublishSubject.create();
        final PublishSubject<Integer> end = PublishSubject.create();

        TestObserver<Integer> to = source.window(start, new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return end;
            }
        })
        .flatMap(Functions.<Observable<Integer>>identity())
        .test();

        start.onNext(1);
        end.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse("Source has observers!", source.hasObservers());
        assertFalse("Start has observers!", start.hasObservers());
        assertFalse("End has observers!", end.hasObservers());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).window(Observable.just(2), Functions.justFunction(Observable.never())));
    }

    @Test
    public void reentrant() {
        final Subject<Integer> ps = PublishSubject.<Integer>create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(2);
                    ps.onComplete();
                }
            }
        };

        ps.window(BehaviorSubject.createDefault(1), Functions.justFunction(Observable.never()))
        .flatMap(new Function<Observable<Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) throws Exception {
                return v;
            }
        })
        .subscribe(to);

        ps.onNext(1);

        to
        .awaitDone(1, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void badSourceCallable() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Object>, Object>() {
            @Override
            public Object apply(Observable<Object> o) throws Exception {
                return o.window(Observable.just(1), Functions.justFunction(Observable.never()));
            }
        }, false, 1, 1, (Object[])null);
    }

    @Test
    public void windowCloseIngoresCancel() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            BehaviorSubject.createDefault(1)
            .window(BehaviorSubject.createDefault(1), new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer f) throws Exception {
                    return new Observable<Integer>() {
                        @Override
                        protected void subscribeActual(
                                Observer<? super Integer> observer) {
                            observer.onSubscribe(Disposable.empty());
                            observer.onNext(1);
                            observer.onNext(2);
                            observer.onError(new TestException());
                        }
                    };
                }
            })
            .doOnNext(new Consumer<Observable<Integer>>() {
                @Override
                public void accept(Observable<Integer> w) throws Throwable {
                    w.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()); // avoid abandonment
                }
            })
            .test()
            .assertValueCount(1)
            .assertNoErrors()
            .assertNotComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    static Observable<Integer> observableDisposed(final AtomicBoolean ref) {
        return Observable.just(1).concatWith(Observable.<Integer>never())
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        ref.set(true);
                    }
                });
    }

    @Test
    public void mainAndBoundaryDisposeOnNoWindows() {
        AtomicBoolean mainDisposed = new AtomicBoolean();
        AtomicBoolean openDisposed = new AtomicBoolean();
        final AtomicBoolean closeDisposed = new AtomicBoolean();

        observableDisposed(mainDisposed)
        .window(observableDisposed(openDisposed), new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return observableDisposed(closeDisposed);
            }
        })
        .doOnNext(new Consumer<Observable<Integer>>() {
            @Override
            public void accept(Observable<Integer> w) throws Throwable {
                w.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()); // avoid abandonment
            }
        })
        .to(TestHelper.<Observable<Integer>>testConsumer())
        .assertSubscribed()
        .assertNoErrors()
        .assertNotComplete()
        .dispose();

        assertTrue(mainDisposed.get());
        assertTrue(openDisposed.get());
        assertTrue(closeDisposed.get());
    }

    @Test
    public void cancellingWindowCancelsUpstream() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.window(Observable.just(1).concatWith(Observable.<Integer>never()), Functions.justFunction(Observable.never()))
        .take(1)
        .flatMap(new Function<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> w) throws Throwable {
                return w.take(1);
            }
        })
        .test();

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        to
        .assertResult(1);

        assertFalse("Subject still has observers!", ps.hasObservers());
    }

    @Test
    public void windowAbandonmentCancelsUpstream() {
        PublishSubject<Integer> ps = PublishSubject.create();

        final AtomicReference<Observable<Integer>> inner = new AtomicReference<>();

        TestObserver<Observable<Integer>> to = ps.window(Observable.<Integer>just(1).concatWith(Observable.<Integer>never()),
                Functions.justFunction(Observable.never()))
        .doOnNext(new Consumer<Observable<Integer>>() {
            @Override
            public void accept(Observable<Integer> v) throws Throwable {
                inner.set(v);
            }
        })
        .test();

        assertTrue(ps.hasObservers());

        to
        .assertValueCount(1)
        ;

        ps.onNext(1);

        assertTrue(ps.hasObservers());

        to.dispose();

        to
        .assertValueCount(1)
        .assertNoErrors()
        .assertNotComplete();

        assertFalse("Subject still has observers!", ps.hasObservers());

        inner.get().test().assertResult();
    }

    @Test
    public void closingIndicatorFunctionCrash() {

        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        TestObserver<Observable<Integer>> to = source.window(boundary, new Function<Integer, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Integer end) throws Throwable {
                throw new TestException();
            }
        })
        .test()
        ;

        to.assertEmpty();

        boundary.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(source.hasObservers());
        assertFalse(boundary.hasObservers());
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .window(Observable.never(), Functions.justFunction(Observable.never()))
        .test()
        .assertFailure(TestException.class);
    }
}
