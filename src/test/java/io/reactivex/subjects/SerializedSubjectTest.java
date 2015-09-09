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

package io.reactivex.subjects;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.exceptions.TestException;
import io.reactivex.subscribers.TestSubscriber;

public class SerializedSubjectTest {

    @Test
    public void testBasic() {
        SerializedSubject<String, String> subject = new SerializedSubject<>(PublishSubject.<String> create());
        TestSubscriber<String> ts = new TestSubscriber<>();
        subject.subscribe(ts);
        subject.onNext("hello");
        subject.onComplete();
        ts.awaitTerminalEvent();
        ts.assertValue("hello");
    }
    
    @Test
    public void testAsyncSubjectValueRelay() {
        AsyncSubject<Integer> async = AsyncSubject.create();
        async.onNext(1);
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        AsyncSubject<Integer> async = AsyncSubject.create();
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        AsyncSubject<Integer> async = AsyncSubject.create();
        TestException te = new TestException();
        async.onError(te);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        PublishSubject<Integer> async = PublishSubject.create();
        async.onNext(1);
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        PublishSubject<Integer> async = PublishSubject.create();
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        PublishSubject<Integer> async = PublishSubject.create();
        TestException te = new TestException();
        async.onError(te);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        async.onNext(1);
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        async.onNext(1);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        BehaviorSubject<Integer> async = BehaviorSubject.create();
        TestException te = new TestException();
        async.onError(te);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.create();
        async.onNext(1);
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.create();
        async.onNext(1);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
        async.onNext(0);
        async.onNext(1);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.create();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.create();
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.create();
        TestException te = new TestException();
        async.onError(te);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
        async.onComplete();
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
        ReplaySubject<Integer> async = ReplaySubject.createWithSize(1);
        TestException te = new TestException();
        async.onError(te);
        Subject<Integer, Integer> serial = async.toSerialized();
        
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
    public void testDontWrapSerializedSubjectAgain() {
        PublishSubject<Object> s = PublishSubject.create();
        Subject<Object, Object> s1 = s.toSerialized();
        Subject<Object, Object> s2 = s1.toSerialized();
        assertSame(s1, s2);
    }
}