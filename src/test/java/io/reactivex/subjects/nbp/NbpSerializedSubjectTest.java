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

package io.reactivex.subjects.nbp;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.exceptions.TestException;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpSerializedSubjectTest {

    @Test
    public void testBasic() {
        NbpSerializedSubject<String, String> subject = new NbpSerializedSubject<>(NbpPublishSubject.<String> create());
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        subject.subscribe(ts);
        subject.onNext("hello");
        subject.onComplete();
        ts.awaitTerminalEvent();
        ts.assertValue("hello");
    }
    
    @Test
    public void testAsyncSubjectValueRelay() {
        NbpAsyncSubject<Integer> async = NbpAsyncSubject.create();
        async.onNext(1);
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, serial.getValue());
        assertTrue(serial.hasValue());
        assertArrayEquals(new Object[] { 1 }, serial.getValues());
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testAsyncSubjectValueEmpty() {
        NbpAsyncSubject<Integer> async = NbpAsyncSubject.create();
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testAsyncSubjectValueError() {
        NbpAsyncSubject<Integer> async = NbpAsyncSubject.create();
        TestException te = new TestException();
        async.onError(te);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testPublishSubjectValueRelay() {
        NbpPublishSubject<Integer> async = NbpPublishSubject.create();
        async.onNext(1);
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        
        assertArrayEquals(new Object[0], serial.getValues());
        assertArrayEquals(new Integer[0], serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    
    @Test
    public void testPublishSubjectValueEmpty() {
        NbpPublishSubject<Integer> async = NbpPublishSubject.create();
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testPublishSubjectValueError() {
        NbpPublishSubject<Integer> async = NbpPublishSubject.create();
        TestException te = new TestException();
        async.onError(te);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }

    @Test
    public void testBehaviorSubjectValueRelay() {
        NbpBehaviorSubject<Integer> async = NbpBehaviorSubject.create();
        async.onNext(1);
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectValueRelayIncomplete() {
        NbpBehaviorSubject<Integer> async = NbpBehaviorSubject.create();
        async.onNext(1);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, serial.getValue());
        assertTrue(serial.hasValue());
        assertArrayEquals(new Object[] { 1 }, serial.getValues());
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectIncompleteEmpty() {
        NbpBehaviorSubject<Integer> async = NbpBehaviorSubject.create();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectEmpty() {
        NbpBehaviorSubject<Integer> async = NbpBehaviorSubject.create();
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testBehaviorSubjectError() {
        NbpBehaviorSubject<Integer> async = NbpBehaviorSubject.create();
        TestException te = new TestException();
        async.onError(te);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    
    @Test
    public void testReplaySubjectValueRelay() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.create();
        async.onNext(1);
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, serial.getValue());
        assertTrue(serial.hasValue());
        assertArrayEquals(new Object[] { 1 }, serial.getValues());
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayIncomplete() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.create();
        async.onNext(1);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, serial.getValue());
        assertTrue(serial.hasValue());
        assertArrayEquals(new Object[] { 1 }, serial.getValues());
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBounded() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, serial.getValue());
        assertTrue(serial.hasValue());
        assertArrayEquals(new Object[] { 1 }, serial.getValues());
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBoundedIncomplete() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertEquals((Integer)1, serial.getValue());
        assertTrue(serial.hasValue());
        assertArrayEquals(new Object[] { 1 }, serial.getValues());
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { 1 }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { 1, null }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayBoundedEmptyIncomplete() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.createWithSize(1);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectValueRelayEmptyIncomplete() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.create();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    
    @Test
    public void testReplaySubjectEmpty() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.create();
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectError() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.create();
        TestException te = new TestException();
        async.onError(te);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    
    @Test
    public void testReplaySubjectBoundedEmpty() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.createWithSize(1);
        async.onComplete();
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertTrue(serial.hasComplete());
        assertFalse(serial.hasThrowable());
        assertNull(serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    @Test
    public void testReplaySubjectBoundedError() {
        NbpReplaySubject<Integer> async = NbpReplaySubject.createWithSize(1);
        TestException te = new TestException();
        async.onError(te);
        NbpSubject<Integer, Integer> serial = async.toSerialized();
        
        assertFalse(serial.hasSubscribers());
        assertFalse(serial.hasComplete());
        assertTrue(serial.hasThrowable());
        assertSame(te, serial.getThrowable());
        assertNull(serial.getValue());
        assertFalse(serial.hasValue());
        assertArrayEquals(new Object[] { }, serial.getValues());
        assertArrayEquals(new Integer[] { }, serial.getValues(new Integer[0]));
        assertArrayEquals(new Integer[] { null }, serial.getValues(new Integer[] { 0 }));
        assertArrayEquals(new Integer[] { null, 0 }, serial.getValues(new Integer[] { 0, 0 }));
    }
    
    @Test
    public void testDontWrapNbpSerializedSubjectAgain() {
        NbpPublishSubject<Object> s = NbpPublishSubject.create();
        NbpSubject<Object, Object> s1 = s.toSerialized();
        NbpSubject<Object, Object> s2 = s1.toSerialized();
        assertSame(s1, s2);
    }
}