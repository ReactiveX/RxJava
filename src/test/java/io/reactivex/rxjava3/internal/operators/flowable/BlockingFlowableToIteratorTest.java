/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.operators.flowable.BlockingFlowableIterable.BlockingFlowableIterator;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BlockingFlowableToIteratorTest extends RxJavaTest {

    @Test
    public void toIterator() {
        Flowable<String> obs = Flowable.just("one", "two", "three");

        Iterator<String> it = obs.blockingIterable().iterator();

        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        assertTrue(it.hasNext());
        assertEquals("two", it.next());

        assertTrue(it.hasNext());
        assertEquals("three", it.next());

        assertFalse(it.hasNext());

    }

    @Test(expected = TestException.class)
    public void toIteratorWithException() {
        Flowable<String> obs = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                subscriber.onNext("one");
                subscriber.onError(new TestException());
            }
        });

        Iterator<String> it = obs.blockingIterable().iterator();

        assertTrue(it.hasNext());
        assertEquals("one", it.next());

        assertTrue(it.hasNext());
        it.next();
    }

    @Test
    public void iteratorExertBackpressure() {
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

    @Test(expected = UnsupportedOperationException.class)
    public void remove() {
        BlockingFlowableIterator<Integer> it = new BlockingFlowableIterator<>(128);
        it.remove();
    }

    @Test
    public void dispose() {
        BlockingFlowableIterator<Integer> it = new BlockingFlowableIterator<>(128);

        assertFalse(it.isDisposed());

        it.dispose();

        assertTrue(it.isDisposed());
    }

    @Test
    public void interruptWait() {
        BlockingFlowableIterator<Integer> it = new BlockingFlowableIterator<>(128);

        try {
            Thread.currentThread().interrupt();

            it.hasNext();
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyThrowsNoSuch() {
        BlockingFlowableIterator<Integer> it = new BlockingFlowableIterator<>(128);
        it.onComplete();
        it.next();
    }

    @Test(expected = MissingBackpressureException.class)
    public void overflowQueue() {
        Iterator<Integer> it = new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
            }
        }
        .blockingIterable(1)
        .iterator();

        it.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void disposedIteratorHasNextReturns() {
        Iterator<Integer> it = PublishProcessor.<Integer>create()
                .blockingIterable().iterator();
        ((Disposable)it).dispose();
        assertFalse(it.hasNext());
        it.next();
    }

    @Test
    public void asyncDisposeUnblocks() {
        final Iterator<Integer> it = PublishProcessor.<Integer>create()
                .blockingIterable().iterator();

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                ((Disposable)it).dispose();
            }
        }, 1, TimeUnit.SECONDS);

        assertFalse(it.hasNext());
    }
}
