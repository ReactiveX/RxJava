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

import org.junit.Test;
import org.mockito.InOrder;
import rx.Observable;
import rx.Observer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class OperatorTakeLastTest {

    @Test
    public void testTakeLastEmpty() {
        Observable<String> w = Observable.empty();
        Observable<String> take = w.takeLast(2);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        take.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testTakeLast1() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> take = w.takeLast(2);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        InOrder inOrder = inOrder(observer);
        take.subscribe(observer);
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testTakeLast2() {
        Observable<String> w = Observable.from("one");
        Observable<String> take = w.takeLast(10);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testTakeLastWithZeroCount() {
        Observable<String> w = Observable.from("one");
        Observable<String> take = w.takeLast(0);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        take.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testTakeLastWithNull() {
        Observable<String> w = Observable.from("one", null, "three");
        Observable<String> take = w.takeLast(2);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        take.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onNext(null);
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTakeLastWithNegativeCount() {
        Observable.from("one").takeLast(-1);
    }

}
