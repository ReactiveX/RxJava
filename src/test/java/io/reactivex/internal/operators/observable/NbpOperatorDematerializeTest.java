/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.observers.TestObserver;

public class NbpOperatorDematerializeTest {

    @Test
    public void testDematerialize1() {
        Observable<Try<Optional<Integer>>> notifications = Observable.just(1, 2).materialize();
        Observable<Integer> dematerialize = notifications.dematerialize();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        dematerialize.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDematerialize2() {
        Throwable exception = new Throwable("test");
        Observable<Integer> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.materialize().dematerialize();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        dematerialize.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onError(exception);
        verify(NbpObserver, times(0)).onComplete();
        verify(NbpObserver, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testDematerialize3() {
        Exception exception = new Exception("test");
        Observable<Integer> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.materialize().dematerialize();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        dematerialize.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onError(exception);
        verify(NbpObserver, times(0)).onComplete();
        verify(NbpObserver, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testErrorPassThru() {
        Exception exception = new Exception("test");
        Observable<Integer> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.dematerialize();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();

        dematerialize.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onError(exception);
        verify(NbpObserver, times(0)).onComplete();
        verify(NbpObserver, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testCompletePassThru() {
        Observable<Integer> o = Observable.empty();
        Observable<Integer> dematerialize = o.dematerialize();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        TestObserver<Integer> ts = new TestObserver<Integer>(NbpObserver);
        dematerialize.subscribe(ts);

        System.out.println(ts.errors());

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testHonorsContractWhenCompleted() {
        Observable<Integer> source = Observable.just(1);
        
        Observable<Integer> result = source.materialize().dematerialize();
        
        Observer<Integer> o = TestHelper.mockNbpSubscriber();
        
        result.unsafeSubscribe(o);
        
        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testHonorsContractWhenThrows() {
        Observable<Integer> source = Observable.error(new TestException());
        
        Observable<Integer> result = source.materialize().dematerialize();
        
        Observer<Integer> o = TestHelper.mockNbpSubscriber();
        
        result.unsafeSubscribe(o);
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
}