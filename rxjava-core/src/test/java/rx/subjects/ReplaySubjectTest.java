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
package rx.subjects;

import static org.junit.Assert.*;
import org.junit.*;
import org.mockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import rx.*;
import rx.schedulers.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ReplaySubjectTest {

    private final Throwable testException = new Throwable();

    @SuppressWarnings("unchecked")
    @Test
    public void testCompleted() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> o1 = mock(Observer.class);
        subject.subscribe(o1);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        subject.onNext("four");
        subject.onCompleted();
        subject.onError(new Throwable());

        assertCompletedObserver(o1);

        // assert that subscribing a 2nd time gets the same data
        Observer<String> o2 = mock(Observer.class);
        subject.subscribe(o2);
        assertCompletedObserver(o2);
    }

    @Test
    public void testCompletedStopsEmittingData() {
        ReplaySubject<Integer> channel = ReplaySubject.create();
        @SuppressWarnings("unchecked")
        Observer<Object> observerA = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerB = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerC = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerD = mock(Observer.class);

        Subscription a = channel.subscribe(observerA);
        Subscription b = channel.subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);
        InOrder inOrderD = inOrder(observerD);

        channel.onNext(42);

        // both A and B should have received 42 from before subscription
        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        a.unsubscribe();

        // a should receive no more
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        // only be should receive 4711 at this point
        inOrderB.verify(observerB).onNext(4711);

        channel.onCompleted();

        // B is subscribed so should receive onCompleted
        inOrderB.verify(observerB).onCompleted();

        Subscription c = channel.subscribe(observerC);

        // when C subscribes it should receive 42, 4711, onCompleted
        inOrderC.verify(observerC).onNext(42);
        inOrderC.verify(observerC).onNext(4711);
        inOrderC.verify(observerC).onCompleted();

        // if further events are propagated they should be ignored
        channel.onNext(13);
        channel.onNext(14);
        channel.onNext(15);
        channel.onError(new RuntimeException());

        // a new subscription should only receive what was emitted prior to terminal state onCompleted
        Subscription d = channel.subscribe(observerD);

        inOrderD.verify(observerD).onNext(42);
        inOrderD.verify(observerD).onNext(4711);
        inOrderD.verify(observerD).onCompleted();

        Mockito.verifyNoMoreInteractions(observerA);
        Mockito.verifyNoMoreInteractions(observerB);
        Mockito.verifyNoMoreInteractions(observerC);
        Mockito.verifyNoMoreInteractions(observerD);

    }

    @Test
    public void testCompletedAfterError() {
        ReplaySubject<String> subject = ReplaySubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);

        subject.onNext("one");
        subject.onError(testException);
        subject.onNext("two");
        subject.onCompleted();
        subject.onError(new RuntimeException());

        subject.subscribe(aObserver);
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onError(testException);
        verifyNoMoreInteractions(aObserver);
    }

    @Test
    public void testCapacity() {
        ReplaySubject<String> subject = ReplaySubject.create(1);
        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        assertCompletedObserver(aObserver);

        Observer<String> anotherObserver = mock(Observer.class);
        subject.subscribe(anotherObserver);

        verify(anotherObserver, times(0)).onNext("one");
        verify(anotherObserver, times(0)).onNext("two");
        verify(anotherObserver, times(1)).onNext("three");
        verify(anotherObserver, times(1)).onCompleted();
        verifyNoMoreInteractions(anotherObserver);
    }

    private void assertCompletedObserver(Observer<String> aObserver) {
        InOrder inOrder = inOrder(aObserver);

        inOrder.verify(aObserver, times(1)).onNext("one");
        inOrder.verify(aObserver, times(1)).onNext("two");
        inOrder.verify(aObserver, times(1)).onNext("three");
        inOrder.verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        inOrder.verify(aObserver, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testError() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);

        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onCompleted();

        assertErrorObserver(aObserver);

        aObserver = mock(Observer.class);
        subject.subscribe(aObserver);
        assertErrorObserver(aObserver);
    }

    private void assertErrorObserver(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, times(1)).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSubscribeMidSequence() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> aObserver = mock(Observer.class);
        subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");

        assertObservedUntilTwo(aObserver);

        Observer<String> anotherObserver = mock(Observer.class);
        subject.subscribe(anotherObserver);
        assertObservedUntilTwo(anotherObserver);

        subject.onNext("three");
        subject.onCompleted();

        assertCompletedObserver(aObserver);
        assertCompletedObserver(anotherObserver);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnsubscribeFirstObserver() {
        ReplaySubject<String> subject = ReplaySubject.create();

        Observer<String> aObserver = mock(Observer.class);
        Subscription subscription = subject.subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");

        subscription.unsubscribe();
        assertObservedUntilTwo(aObserver);

        Observer<String> anotherObserver = mock(Observer.class);
        subject.subscribe(anotherObserver);
        assertObservedUntilTwo(anotherObserver);

        subject.onNext("three");
        subject.onCompleted();

        assertObservedUntilTwo(aObserver);
        assertCompletedObserver(anotherObserver);
    }

    private void assertObservedUntilTwo(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test(timeout = 2000)
    public void testNewSubscriberDoesntBlockExisting() throws InterruptedException {

        final AtomicReference<String> lastValueForObserver1 = new AtomicReference<String>();
        Observer<String> observer1 = new Observer<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String v) {
                System.out.println("observer1: " + v);
                lastValueForObserver1.set(v);
            }

        };

        final AtomicReference<String> lastValueForObserver2 = new AtomicReference<String>();
        final CountDownLatch oneReceived = new CountDownLatch(1);
        final CountDownLatch makeSlow = new CountDownLatch(1);
        final CountDownLatch completed = new CountDownLatch(1);
        Observer<String> observer2 = new Observer<String>() {

            @Override
            public void onCompleted() {
                completed.countDown();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String v) {
                System.out.println("observer2: " + v);
                if (v.equals("one")) {
                    oneReceived.countDown();
                } else {
                    try {
                        makeSlow.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    lastValueForObserver2.set(v);
                }
            }

        };

        ReplaySubject<String> subject = ReplaySubject.create();
        Subscription s1 = subject.subscribe(observer1);
        subject.onNext("one");
        assertEquals("one", lastValueForObserver1.get());
        subject.onNext("two");
        assertEquals("two", lastValueForObserver1.get());

        Subscription s2 = subject.observeOn(Schedulers.newThread()).subscribe(observer2);

        System.out.println("before waiting for one");

        // wait until observer2 starts having replay occur
        oneReceived.await();

        System.out.println("after waiting for one");

        subject.onNext("three");
        // if subscription blocked existing subscribers then 'makeSlow' would cause this to not be there yet 
        assertEquals("three", lastValueForObserver1.get());
        subject.onCompleted();

        // release 
        makeSlow.countDown();
        completed.await();
        // all of them should be emitted with the last being "three"
        assertEquals("three", lastValueForObserver2.get());

    }

}
