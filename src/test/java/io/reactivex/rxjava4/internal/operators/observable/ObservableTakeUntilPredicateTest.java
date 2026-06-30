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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.Predicate;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.*;

public class ObservableTakeUntilPredicateTest extends RxJavaTest {
    @Test
    public void takeEmpty() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.empty().takeUntil(_ -> true).subscribe(o);

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }

    @Test
    public void takeAll() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1, 2).takeUntil(_ -> false).subscribe(o);

        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }

    @Test
    public void takeFirst() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1, 2).takeUntil(_ -> true).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }

    @Test
    public void takeSome() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1, 2, 3).takeUntil(t1 -> t1 == 2)
        .subscribe(o);

        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onNext(3);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }

    @Test
    public void functionThrows() {
        Observer<Object> o = TestHelper.mockObserver();

        Predicate<Integer> predicate = (_ -> {
                throw new TestException("Forced failure");
        });
        Observable.just(1, 2, 3).takeUntil(predicate).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void sourceThrows() {
        Observer<Object> o = TestHelper.mockObserver();

        Observable.just(1)
        .concatWith(Observable.<Integer>error(new TestException()))
        .concatWith(Observable.just(2))
        .takeUntil(_ -> false).subscribe(o);

        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void errorIncludesLastValueAsCause() {
        TestObserverEx<String> to = new TestObserverEx<>();
        final TestException e = new TestException("Forced failure");
        Predicate<String> predicate = (_ -> {
                throw e;
        });
        Observable.just("abc").takeUntil(predicate).subscribe(to);

        to.assertTerminated();
        to.assertNotComplete();
        to.assertError(TestException.class);
        // FIXME last cause value is not saved
//        assertTrue(ts.errors().get(0).getCause().getMessage().contains("abc"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().takeUntil(Functions.alwaysFalse()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(o -> o.takeUntil(Functions.alwaysFalse()));
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() /* NFI */ {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onComplete();
                    observer.onNext(1);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .takeUntil(Functions.alwaysFalse())
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
