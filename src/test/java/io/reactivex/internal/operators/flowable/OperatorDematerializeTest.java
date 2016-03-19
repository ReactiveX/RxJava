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

package io.reactivex.internal.operators.flowable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorDematerializeTest {

    @Test
    public void testDematerialize1() {
        Flowable<Try<Optional<Integer>>> notifications = Flowable.just(1, 2).materialize();
        Flowable<Integer> dematerialize = notifications.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDematerialize2() {
        Throwable exception = new Throwable("test");
        Flowable<Integer> observable = Flowable.error(exception);
        Flowable<Integer> dematerialize = observable.materialize().dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testDematerialize3() {
        Exception exception = new Exception("test");
        Flowable<Integer> observable = Flowable.error(exception);
        Flowable<Integer> dematerialize = observable.materialize().dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testErrorPassThru() {
        Exception exception = new Exception("test");
        Flowable<Integer> observable = Flowable.error(exception);
        Flowable<Integer> dematerialize = observable.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testCompletePassThru() {
        Flowable<Integer> observable = Flowable.empty();
        Flowable<Integer> dematerialize = observable.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(observer);
        dematerialize.subscribe(ts);

        System.out.println(ts.errors());

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testHonorsContractWhenCompleted() {
        Flowable<Integer> source = Flowable.just(1);
        
        Flowable<Integer> result = source.materialize().dematerialize();
        
        Subscriber<Integer> o = TestHelper.mockSubscriber();
        
        result.unsafeSubscribe(o);
        
        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testHonorsContractWhenThrows() {
        Flowable<Integer> source = Flowable.error(new TestException());
        
        Flowable<Integer> result = source.materialize().dematerialize();
        
        Subscriber<Integer> o = TestHelper.mockSubscriber();
        
        result.unsafeSubscribe(o);
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
}