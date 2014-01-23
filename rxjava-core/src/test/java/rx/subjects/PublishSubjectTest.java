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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class PublishSubjectTest {

    @Test
    public void testCompleted() {
        PublishSubject<String> subject = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.toObservable().subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        @SuppressWarnings("unchecked")
        Observer<String> anotherObserver = mock(Observer.class);
        subject.toObservable().subscribe(anotherObserver);

        subject.onNext("four");
        subject.onCompleted();
        subject.onError(new Throwable());

        assertCompletedObserver(aObserver);
        // todo bug?            assertNeverObserver(anotherObserver);
    }

    @Test
    public void testCompletedStopsEmittingData() {
        PublishSubject<Object> channel = PublishSubject.create();
        @SuppressWarnings("unchecked")
        Observer<Object> observerA = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerB = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observerC = mock(Observer.class);

        Subscription a = channel.toObservable().subscribe(observerA);
        Subscription b = channel.toObservable().subscribe(observerB);

        InOrder inOrderA = inOrder(observerA);
        InOrder inOrderB = inOrder(observerB);
        InOrder inOrderC = inOrder(observerC);

        channel.onNext(42);

        inOrderA.verify(observerA).onNext(42);
        inOrderB.verify(observerB).onNext(42);

        a.unsubscribe();
        inOrderA.verifyNoMoreInteractions();

        channel.onNext(4711);

        inOrderB.verify(observerB).onNext(4711);

        channel.onCompleted();

        inOrderB.verify(observerB).onCompleted();

        Subscription c = channel.toObservable().subscribe(observerC);

        inOrderC.verify(observerC).onCompleted();

        channel.onNext(13);

        inOrderB.verifyNoMoreInteractions();
        inOrderC.verifyNoMoreInteractions();
    }

    private void assertCompletedObserver(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testError() {
        PublishSubject<String> subject = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.toObservable().subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);

        @SuppressWarnings("unchecked")
        Observer<String> anotherObserver = mock(Observer.class);
        subject.toObservable().subscribe(anotherObserver);

        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onCompleted();

        assertErrorObserver(aObserver);
        // todo bug?            assertNeverObserver(anotherObserver);
    }

    private void assertErrorObserver(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, times(1)).onError(testException);
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testSubscribeMidSequence() {
        PublishSubject<String> subject = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        subject.toObservable().subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");

        assertObservedUntilTwo(aObserver);

        @SuppressWarnings("unchecked")
        Observer<String> anotherObserver = mock(Observer.class);
        subject.toObservable().subscribe(anotherObserver);

        subject.onNext("three");
        subject.onCompleted();

        assertCompletedObserver(aObserver);
        assertCompletedStartingWithThreeObserver(anotherObserver);
    }

    private void assertCompletedStartingWithThreeObserver(Observer<String> aObserver) {
        verify(aObserver, Mockito.never()).onNext("one");
        verify(aObserver, Mockito.never()).onNext("two");
        verify(aObserver, times(1)).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testUnsubscribeFirstObserver() {
        PublishSubject<String> subject = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        Subscription subscription = subject.toObservable().subscribe(aObserver);

        subject.onNext("one");
        subject.onNext("two");

        subscription.unsubscribe();
        assertObservedUntilTwo(aObserver);

        @SuppressWarnings("unchecked")
        Observer<String> anotherObserver = mock(Observer.class);
        subject.toObservable().subscribe(anotherObserver);

        subject.onNext("three");
        subject.onCompleted();

        assertObservedUntilTwo(aObserver);
        assertCompletedStartingWithThreeObserver(anotherObserver);
    }

    private void assertObservedUntilTwo(Observer<String> aObserver) {
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, Mockito.never()).onCompleted();
    }

    @Test
    public void testNestedSubscribe() {
        final PublishSubject<Integer> s = PublishSubject.create();

        final AtomicInteger countParent = new AtomicInteger();
        final AtomicInteger countChildren = new AtomicInteger();
        final AtomicInteger countTotal = new AtomicInteger();

        final ArrayList<String> list = new ArrayList<String>();

        s.toObservable().flatMap(new Func1<Integer, Observable<String>>() {

            @Override
            public Observable<String> call(final Integer v) {
                countParent.incrementAndGet();

                // then subscribe to subject again (it will not receive the previous value)
                return s.toObservable().map(new Func1<Integer, String>() {

                    @Override
                    public String call(Integer v2) {
                        countChildren.incrementAndGet();
                        return "Parent: " + v + " Child: " + v2;
                    }

                });
            }

        }).subscribe(new Action1<String>() {

            @Override
            public void call(String v) {
                countTotal.incrementAndGet();
                list.add(v);
            }

        });

        for (int i = 0; i < 10; i++) {
            s.onNext(i);
        }
        s.onCompleted();

        //            System.out.println("countParent: " + countParent.get());
        //            System.out.println("countChildren: " + countChildren.get());
        //            System.out.println("countTotal: " + countTotal.get());

        // 9+8+7+6+5+4+3+2+1+0 == 45
        assertEquals(45, list.size());
    }

    /**
     * Should be able to unsubscribe all Observers, have it stop emitting, then subscribe new ones and it start emitting again.
     */
    @Test
    public void testReSubscribe() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        Observer<Integer> o1 = mock(Observer.class);
        Subscription s1 = ps.toObservable().subscribe(o1);

        // emit
        ps.onNext(1);

        // validate we got it
        InOrder inOrder1 = inOrder(o1);
        inOrder1.verify(o1, times(1)).onNext(1);
        inOrder1.verifyNoMoreInteractions();

        // unsubscribe
        s1.unsubscribe();

        // emit again but nothing will be there to receive it
        ps.onNext(2);

        Observer<Integer> o2 = mock(Observer.class);
        Subscription s2 = ps.toObservable().subscribe(o2);

        // emit
        ps.onNext(3);

        // validate we got it
        InOrder inOrder2 = inOrder(o2);
        inOrder2.verify(o2, times(1)).onNext(3);
        inOrder2.verifyNoMoreInteractions();

        s2.unsubscribe();
    }

    private final Throwable testException = new Throwable();

}
