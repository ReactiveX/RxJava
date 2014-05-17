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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.observers.Subscribers;
import rx.observers.TestSubscriber;

public class OperatorDematerializeTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testDematerialize1() {
        Observable<Notification<Integer>> notifications = Observable.from(1, 2).materialize();
        Observable<Integer> dematerialize = notifications.dematerialize();

        Observer<Integer> observer = mock(Observer.class);
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDematerialize2() {
        Throwable exception = new Throwable("test");
        Observable<Integer> observable = Observable.error(exception);
        Observable<Integer> dematerialize = observable.materialize().dematerialize();

        Observer<Integer> observer = mock(Observer.class);
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onCompleted();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDematerialize3() {
        Exception exception = new Exception("test");
        Observable<Integer> observable = Observable.error(exception);
        Observable<Integer> dematerialize = observable.materialize().dematerialize();

        Observer<Integer> observer = mock(Observer.class);
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onCompleted();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testErrorPassThru() {
        Exception exception = new Exception("test");
        Observable<Integer> observable = Observable.error(exception);
        Observable<Integer> dematerialize = observable.dematerialize();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onCompleted();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testCompletePassThru() {
        Observable<Integer> observable = Observable.empty();
        Observable<Integer> dematerialize = observable.dematerialize();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(observer);
        dematerialize.subscribe(ts);

        System.out.println(ts.getOnErrorEvents());

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testHonorsContractWhenCompleted() {
        Observable<Integer> source = Observable.just(1);
        
        Observable<Integer> result = source.materialize().dematerialize();
        
        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
        
        result.unsafeSubscribe(Subscribers.from(o));
        
        verify(o).onNext(1);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testHonorsContractWhenThrows() {
        Observable<Integer> source = Observable.error(new TestException());
        
        Observable<Integer> result = source.materialize().dematerialize();
        
        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
        
        result.unsafeSubscribe(Subscribers.from(o));
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }
}
