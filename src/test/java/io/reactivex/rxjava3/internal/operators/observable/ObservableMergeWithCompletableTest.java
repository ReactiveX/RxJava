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

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableMergeWithCompletableTest extends RxJavaTest {

    @Test
    public void normal() {
        final TestObserver<Integer> to = new TestObserver<>();

        Observable.range(1, 5).mergeWith(
                Completable.fromAction(new Action() {
                    @Override
                    public void run() throws Exception {
                        to.onNext(100);
                    }
                })
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
    public void completeRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();
            final CompletableSubject cs = CompletableSubject.create();

            TestObserver<Integer> to = ps.mergeWith(cs).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onNext(1);
                    ps.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    cs.onComplete();
                }
            };

            TestHelper.race(r1, r2);

            to.assertResult(1);
        }
    }

    @Test
    public void isDisposed() {
        new Observable<Integer>() {
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

        assertFalse("main has observers!", ps.hasObservers());
        assertFalse("other has observers", cs.hasObservers());
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

        assertFalse("main has observers!", ps.hasObservers());
        assertFalse("other has observers", cs.hasObservers());
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new ObservableConverter<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Observable<Integer> upstream) {
                return upstream.mergeWith(Completable.complete().hide());
            }
        });
    }
}
