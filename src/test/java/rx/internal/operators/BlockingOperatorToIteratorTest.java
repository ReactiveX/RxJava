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
package rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static rx.internal.operators.BlockingOperatorToIterator.toIterator;

import java.util.Iterator;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.internal.operators.BlockingOperatorToIterator.SubscriberIterator;
import rx.internal.util.RxRingBuffer;

public class BlockingOperatorToIteratorTest {

    @Test
    public void testToIterator() {
        Observable<String> obs = Observable.just("one", "two", "three");

        Iterator<String> it = toIterator(obs);

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
        Observable<String> obs = Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onError(new TestException());
            }
        });

        Iterator<String> it = toIterator(obs);

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        it.next();
    }

    @Test(expected = TestException.class)
    public void testExceptionThrownFromOnSubscribe() {
        Iterable<String> strings = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                throw new TestException("intentional");
            }
        }).toBlocking().toIterable();
        for (String string : strings) {
            // never reaches here
            System.out.println(string);
        }
    }

    @Test
    public void testIteratorExertBackpressure() {
        final Counter src = new Counter();

        Observable<Integer> obs = Observable.from(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return src;
            }
        });

        Iterator<Integer> it = toIterator(obs);
        while (it.hasNext()) {
            // Correct backpressure should cause this interleaved behavior.
            // We first request RxRingBuffer.SIZE. Then in increments of
            // SubscriberIterator.LIMIT.
            int i = it.next();
            int expected = i - (i % SubscriberIterator.LIMIT) + RxRingBuffer.SIZE;
            expected = Math.min(expected, Counter.MAX);

            assertEquals(expected, src.count);
        }
    }

    public static final class Counter implements Iterator<Integer> {
        static final int MAX = 5 * RxRingBuffer.SIZE;
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
