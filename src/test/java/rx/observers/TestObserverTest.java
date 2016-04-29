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
package rx.observers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.subjects.PublishSubject;

public class TestObserverTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAssert() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2));
        TestObserver<Integer> o = new TestObserver<Integer>();
        oi.subscribe(o);

        o.assertReceivedOnNext(Arrays.asList(1, 2));
        assertEquals(2, o.getOnNextEvents().size());
        o.assertTerminalEvent();
    }

    @Test
    public void testAssertNotMatchCount() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2));
        TestObserver<Integer> o = new TestObserver<Integer>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Number of items does not match. Provided: 1  Actual: 2.\n" +
                "Provided values: [1]\n" +
                "Actual values: [1, 2]");

        o.assertReceivedOnNext(Arrays.asList(1));
        assertEquals(2, o.getOnNextEvents().size());
        o.assertTerminalEvent();
    }

    @Test
    public void testAssertNotMatchValue() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2));
        TestObserver<Integer> o = new TestObserver<Integer>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Value at index: 1 expected to be [3] (Integer) but was: [2] (Integer)");

        o.assertReceivedOnNext(Arrays.asList(1, 3));
        assertEquals(2, o.getOnNextEvents().size());
        o.assertTerminalEvent();
    }

    @Test
    public void testAssertTerminalEventNotReceived() {
        PublishSubject<Integer> p = PublishSubject.create();
        TestObserver<Integer> o = new TestObserver<Integer>();
        p.subscribe(o);

        p.onNext(1);
        p.onNext(2);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("No terminal events received.");

        o.assertReceivedOnNext(Arrays.asList(1, 2));
        assertEquals(2, o.getOnNextEvents().size());
        o.assertTerminalEvent();
    }

    @Test
    public void testWrappingMock() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2));
        @SuppressWarnings("unchecked")
        Observer<Integer> mockObserver = mock(Observer.class);
        oi.subscribe(new TestObserver<Integer>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onNext(2);
        inOrder.verify(mockObserver, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testWrappingMockWhenUnsubscribeInvolved() {
        Observable<Integer> oi = Observable.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)).take(2);
        @SuppressWarnings("unchecked")
        Observer<Integer> mockObserver = mock(Observer.class);
        oi.subscribe(new TestObserver<Integer>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onNext(2);
        inOrder.verify(mockObserver, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testErrorSwallowed() {
        Observable.error(new RuntimeException()).subscribe(new TestObserver<Object>());
    }
    
    @Test
    public void testGetEvents() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        to.onNext(1);
        to.onNext(2);
        
        assertEquals(Arrays.<Object>asList(Arrays.asList(1, 2), 
                Collections.emptyList(), 
                Collections.emptyList()), to.getEvents());
        
        to.onCompleted();
        
        assertEquals(Arrays.<Object>asList(Arrays.asList(1, 2), Collections.emptyList(),
                Collections.singletonList(Notification.createOnCompleted())), to.getEvents());
        
        TestException ex = new TestException();
        TestObserver<Integer> to2 = new TestObserver<Integer>();
        to2.onNext(1);
        to2.onNext(2);
        
        assertEquals(Arrays.<Object>asList(Arrays.asList(1, 2), 
                Collections.emptyList(), 
                Collections.emptyList()), to2.getEvents());
        
        to2.onError(ex);
        
        assertEquals(Arrays.<Object>asList(
                Arrays.asList(1, 2),
                Collections.singletonList(ex),
                Collections.emptyList()), 
                    to2.getEvents());
    }

    @Test
    public void testNullExpected() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        to.onNext(1);

        try {
            to.assertReceivedOnNext(Arrays.asList((Integer)null));
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Null element check assertion didn't happen!");
    }
    
    @Test
    public void testNullActual() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        to.onNext(null);

        try {
            to.assertReceivedOnNext(Arrays.asList(1));
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Null element check assertion didn't happen!");
    }
    
    @Test
    public void testTerminalErrorOnce() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        to.onError(new TestException());
        to.onError(new TestException());
        
        try {
            to.assertTerminalEvent();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple onError terminal events!");
    }
    @Test
    public void testTerminalCompletedOnce() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        to.onCompleted();
        to.onCompleted();
        
        try {
            to.assertTerminalEvent();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple onError terminal events!");
    }
    
    @Test
    public void testTerminalOneKind() {
        TestObserver<Integer> to = new TestObserver<Integer>();
        to.onError(new TestException());
        to.onCompleted();
        
        try {
            to.assertTerminalEvent();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple kinds of events!");
    }
}
