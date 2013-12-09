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
package rx.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationObserveOn.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Observer;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.util.functions.Action1;

public class OperationObserveOnTest {

    /**
     * This is testing a no-op path since it uses Schedulers.immediate() which will not do scheduling.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testObserveOn() {
        Observer<Integer> observer = mock(Observer.class);
        Observable.create(observeOn(Observable.from(1, 2, 3), Schedulers.immediate())).subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOrdering() throws InterruptedException {
        Observable<String> obs = Observable.from("one", null, "two", "three", "four");

        Observer<String> observer = mock(Observer.class);

        InOrder inOrder = inOrder(observer);

        final CountDownLatch completedLatch = new CountDownLatch(1);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                completedLatch.countDown();
                return null;
            }
        }).when(observer).onCompleted();

        obs.observeOn(Schedulers.threadPoolForComputation()).subscribe(observer);

        if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting");
        }

        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext(null);
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThreadName() throws InterruptedException {
        Observable<String> obs = Observable.from("one", null, "two", "three", "four");

        Observer<String> observer = mock(Observer.class);

        InOrder inOrder = inOrder(observer);

        final CountDownLatch completedLatch = new CountDownLatch(1);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                completedLatch.countDown();

                return null;
            }
        }).when(observer).onCompleted();

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                completedLatch.countDown();

                return null;
            }
        }).when(observer).onError(any(Exception.class));

        obs.observeOn(Schedulers.newThread()).doOnEach(new Action1<String>() {

            @Override
            public void call(String t1) {
                String threadName = Thread.currentThread().getName();
                boolean correctThreadName = threadName.startsWith("RxNewThreadScheduler");
                System.out.println("ThreadName: " + threadName + "  Correct => " + correctThreadName);
                assertTrue(correctThreadName);
            }

        }).subscribe(observer);

        if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting");
        }

        inOrder.verify(observer, times(1)).onCompleted();
    }
    @Test
    public void observeOnTheSameSchedulerTwice() {
        TestScheduler scheduler = new TestScheduler();
        
        Observable<Integer> o = Observable.from(1, 2, 3);
        Observable<Integer> o2 = o.observeOn(scheduler);

        @SuppressWarnings("unchecked")
        Observer<Object> observer1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observer2 = mock(Observer.class);
        
        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);
        
        o2.subscribe(observer1);
        o2.subscribe(observer2);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        inOrder1.verify(observer1, times(1)).onNext(1);
        inOrder1.verify(observer1, times(1)).onNext(2);
        inOrder1.verify(observer1, times(1)).onNext(3);
        inOrder1.verify(observer1, times(1)).onCompleted();
        verify(observer1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(observer2, times(1)).onNext(1);
        inOrder2.verify(observer2, times(1)).onNext(2);
        inOrder2.verify(observer2, times(1)).onNext(3);
        inOrder2.verify(observer2, times(1)).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();

    }
    @Test
    public void observeSameOnMultipleSchedulers() {
        TestScheduler scheduler1 = new TestScheduler();
        TestScheduler scheduler2 = new TestScheduler();
        
        Observable<Integer> o = Observable.from(1, 2, 3);
        Observable<Integer> o1 = o.observeOn(scheduler1);
        Observable<Integer> o2 = o.observeOn(scheduler2);

        @SuppressWarnings("unchecked")
        Observer<Object> observer1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observer2 = mock(Observer.class);
        
        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);
        
        o1.subscribe(observer1);
        o2.subscribe(observer2);
        
        scheduler1.advanceTimeBy(1, TimeUnit.SECONDS);
        scheduler2.advanceTimeBy(1, TimeUnit.SECONDS);
        
        inOrder1.verify(observer1, times(1)).onNext(1);
        inOrder1.verify(observer1, times(1)).onNext(2);
        inOrder1.verify(observer1, times(1)).onNext(3);
        inOrder1.verify(observer1, times(1)).onCompleted();
        verify(observer1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(observer2, times(1)).onNext(1);
        inOrder2.verify(observer2, times(1)).onNext(2);
        inOrder2.verify(observer2, times(1)).onNext(3);
        inOrder2.verify(observer2, times(1)).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();

    }
}
