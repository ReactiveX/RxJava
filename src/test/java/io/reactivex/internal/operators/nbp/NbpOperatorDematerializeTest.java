/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorDematerializeTest {

    @Test
    public void testDematerialize1() {
        NbpObservable<Try<Optional<Integer>>> notifications = NbpObservable.just(1, 2).materialize();
        NbpObservable<Integer> dematerialize = notifications.dematerialize();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        dematerialize.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDematerialize2() {
        Throwable exception = new Throwable("test");
        NbpObservable<Integer> o = NbpObservable.error(exception);
        NbpObservable<Integer> dematerialize = o.materialize().dematerialize();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        dematerialize.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onError(exception);
        verify(NbpObserver, times(0)).onComplete();
        verify(NbpObserver, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testDematerialize3() {
        Exception exception = new Exception("test");
        NbpObservable<Integer> o = NbpObservable.error(exception);
        NbpObservable<Integer> dematerialize = o.materialize().dematerialize();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        dematerialize.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onError(exception);
        verify(NbpObserver, times(0)).onComplete();
        verify(NbpObserver, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testErrorPassThru() {
        Exception exception = new Exception("test");
        NbpObservable<Integer> o = NbpObservable.error(exception);
        NbpObservable<Integer> dematerialize = o.dematerialize();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();

        dematerialize.subscribe(NbpObserver);

        verify(NbpObserver, times(1)).onError(exception);
        verify(NbpObserver, times(0)).onComplete();
        verify(NbpObserver, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testCompletePassThru() {
        NbpObservable<Integer> o = NbpObservable.empty();
        NbpObservable<Integer> dematerialize = o.dematerialize();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>(NbpObserver);
        dematerialize.subscribe(ts);

        System.out.println(ts.errors());

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testHonorsContractWhenCompleted() {
        NbpObservable<Integer> source = NbpObservable.just(1);
        
        NbpObservable<Integer> result = source.materialize().dematerialize();
        
        NbpSubscriber<Integer> o = TestHelper.mockNbpSubscriber();
        
        result.unsafeSubscribe(o);
        
        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testHonorsContractWhenThrows() {
        NbpObservable<Integer> source = NbpObservable.error(new TestException());
        
        NbpObservable<Integer> result = source.materialize().dematerialize();
        
        NbpSubscriber<Integer> o = TestHelper.mockNbpSubscriber();
        
        result.unsafeSubscribe(o);
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
}