/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.functions.Functions;

public class OperatorAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Boolean> observable = w.exists(Functions.alwaysTrue());

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Boolean> observable = w.isEmpty();

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithOneItem() {
        Observable<Integer> w = Observable.from(1);
        Observable<Boolean> observable = w.exists(Functions.alwaysTrue());

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Observable<Integer> w = Observable.from(1);
        Observable<Boolean> observable = w.isEmpty();

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.exists(Functions.alwaysTrue());

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.isEmpty();

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithPredicate1() {
        Observable<Integer> w = Observable.from(1, 2, 3);
        Observable<Boolean> observable = w.exists(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 2;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testExists1() {
        Observable<Integer> w = Observable.from(1, 2, 3);
        Observable<Boolean> observable = w.exists(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 2;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithPredicate2() {
        Observable<Integer> w = Observable.from(1, 2, 3);
        Observable<Boolean> observable = w.exists(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 1;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.exists(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return true;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }
}
