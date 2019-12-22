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

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.SingleSubject;

public class ObservableConcatWithSingleTest extends RxJavaTest {

    @Test
    public void normal() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.range(1, 5)
        .concatWith(Single.just(100))
        .subscribe(to);

        to.assertResult(1, 2, 3, 4, 5, 100);
    }

    @Test
    public void mainError() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.<Integer>error(new TestException())
        .concatWith(Single.just(100))
        .subscribe(to);

        to.assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.range(1, 5)
        .concatWith(Single.<Integer>error(new TestException()))
        .subscribe(to);

        to.assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void takeMain() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.range(1, 5)
        .concatWith(Single.just(100))
        .take(3)
        .subscribe(to);

        to.assertResult(1, 2, 3);
    }

    @Test
    public void cancelOther() {
        SingleSubject<Object> other = SingleSubject.create();

        TestObserver<Object> to = Observable.empty()
                .concatWith(other)
                .test();

        assertTrue(other.hasObservers());

        to.dispose();

        assertFalse(other.hasObservers());
    }

    @Test
    public void consumerDisposed() {
        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                Disposable bs1 = Disposable.empty();
                observer.onSubscribe(bs1);

                assertFalse(((Disposable)observer).isDisposed());

                observer.onNext(1);

                assertTrue(((Disposable)observer).isDisposed());
                assertTrue(bs1.isDisposed());
            }
        }.concatWith(Single.just(100))
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void badSource() {
        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                Disposable bs1 = Disposable.empty();
                observer.onSubscribe(bs1);

                Disposable bs2 = Disposable.empty();
                observer.onSubscribe(bs2);

                assertFalse(bs1.isDisposed());
                assertTrue(bs2.isDisposed());

                observer.onComplete();
            }
        }.concatWith(Single.<Integer>just(100))
        .test()
        .assertResult(100);
    }

    @Test
    public void badSource2() {
        Flowable.empty().concatWith(new Single<Integer>() {
            @Override
            protected void subscribeActual(SingleObserver<? super Integer> observer) {
                Disposable bs1 = Disposable.empty();
                observer.onSubscribe(bs1);

                Disposable bs2 = Disposable.empty();
                observer.onSubscribe(bs2);

                assertFalse(bs1.isDisposed());
                assertTrue(bs2.isDisposed());

                observer.onSuccess(100);
            }
        })
        .test()
        .assertResult(100);
    }

}
