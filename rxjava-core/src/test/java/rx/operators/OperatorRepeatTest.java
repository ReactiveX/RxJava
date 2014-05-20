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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class OperatorRepeatTest {

    @Test(timeout = 2000)
    public void testRepetition() {
        int NUM = 10;
        final AtomicInteger count = new AtomicInteger();
        int value = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> o) {
                o.onNext(count.incrementAndGet());
                o.onCompleted();
            }
        }).repeat(Schedulers.computation()).take(NUM).toBlocking().last();

        assertEquals(NUM, value);
    }

    @Test(timeout = 2000)
    public void testRepeatTake() {
        Observable<Integer> xs = Observable.from(1, 2);
        Object[] ys = xs.repeat(Schedulers.newThread()).take(4).toList().toBlocking().last().toArray();
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

    @Test(timeout = 20000)
    public void testNoStackOverFlow() {
        Observable.from(1).repeat(Schedulers.newThread()).take(100000).toBlocking().last();
    }

    @Test
    public void testRepeatTakeWithSubscribeOn() throws InterruptedException {

        final AtomicInteger counter = new AtomicInteger();
        Observable<Integer> oi = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> sub) {
                counter.incrementAndGet();
                sub.onNext(1);
                sub.onNext(2);
                sub.onCompleted();
            }
        }).subscribeOn(Schedulers.newThread());

        Object[] ys = oi.repeat(Schedulers.newThread()).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return t1;
            }

        }).take(4).toList().toBlocking().last().toArray();

        assertEquals(2, counter.get());
        assertArrayEquals(new Object[] { 1, 2, 1, 2 }, ys);
    }

    @Test(timeout = 2000)
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
        
        Observable.error(new TestException()).repeat(10).subscribe(o);
        
        verify(o).onError(any(TestException.class));
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
}
