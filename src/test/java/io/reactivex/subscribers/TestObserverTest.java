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

package io.reactivex.subscribers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.exceptions.TestException;
import io.reactivex.subjects.PublishSubject;

public class TestObserverTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAssert() {
        Observable<Integer> oi = Observable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<>();
        oi.subscribe(o);

        o.assertValues(1, 2);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testAssertNotMatchCount() {
        Observable<Integer> oi = Observable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        // FIXME different message format
//        thrown.expectMessage("Number of items does not match. Provided: 1  Actual: 2");

        o.assertValue(1);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testAssertNotMatchValue() {
        Observable<Integer> oi = Observable.fromIterable(Arrays.asList(1, 2));
        TestSubscriber<Integer> o = new TestSubscriber<>();
        oi.subscribe(o);

        thrown.expect(AssertionError.class);
        // FIXME different message format
//        thrown.expectMessage("Value at index: 1 expected to be [3] (Integer) but was: [2] (Integer)");

        o.assertValues(1, 3);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testAssertTerminalEventNotReceived() {
        PublishSubject<Integer> p = PublishSubject.create();
        TestSubscriber<Integer> o = new TestSubscriber<>();
        p.subscribe(o);

        p.onNext(1);
        p.onNext(2);

        thrown.expect(AssertionError.class);
        // FIXME different message format
//        thrown.expectMessage("No terminal events received.");

        o.assertValues(1, 2);
        o.assertValueCount(2);
        o.assertTerminated();
    }

    @Test
    public void testWrappingMock() {
        Observable<Integer> oi = Observable.fromIterable(Arrays.asList(1, 2));

        Subscriber<Integer> mockObserver = TestHelper.mockSubscriber();
        
        oi.subscribe(new TestSubscriber<>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onNext(2);
        inOrder.verify(mockObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testWrappingMockWhenUnsubscribeInvolved() {
        Observable<Integer> oi = Observable.fromIterable(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)).take(2);
        Subscriber<Integer> mockObserver = TestHelper.mockSubscriber();
        oi.subscribe(new TestSubscriber<>(mockObserver));

        InOrder inOrder = inOrder(mockObserver);
        inOrder.verify(mockObserver, times(1)).onNext(1);
        inOrder.verify(mockObserver, times(1)).onNext(2);
        inOrder.verify(mockObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testErrorSwallowed() {
        Observable.error(new RuntimeException()).subscribe(new TestSubscriber<>());
    }
    
    @Test
    public void testGetEvents() {
        TestSubscriber<Integer> to = new TestSubscriber<>();
        to.onNext(1);
        to.onNext(2);
        
        assertEquals(Arrays.<Object>asList(Arrays.asList(1, 2), 
                Collections.emptyList(), 
                Collections.emptyList()), to.getEvents());
        
        to.onComplete();
        
        assertEquals(Arrays.<Object>asList(Arrays.asList(1, 2), Collections.emptyList(),
                Collections.singletonList(Notification.complete())), to.getEvents());
        
        TestException ex = new TestException();
        TestSubscriber<Integer> to2 = new TestSubscriber<>();
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
        TestSubscriber<Integer> to = new TestSubscriber<>();
        to.onNext(1);

        try {
            to.assertValue(null);
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Null element check assertion didn't happen!");
    }
    
    @Test
    public void testNullActual() {
        TestSubscriber<Integer> to = new TestSubscriber<>();
        to.onNext(null);

        try {
            to.assertValue(1);
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Null element check assertion didn't happen!");
    }
    
    @Test
    public void testTerminalErrorOnce() {
        TestSubscriber<Integer> to = new TestSubscriber<>();
        to.onError(new TestException());
        to.onError(new TestException());
        
        try {
            to.assertTerminated();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple onError terminal events!");
    }
    @Test
    public void testTerminalCompletedOnce() {
        TestSubscriber<Integer> to = new TestSubscriber<>();
        to.onComplete();
        to.onComplete();
        
        try {
            to.assertTerminated();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple onComplete terminal events!");
    }
    
    @Test
    public void testTerminalOneKind() {
        TestSubscriber<Integer> to = new TestSubscriber<>();
        to.onError(new TestException());
        to.onComplete();
        
        try {
            to.assertTerminated();
        } catch (AssertionError ex) {
            // this is expected
            return;
        }
        fail("Failed to report multiple kinds of events!");
    }
}