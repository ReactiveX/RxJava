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
import static rx.operators.OperationElementAt.*;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import rx.Observable;
import rx.Observer;

public class OperationElementAtTest {

    @Test
    public void testElementAt() {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Integer> observable = Observable.create(elementAt(w, 1));

        @SuppressWarnings("unchecked")
        Observer<Integer> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(1);
        verify(aObserver, times(1)).onNext(2);
        verify(aObserver, never()).onError(
                any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testElementAtWithMinusIndex() {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Integer> observable = Observable
                .create(elementAt(w, -1));

        try {
            Iterator<Integer> iter = OperationToIterator
                    .toIterator(observable);
            assertTrue(iter.hasNext());
            iter.next();
            fail("expect an IndexOutOfBoundsException when index is out of bounds");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testElementAtWithIndexOutOfBounds()
            throws InterruptedException, ExecutionException {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Integer> observable = Observable.create(elementAt(w, 2));
        try {
            Iterator<Integer> iter = OperationToIterator
                    .toIterator(observable);
            assertTrue(iter.hasNext());
            iter.next();
            fail("expect an IndexOutOfBoundsException when index is out of bounds");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testElementAtOrDefault() throws InterruptedException,
            ExecutionException {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Integer> observable = Observable
                .create(elementAtOrDefault(w, 1, 0));

        @SuppressWarnings("unchecked")
        Observer<Integer> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(1);
        verify(aObserver, times(1)).onNext(2);
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testElementAtOrDefaultWithIndexOutOfBounds()
            throws InterruptedException, ExecutionException {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Integer> observable = Observable
                .create(elementAtOrDefault(w, 2, 0));

        @SuppressWarnings("unchecked")
        Observer<Integer> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, never()).onNext(1);
        verify(aObserver, never()).onNext(2);
        verify(aObserver, times(1)).onNext(0);
        verify(aObserver, never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    @Test
    public void testElementAtOrDefaultWithMinusIndex() {
        Observable<Integer> w = Observable.from(1, 2);
        Observable<Integer> observable = Observable
                .create(elementAtOrDefault(w, -1, 0));

        try {
            Iterator<Integer> iter = OperationToIterator
                    .toIterator(observable);
            assertTrue(iter.hasNext());
            iter.next();
            fail("expect an IndexOutOfBoundsException when index is out of bounds");
        } catch (IndexOutOfBoundsException e) {
        }
    }
}
