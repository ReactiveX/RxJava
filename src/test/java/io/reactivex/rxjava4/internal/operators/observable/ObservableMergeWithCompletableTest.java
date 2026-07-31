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

package io.reactivex.rxjava4.internal.operators.observable;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.subjects.*;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class ObservableMergeWithCompletableTest extends RxJavaTest {

    @Test
    public void normal() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.range(1, 5).mergeWith(
                Completable.fromAction(() -> to.onNext(100))
        )
        .subscribe(to);

        to.assertResult(1, 2, 3, 4, 5, 100);
    }

    @Test
    public void take() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.range(1, 5).mergeWith(
                Completable.complete()
        )
        .take(3)
        .subscribe(to);

        to.assertResult(1, 2, 3);
    }

    @Test
    public void cancel() {
        final PublishSubject<Integer> ps = PublishSubject.create();
        final CompletableSubject cs = CompletableSubject.create();

        TestObserver<Integer> to = ps.mergeWith(cs).test();

        assertTrue(ps.hasObservers());
        assertTrue(cs.hasObservers());

        to.dispose();

        assertFalse(ps.hasObservers());
        assertFalse(cs.hasObservers());
    }

    @Test
    public void mainError() {
        Observable.error(new TestException())
        .mergeWith(Completable.complete())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void otherError() {
        Observable.never()
        .mergeWith(Completable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void isDisposed() {
        new Observable<Integer>() /* NFI */ {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());

                assertFalse(((Disposable)observer).isDisposed());

                observer.onNext(1);

                assertTrue(((Disposable)observer).isDisposed());
            }
        }.mergeWith(Completable.complete())
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void cancelOtherOnMainError() {
        PublishSubject<Integer> ps = PublishSubject.create();
        CompletableSubject cs = CompletableSubject.create();

        TestObserver<Integer> to = ps.mergeWith(cs).test();

        assertTrue(ps.hasObservers());
        assertTrue(cs.hasObservers());

        ps.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse(ps.hasObservers(), "main has observers!");
        assertFalse(cs.hasObservers(), "other has observers");
    }

    @Test
    public void cancelMainOnOtherError() {
        PublishSubject<Integer> ps = PublishSubject.create();
        CompletableSubject cs = CompletableSubject.create();

        TestObserver<Integer> to = ps.mergeWith(cs).test();

        assertTrue(ps.hasObservers());
        assertTrue(cs.hasObservers());

        cs.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse(ps.hasObservers(), "main has observers!");
        assertFalse(cs.hasObservers(), "other has observers");
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel((ObservableConverter<Integer, Observable<Integer>>) upstream -> upstream.mergeWith(Completable.complete().hide()));
    }
}
