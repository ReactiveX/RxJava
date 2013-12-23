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
package rx.subjects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observer;
import rx.Subscription;

public class BehaviorSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testThatObserverReceivesDefaultValueAndSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testThatObserverReceivesLatestAndThenSubsequentEvents() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        subject.onNext("one");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("two");
        subject.onNext("three");

        verify(aObserver, Mockito.never()).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testSubscribeThenOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onCompleted();

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeToCompletedOnlyEmitsOnComplete() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");
        subject.onNext("one");
        subject.onCompleted();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        verify(aObserver, never()).onNext("default");
        verify(aObserver, never()).onNext("one");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeToErrorOnlyEmitsOnError() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");
        subject.onNext("one");
        RuntimeException re = new RuntimeException("test error");
        subject.onError(re);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        verify(aObserver, never()).onNext("default");
        verify(aObserver, never()).onNext("one");
        verify(aObserver, times(1)).onError(re);
        verify(aObserver, never()).onCompleted();
    }

    @Test
    public void testCompletedStopsEmittingData() {
        BehaviorSubject<Integer> channel = BehaviorSubject.create(2013);
        @SuppressWarnings("unchecked")
        Observer<Object> observerA = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerB = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerC = mock(Observer.class);

        Subscription a = channel.subscribe(observerA);
        Subscription b = channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);

        inOrderA.verify(observerA).onNext(2013);
        inOrderB.verify(observerB).onNext(2013);

        channel.onNext(42);

        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        a.unsubscribe();
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        inOrderB.verify(observerB).onNext(4711);

        channel.onCompleted();

        inOrderB.verify(observerB).onCompleted();

        Subscription c = channel.subscribe(observerC);

        inOrderC.verify(observerC).onCompleted();

        channel.onNext(13);

        inOrderB.verifyNoMoreInteractions();
        inOrderC.verifyNoMoreInteractions();
    }

    @Test
    public void testValueSkipWhileSubscribing() throws InterruptedException {
        final BehaviorSubject<Integer> bs = BehaviorSubject.create(1);

        final CountDownLatch firstValuePause = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);

        final List<Integer> values = new ArrayList<Integer>();
        
        final Observer<Integer> onNext = new Observer<Integer>() {

            @Override
            public void onNext(Integer t1) {
                values.add(t1);
                try {
                    firstValuePause.await();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onCompleted() {
                done.countDown();
            }
            
        };
        
        bs.onNext(2);
        bs.onNext(3);
        
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                bs.subscribe(onNext);
            }
        });
        t1.start();
        
        final CountDownLatch t2ready = new CountDownLatch(1);
        
        Thread t2 = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    t2ready.countDown();
                    Thread.sleep(1000);
                    bs.onNext(4);
                    Thread.sleep(1000);
                    bs.onNext(5);
                    Thread.sleep(1000);
                    bs.onNext(6);
                    Thread.sleep(1000);
                    bs.onNext(7);
                    Thread.sleep(1000);
                    bs.onNext(8);
                    Thread.sleep(1000);
                    bs.onNext(9);
                    Thread.sleep(1000);
                    bs.onCompleted();
                } catch (InterruptedException ex) {
                    
                }
            }
        });
        
        t2.start();
        
        if (!t2ready.await(10000, TimeUnit.MILLISECONDS)) {
            fail("Couldn't get t2 ready in time.");
        }
        
        Thread.sleep(3000);
        
        firstValuePause.countDown();
        
        t1.join();
        t2.join();
        
        if (!done.await(10000, TimeUnit.MILLISECONDS)) {
            fail("Not done in time");
        }
        
        assertEquals(Arrays.asList(3, 4, 5, 6, 7, 8, 9), values);
    }
    
    @Test
    public void testCompletedAfterErrorIsNotSent() {
        BehaviorSubject<String> subject = BehaviorSubject.create("default");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();

        verify(aObserver, times(1)).onNext("default");
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onError(testException);
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onCompleted();
    }

}
