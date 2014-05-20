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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.NoSuchElementException;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;

public class OperatorLastTest {

    @Test
    public void testLastWithElements() {
        Observable<Integer> last = Observable.from(1, 2, 3).last();
        assertEquals(3, last.toBlocking().single().intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastWithNoElements() {
        Observable<?> last = Observable.empty().last();
        last.toBlocking().single();
    }

    @Test
    public void testLastMultiSubscribe() {
        Observable<Integer> last = Observable.from(1, 2, 3).last();
        assertEquals(3, last.toBlocking().single().intValue());
        assertEquals(3, last.toBlocking().single().intValue());
    }

    @Test
    public void testLastViaObservable() {
        Observable.from(1, 2, 3).last();
    }

    @Test
    public void testLast() {
        Observable<Integer> observable = Observable.from(1, 2, 3).last();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithOneElement() {
        Observable<Integer> observable = Observable.from(1).last();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithEmpty() {
        Observable<Integer> observable = Observable.<Integer> empty().last();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicate() {
        Observable<Integer> observable = Observable.from(1, 2, 3, 4, 5, 6)
                .last(new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(6);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndOneElement() {
        Observable<Integer> observable = Observable.from(1, 2).last(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndEmpty() {
        Observable<Integer> observable = Observable.from(1).last(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefault() {
        Observable<Integer> observable = Observable.from(1, 2, 3)
                .lastOrDefault(4);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithOneElement() {
        Observable<Integer> observable = Observable.from(1).lastOrDefault(2);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithEmpty() {
        Observable<Integer> observable = Observable.<Integer> empty()
                .lastOrDefault(1);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicate() {
        Observable<Integer> observable = Observable.from(1, 2, 3, 4, 5, 6)
                .lastOrDefault(8, new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(6);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndOneElement() {
        Observable<Integer> observable = Observable.from(1, 2).lastOrDefault(4,
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndEmpty() {
        Observable<Integer> observable = Observable.from(1).lastOrDefault(2,
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
}
