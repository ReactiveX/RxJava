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

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Action;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.*;

public class ObservableMergeWithCompletableTest {

    @Test
    public void normal() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

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
        final TestObserver<Integer> to = new TestObserver<Integer>();

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

        to.cancel();

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
                observer.onSubscribe(Disposables.empty());

                assertFalse(((Disposable)observer).isDisposed());

                observer.onNext(1);

                assertTrue(((Disposable)observer).isDisposed());
            }
        }.mergeWith(Completable.complete())
        .take(1)
        .test()
        .assertResult(1);
    }
}
