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
import static rx.operators.OperationSkipLast.*;

import org.junit.Test;
import org.mockito.InOrder;

import rx.IObservable;
import rx.Observable;
import rx.Observer;
import rx.concurrency.TestScheduler;
import rx.operators.OperationSkipTest.CustomException;
import rx.subjects.PublishSubject;

public class OperationSkipLastTest {

    @Test
    public void testSkipLastEmpty() {
        Observable<String> w = Observable.empty();
        IObservable<String> observable = skipLast(w, 2);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(any(String.class));
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSkipLast1() {
        Observable<String> w = Observable.from("one", "two", "three");
        IObservable<String> observable = skipLast(w, 2);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        InOrder inOrder = inOrder(aObserver);
        observable.subscribe(aObserver);
        inOrder.verify(aObserver, never()).onNext("two");
        inOrder.verify(aObserver, never()).onNext("three");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSkipLast2() {
        Observable<String> w = Observable.from("one", "two");
        IObservable<String> observable = skipLast(w, 2);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(any(String.class));
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSkipLastWithZeroCount() {
        Observable<String> w = Observable.from("one", "two");
        IObservable<String> observable = skipLast(w, 0);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSkipLastWithNull() {
        Observable<String> w = Observable.from("one", null, "two");
        IObservable<String> observable = skipLast(w, 1);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext(null);
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSkipLastWithNegativeCount() {
        Observable<String> w = Observable.from("one");
        IObservable<String> observable = skipLast(w, -1);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(any(String.class));
        verify(aObserver, times(1)).onError(
                any(IndexOutOfBoundsException.class));
        verify(aObserver, never()).onCompleted();
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
