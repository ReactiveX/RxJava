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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.operators.OperationSkipTest.CustomException;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperationSkipLastTest {

    @Test
    public void testSkipLastEmpty() {
        Observable<String> observable = Observable.<String>empty().skipLast(2);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipLast1() {
        Observable<String> observable = Observable.from(Arrays.asList("one", "two", "three")).skipLast(2);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        InOrder inOrder = inOrder(observer);
        observable.subscribe(observer);
        inOrder.verify(observer, never()).onNext("two");
        inOrder.verify(observer, never()).onNext("three");
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipLast2() {
        Observable<String> observable = Observable.from(Arrays.asList("one", "two")).skipLast(2);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipLastWithZeroCount() {
        Observable<String> w = Observable.from("one", "two");
        Observable<String> observable = w.skipLast(0);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipLastWithNull() {
        Observable<String> observable = Observable.from(Arrays.asList("one", null, "two")).skipLast(1);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext(null);
        verify(observer, never()).onNext("two");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSkipLastWithNegativeCount() {
        Observable.from("one").skipLast(-1);
    }

    @Test
    public void testSkipLastTimed() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skipLast(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        scheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS);
        source.onCompleted();

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o, never()).onNext(4);
        inOrder.verify(o, never()).onNext(5);
        inOrder.verify(o, never()).onNext(6);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSkipLastTimedErrorBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skipLast(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onError(new OperationSkipTest.CustomException());

        scheduler.advanceTimeBy(1050, TimeUnit.MILLISECONDS);

        verify(o).onError(any(CustomException.class));

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
    }

    @Test
    public void testSkipLastTimedCompleteBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skipLast(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = mock(Observer.class);

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onCompleted();

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }
}
