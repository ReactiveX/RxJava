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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.subjects.UnicastSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableFilterTest extends RxJavaTest {

    @Test
    public void filter() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> observable = w.filter(new Predicate<String>() {

            @Override
            public boolean test(String t1) {
                return t1.equals("two");
            }
        });

        Observer<String> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, Mockito.never()).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 5).filter(Functions.alwaysTrue()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.filter(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void fusedSync() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        Observable.range(1, 5)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(to);

        to.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 4);
    }

    @Test
    public void fusedAsync() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        UnicastSubject<Integer> us = UnicastSubject.create();

        us
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(to);

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        to.assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 4);
    }

    @Test
    public void fusedReject() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY | QueueFuseable.BOUNDARY);

        Observable.range(1, 5)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(to);

        to.assertFusionMode(QueueFuseable.NONE)
        .assertResult(2, 4);
    }

    @Test
    public void filterThrows() {
        Observable.range(1, 5)
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
}
