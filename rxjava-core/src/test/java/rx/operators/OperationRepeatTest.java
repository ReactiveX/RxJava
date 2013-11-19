/**
 * Copyright 2013 Netflix, Inc.
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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static rx.Observable.repeat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Observer;
import rx.concurrency.TestScheduler;

public class OperationRepeatTest {

    @Test
    public void testRepeat() {
        Observable<Integer> observable = Observable.from(1, 2).repeat(3);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(1);
        inOrder.verify(observer).onNext(2);
        inOrder.verify(observer).onNext(1);
        inOrder.verify(observer).onNext(2);
        inOrder.verify(observer).onNext(1);
        inOrder.verify(observer).onNext(2);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRepeatWithSingleValue() {
        Observable<Integer> observable = repeat(1, 3);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(3)).onNext(1);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRepeatWithScheduler() {
        TestScheduler scheduler = new TestScheduler();
        Observable<Integer> observable = Observable.from(1, 2).repeat(3,
                scheduler);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verifyNoMoreInteractions();

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        inOrder.verify(observer).onNext(1);
        inOrder.verify(observer).onNext(2);
        inOrder.verify(observer).onNext(1);
        inOrder.verify(observer).onNext(2);
        inOrder.verify(observer).onNext(1);
        inOrder.verify(observer).onNext(2);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRepeatWithInfiniteRepeatCount() {
        Observable<String> observable = repeat("foo");

        @SuppressWarnings("unchecked")
        Observer<String> observer = (Observer<String>) mock(Observer.class);

        doAnswer(new Answer<Void>() {
            private int count = 0;

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                count++;
                if (count == 100) {
                    // Only verify if repeating 100 times
                    // We can not really verify if repeating infinitely.
                    throw new RuntimeException("some error");
                }
                return null;
            }
        }).when(observer).onNext(anyString());

        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(100)).onNext("foo");
        inOrder.verify(observer).onError(isA(RuntimeException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRepeatWithZeroRepeatCount() {
        Observable<Integer> observable = repeat(1, 0);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRepeatWithNegativeRepeatCount() {
        repeat(1, -1);
    }
}
