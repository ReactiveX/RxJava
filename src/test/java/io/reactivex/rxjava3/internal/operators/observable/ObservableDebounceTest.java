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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.observable.ObservableDebounceTimed.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableDebounceTest extends RxJavaTest {

    private TestScheduler scheduler;
    private Observer<String> observer;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        observer = TestHelper.mockObserver();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void debounceWithCompleted() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposable.empty());
                publishNext(observer, 100, "one");    // Should be skipped since "two" will arrive before the timeout expires.
                publishNext(observer, 400, "two");    // Should be published since "three" will arrive after the timeout expires.
                publishNext(observer, 900, "three");   // Should be skipped since onComplete will arrive before the timeout expires.
                publishCompleted(observer, 1000);     // Should be published as soon as the timeout expires.
            }
        });

        Observable<String> sampled = source.debounce(400, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        // must go to 800 since it must be 400 after when two is sent, which is at 400
        scheduler.advanceTimeTo(800, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void debounceNeverEmits() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposable.empty());
                // all should be skipped since they are happening faster than the 200ms timeout
                publishNext(observer, 100, "a");    // Should be skipped
                publishNext(observer, 200, "b");    // Should be skipped
                publishNext(observer, 300, "c");    // Should be skipped
                publishNext(observer, 400, "d");    // Should be skipped
                publishNext(observer, 500, "e");    // Should be skipped
                publishNext(observer, 600, "f");    // Should be skipped
                publishNext(observer, 700, "g");    // Should be skipped
                publishNext(observer, 800, "h");    // Should be skipped
                publishCompleted(observer, 900);     // Should be published as soon as the timeout expires.
            }
        });

        Observable<String> sampled = source.debounce(200, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(0)).onNext(anyString());
        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void debounceWithError() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposable.empty());
                Exception error = new TestException();
                publishNext(observer, 100, "one");    // Should be published since "two" will arrive after the timeout expires.
                publishNext(observer, 600, "two");    // Should be skipped since onError will arrive before the timeout expires.
                publishError(observer, 700, error);   // Should be published as soon as the timeout expires.
            }
        });

        Observable<String> sampled = source.debounce(400, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        // 100 + 400 means it triggers at 500
        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        inOrder.verify(observer).onNext("one");
        scheduler.advanceTimeTo(701, TimeUnit.MILLISECONDS);
        inOrder.verify(observer).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    private <T> void publishCompleted(final Observer<T> observer, long delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Observer<T> observer, long delay, final Exception error) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Observer<T> observer, final long delay, final T value) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void debounceSelectorNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> debouncer = PublishSubject.create();
        Function<Integer, Observable<Integer>> debounceSel = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return debouncer;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        source.debounce(debounceSel).subscribe(o);

        source.onNext(1);
        debouncer.onNext(1);

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);

        debouncer.onNext(2);

        source.onNext(5);
        source.onComplete();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(4);
        inOrder.verify(o).onNext(5);
        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void debounceSelectorFuncThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        Function<Integer, Observable<Integer>> debounceSel = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };

        Observer<Object> o = TestHelper.mockObserver();

        source.debounce(debounceSel).subscribe(o);

        source.onNext(1);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void debounceSelectorObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        Function<Integer, Observable<Integer>> debounceSel = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return Observable.error(new TestException());
            }
        };

        Observer<Object> o = TestHelper.mockObserver();

        source.debounce(debounceSel).subscribe(o);

        source.onNext(1);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void debounceTimedLastIsNotLost() {
        PublishSubject<Integer> source = PublishSubject.create();

        Observer<Object> o = TestHelper.mockObserver();

        source.debounce(100, TimeUnit.MILLISECONDS, scheduler).subscribe(o);

        source.onNext(1);
        source.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void debounceSelectorLastIsNotLost() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> debouncer = PublishSubject.create();

        Function<Integer, Observable<Integer>> debounceSel = new Function<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> apply(Integer t1) {
                return debouncer;
            }
        };

        Observer<Object> o = TestHelper.mockObserver();

        source.debounce(debounceSel).subscribe(o);

        source.onNext(1);
        source.onComplete();

        debouncer.onComplete();

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void debounceWithTimeBackpressure() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        TestObserverEx<Integer> observer = new TestObserverEx<>();

        Observable.merge(
                Observable.just(1),
                Observable.just(2).delay(10, TimeUnit.MILLISECONDS, scheduler)
        ).debounce(20, TimeUnit.MILLISECONDS, scheduler).take(1).subscribe(observer);

        scheduler.advanceTimeBy(30, TimeUnit.MILLISECONDS);

        observer.assertValue(2);
        observer.assertTerminated();
        observer.assertNoErrors();
    }

    @Test
    public void debounceDefault() throws Exception {

        Observable.just(1).debounce(1, TimeUnit.SECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().debounce(1, TimeUnit.SECONDS, new TestScheduler()));

        TestHelper.checkDisposed(PublishSubject.create().debounce(Functions.justFunction(Observable.never())));

        Disposable d = new ObservableDebounceTimed.DebounceEmitter<>(1, 1, null);
        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onComplete();
                    observer.onNext(1);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .debounce(1, TimeUnit.SECONDS, new TestScheduler())
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceSelector() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(Observable<Integer> o) throws Exception {
                return o.debounce(new Function<Integer, ObservableSource<Long>>() {
                    @Override
                    public ObservableSource<Long> apply(Integer v) throws Exception {
                        return Observable.timer(1, TimeUnit.SECONDS);
                    }
                });
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(final Observable<Integer> o) throws Exception {
                return Observable.just(1).debounce(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer v) throws Exception {
                        return o;
                    }
                });
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void debounceWithEmpty() {
        Observable.just(1).debounce(Functions.justFunction(Observable.empty()))
        .test()
        .assertResult(1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> o) throws Exception {
                return o.debounce(Functions.justFunction(Observable.never()));
            }
        });
    }

    @Test
    public void disposeInOnNext() {
        final TestObserver<Integer> to = new TestObserver<>();

        BehaviorSubject.createDefault(1)
        .debounce(new Function<Integer, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Integer o) throws Exception {
                to.dispose();
                return Observable.never();
            }
        })
        .subscribeWith(to)
        .assertEmpty();

        assertTrue(to.isDisposed());
    }

    @Test
    public void disposedInOnComplete() {
        final TestObserver<Integer> to = new TestObserver<>();

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                to.dispose();
                observer.onComplete();
            }
        }
        .debounce(Functions.justFunction(Observable.never()))
        .subscribeWith(to)
        .assertEmpty();
    }

    @Test
    public void emitLate() {
        final AtomicReference<Observer<? super Integer>> ref = new AtomicReference<>();

        TestObserver<Integer> to = Observable.range(1, 2)
        .debounce(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer o) throws Exception {
                if (o != 1) {
                    return Observable.never();
                }
                return new Observable<Integer>() {
                    @Override
                    protected void subscribeActual(Observer<? super Integer> observer) {
                        observer.onSubscribe(Disposable.empty());
                        ref.set(observer);
                    }
                };
            }
        })
        .test();

        ref.get().onNext(1);

        to
        .assertResult(2);
    }

    @Test
    public void timedDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f)
                    throws Exception {
                return f.debounce(1, TimeUnit.SECONDS);
            }
        });
    }

    @Test
    public void timedDisposedIgnoredBySource() {
        final TestObserver<Integer> to = new TestObserver<>();

        new Observable<Integer>() {
            @Override
            protected void subscribeActual(
                    Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                to.dispose();
                observer.onNext(1);
                observer.onComplete();
            }
        }
        .debounce(1, TimeUnit.SECONDS)
        .subscribe(to);
    }

    @Test
    public void timedLateEmit() {
        TestObserver<Integer> to = new TestObserver<>();
        DebounceTimedObserver<Integer> sub = new DebounceTimedObserver<>(
                to, 1, TimeUnit.SECONDS, new TestScheduler().createWorker());

        sub.onSubscribe(Disposable.empty());

        DebounceEmitter<Integer> de = new DebounceEmitter<>(1, 50, sub);
        de.run();
        de.run();

        to.assertEmpty();
    }

    @Test
    public void timedError() {
        Observable.error(new TestException())
        .debounce(1, TimeUnit.SECONDS)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void debounceOnEmpty() {
        Observable.empty().debounce(new Function<Object, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Object o) {
                return Observable.just(new Object());
            }
        }).subscribe();
    }

    @Test
    public void doubleOnSubscribeTime() {
        TestHelper.checkDoubleOnSubscribeObservable(o -> o.debounce(1, TimeUnit.SECONDS));
    }
}
