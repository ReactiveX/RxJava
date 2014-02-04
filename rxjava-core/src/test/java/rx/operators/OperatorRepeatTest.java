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

import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import rx.Observer;
import rx.Observable;
import org.junit.Test;
import static org.mockito.Mockito.*;
import rx.Subscription;
import rx.operators.OperationReduceTest.CustomException;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class OperatorRepeatTest {
    @Test/* (timeout = 2000) */
    public void testRepeatAndTake() {
        @SuppressWarnings("unchecked")
                Observer<Object> o = mock(Observer.class);
        
        Observable.from(1).repeat().take(10).subscribe(o);
        
        verify(o, times(10)).onNext(1);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void testRepeatLimited() {
        @SuppressWarnings("unchecked")
                Observer<Object> o = mock(Observer.class);
        
        Observable.from(1).repeat(10).subscribe(o);
        
        verify(o, times(10)).onNext(1);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test(timeout = 2000)
    public void testRepeatError() {
        @SuppressWarnings("unchecked")
                Observer<Object> o = mock(Observer.class);
        
        Observable.error(new CustomException()).repeat(10).subscribe(o);
        
        verify(o).onError(any(CustomException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        
    }
    @Test(timeout = 2000)
    public void testRepeatZero() {
        @SuppressWarnings("unchecked")
                Observer<Object> o = mock(Observer.class);
        
        Observable.from(1).repeat(0).subscribe(o);
        
        verify(o).onCompleted();
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testRepetition() {
        int NUM = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Observable.create(new Observable.OnSubscribeFunc<Integer>() {
            
            @Override
            public Subscription onSubscribe(Observer<? super Integer> o) {
                o.onNext(count.incrementAndGet());
                o.onCompleted();
                return Subscriptions.empty();
            }
        }).repeat(Schedulers.computation()).take(NUM).toBlockingObservable().last();
        
        assertEquals(NUM, value);
    }
    
    @Test
    public void testRepeatTake() {
        Observable<Integer> xs = Observable.from(1, 2);
        Object[] ys = xs.repeat(Schedulers.newThread()).take(4).toList().toBlockingObservable().last().toArray();
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }
    
    @Test
    public void testNoStackOverFlow() {
        assertEquals((Integer)1,
                Observable.from(1).repeat(Schedulers.newThread()).take(100000).toBlockingObservable().last());
    }}
