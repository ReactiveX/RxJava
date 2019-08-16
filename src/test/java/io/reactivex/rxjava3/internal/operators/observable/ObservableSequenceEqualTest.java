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

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.BiPredicate;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableSequenceEqualTest extends RxJavaTest {

    @Test
    public void observable1() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.just("one", "two", "three")).toObservable();
        verifyResult(o, true);
    }

    @Test
    public void observable2() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.just("one", "two", "three", "four")).toObservable();
        verifyResult(o, false);
    }

    @Test
    public void observable3() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three", "four"),
                Observable.just("one", "two", "three")).toObservable();
        verifyResult(o, false);
    }

    @Test
    public void withError1Observable() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())),
                Observable.just("one", "two", "three")).toObservable();
        verifyError(o);
    }

    @Test
    public void withError2Observable() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException()))).toObservable();
        verifyError(o);
    }

    @Test
    public void withError3Observable() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())),
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException()))).toObservable();
        verifyError(o);
    }

    @Test
    public void withEmpty1Observable() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.<String> empty(),
                Observable.just("one", "two", "three")).toObservable();
        verifyResult(o, false);
    }

    @Test
    public void withEmpty2Observable() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.<String> empty()).toObservable();
        verifyResult(o, false);
    }

    @Test
    public void withEmpty3Observable() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.<String> empty(), Observable.<String> empty()).toObservable();
        verifyResult(o, true);
    }

    @Test
    public void withEqualityErrorObservable() {
        Observable<Boolean> o = Observable.sequenceEqual(
                Observable.just("one"), Observable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                }).toObservable();
        verifyError(o);
    }

    private void verifyResult(Single<Boolean> o, boolean result) {
        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(result);
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Observable<Boolean> observable) {
        Observer<Boolean> observer = TestHelper.mockObserver();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    private void verifyError(Single<Boolean> single) {
        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(isA(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void prefetchObservable() {
        Observable.sequenceEqual(Observable.range(1, 20), Observable.range(1, 20), 2)
        .toObservable()
        .test()
        .assertResult(true);
    }

    @Test
    public void disposedObservable() {
        TestHelper.checkDisposed(Observable.sequenceEqual(Observable.just(1), Observable.just(2)).toObservable());
    }

    @Test
    public void one() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.just("one", "two", "three"));
        verifyResult(o, true);
    }

    @Test
    public void two() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.just("one", "two", "three", "four"));
        verifyResult(o, false);
    }

    @Test
    public void three() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three", "four"),
                Observable.just("one", "two", "three"));
        verifyResult(o, false);
    }

    @Test
    public void withError1() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())),
                Observable.just("one", "two", "three"));
        verifyError(o);
    }

    @Test
    public void withError2() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())));
        verifyError(o);
    }

    @Test
    public void withError3() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())),
                Observable.concat(Observable.just("one"),
                        Observable.<String> error(new TestException())));
        verifyError(o);
    }

    @Test
    public void withEmpty1() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.<String> empty(),
                Observable.just("one", "two", "three"));
        verifyResult(o, false);
    }

    @Test
    public void withEmpty2() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.just("one", "two", "three"),
                Observable.<String> empty());
        verifyResult(o, false);
    }

    @Test
    public void withEmpty3() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.<String> empty(), Observable.<String> empty());
        verifyResult(o, true);
    }

    @Test
    public void withEqualityError() {
        Single<Boolean> o = Observable.sequenceEqual(
                Observable.just("one"), Observable.just("one"),
                new BiPredicate<String, String>() {
                    @Override
                    public boolean test(String t1, String t2) {
                        throw new TestException();
                    }
                });
        verifyError(o);
    }

    private void verifyResult(Observable<Boolean> o, boolean result) {
        Observer<Boolean> observer = TestHelper.mockObserver();

        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(result);
        inOrder.verify(observer).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void prefetch() {
        Observable.sequenceEqual(Observable.range(1, 20), Observable.range(1, 20), 2)
        .test()
        .assertResult(true);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.sequenceEqual(Observable.just(1), Observable.just(2)));
    }

    @Test
    public void simpleInequal() {
        Observable.sequenceEqual(Observable.just(1), Observable.just(2))
        .test()
        .assertResult(false);
    }

    @Test
    public void simpleInequalObservable() {
        Observable.sequenceEqual(Observable.just(1), Observable.just(2))
        .toObservable()
        .test()
        .assertResult(false);
    }

    @Test
    public void onNextCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Boolean> to = Observable.sequenceEqual(Observable.never(), ps).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }

    @Test
    public void onNextCancelRaceObservable() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Boolean> to = Observable.sequenceEqual(Observable.never(), ps).toObservable().test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                }
            };

            TestHelper.race(r1, r2);

            to.assertEmpty();
        }
    }
}
