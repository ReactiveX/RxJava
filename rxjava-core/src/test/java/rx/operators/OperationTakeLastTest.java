/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.*;
import static rx.operators.OperationTakeLast.*;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.operators.OperationSkipTest.CustomException;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperationTakeLastTest {

    @Test
    public void testTakeLastEmpty() {
        Observable<String> w = Observable.empty();
        Observable<String> take = Observable.create(takeLast(w, 2));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, never()).onNext(any(String.class));
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLast1() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> take = Observable.create(takeLast(w, 2));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        InOrder inOrder = inOrder(aObserver);
        take.subscribe(aObserver);
        inOrder.verify(aObserver, times(1)).onNext("two");
        inOrder.verify(aObserver, times(1)).onNext("three");
        verify(aObserver, never()).onNext("one");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLast2() {
        Observable<String> w = Observable.from("one");
        Observable<String> take = Observable.create(takeLast(w, 10));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLastWithZeroCount() {
        Observable<String> w = Observable.from("one");
        Observable<String> take = Observable.create(takeLast(w, 0));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, never()).onNext("one");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLastWithNull() {
        Observable<String> w = Observable.from("one", null, "three");
        Observable<String> take = Observable.create(takeLast(w, 2));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, never()).onNext("one");
        verify(aObserver, times(1)).onNext(null);
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testTakeLastWithNegativeCount() {
        Observable<String> w = Observable.from("one");
        Observable<String> take = Observable.create(takeLast(w, -1));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        take.subscribe(aObserver);
        verify(aObserver, never()).onNext("one");
        verify(aObserver, times(1)).onError(
                any(IndexOutOfBoundsException.class));
        verify(aObserver, never()).onCompleted();
    }
    
    @Test
    public void testTakeLastTimed() {
        TestScheduler scheduler = new TestScheduler();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<Integer> result = source.takeLast(1, TimeUnit.SECONDS, scheduler, scheduler);
        
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

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);
        
        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(4);
        inOrder.verify(o).onNext(5);
        inOrder.verify(o).onNext(6);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        verify(o, never()).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testTakeLastTimedErrorBeforeTime() {
        TestScheduler scheduler = new TestScheduler();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<Integer> result = source.takeLast(1, TimeUnit.SECONDS, scheduler);
        
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onError(new CustomException());
        
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        
        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onError(any(CustomException.class));
        inOrder.verifyNoMoreInteractions();
        
        verify(o, never()).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o, never()).onCompleted();
    }
    
    @Test
    public void testTakeLastTimedErrorAfterTime() {
        TestScheduler scheduler = new TestScheduler();
        
        PublishSubject<Integer> source = PublishSubject.create();
        
        Observable<Integer> result = source.takeLast(1, TimeUnit.SECONDS, scheduler);
        
        Observer<Object> o = mock(Observer.class);
        
        result.subscribe(o);
        
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onError(new CustomException());

        scheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS);
        
        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onError(any(CustomException.class));
        inOrder.verifyNoMoreInteractions();
        
        verify(o, never()).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o, never()).onCompleted();
    }
}
