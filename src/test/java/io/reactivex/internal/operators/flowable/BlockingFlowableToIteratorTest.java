/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;

public class BlockingFlowableToIteratorTest {

    @Test
    public void testToIterator() {
        Flowable<String> obs = Flowable.just("one", "two", "three");

        Iterator<String> it = obs.blockingIterable().iterator();

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        assertEquals("two", it.next());

        assertEquals(true, it.hasNext());
        assertEquals("three", it.next());

        assertEquals(false, it.hasNext());

    }

    @Test(expected = TestException.class)
    public void testToIteratorWithException() {
        Flowable<String> obs = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                observer.onNext("one");
                observer.onError(new TestException());
            }
        });

        Iterator<String> it = obs.blockingIterable().iterator();

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        it.next();
    }

    @Ignore("subscribe() should not throw")
    @Test(expected = TestException.class)
    public void testExceptionThrownFromOnSubscribe() {
        Iterable<String> strings = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                throw new TestException("intentional");
            }
        }).blockingIterable();
        
        for (String string : strings) {
            // never reaches here
            System.out.println(string);
        }
    }
    
    @Ignore("This is not a separate class anymore")
    @Test
    public void constructorShouldBePrivate() {
        // TestHelper.checkUtilityClass(BlockingOperatorToIterator.class);
    }
    
    @Test
    public void testIteratorExertBackpressure() {
        final Counter src = new Counter();

        Flowable<Integer> obs = Flowable.fromIterable(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return src;
            }
        });

        Iterator<Integer> it = obs.blockingIterable().iterator();
        while (it.hasNext()) {
            // Correct backpressure should cause this interleaved behavior.
            // We first request RxRingBuffer.SIZE. Then in increments of
            // SubscriberIterator.LIMIT.
            int i = it.next();
            int expected = i - (i % (Flowable.bufferSize() - (Flowable.bufferSize() >> 2))) + Flowable.bufferSize();
            expected = Math.min(expected, Counter.MAX);

            assertEquals(expected, src.count);
        }
    }
    
    public static final class Counter implements Iterator<Integer> {
        static final int MAX = 5 * Flowable.bufferSize();
        public int count;

        @Override
        public boolean hasNext() {
            return count < MAX;
        }

        @Override
        public Integer next() {
            return ++count;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}