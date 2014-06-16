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
package rx.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public abstract class RxRingBufferTest {

    protected abstract RxRingBuffer createRingBuffer();

    @Test
    public void missingBackpressureException() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        b.requestIfNeeded(s);
        for (int i = 0; i < Producer.BUFFER_SIZE; i++) {
            b.onNext("one");
        }
        try {
            b.onNext("o");
            fail("expected failure adding beyond size");
        } catch (Exception e) {
            // expecting failure
            assertTrue(e instanceof MissingBackpressureException);
        }
    }

    @Test
    public void addAndPollFailBackpressure() throws MissingBackpressureException {

        RxRingBuffer b = createRingBuffer();

        TestSubscriber<Object> s = new TestSubscriber<Object>();
        try {
            for (int i = 0; i < Producer.BUFFER_SIZE; i++) {
                //                System.out.println("Add: " + i);
                b.onNext("one");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        b.poll();
        b.onNext("o");
        try {
            b.onNext("o");
            fail("expected failure adding beyond size");
        } catch (Exception e) {
            // expecting failure
            assertTrue(e instanceof MissingBackpressureException);
        }
    }

    @Test
    public void addAndPoll() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        b.onNext("o");
        b.onNext("o");
        b.poll();
        b.poll();
        b.onNext("o");
        b.onNext("o");
    }

    /**
     * assert that we only request when we have space (items not yet polled).
     */
    @Test
    public void onNextPollRequestCycle() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        assertEquals(Producer.BUFFER_SIZE, b.available());
        assertEquals(0, b.requested());
        b.requestIfNeeded(s);
        assertEquals(Producer.BUFFER_SIZE, b.requested());
        b.onNext("a");
        assertEquals(Producer.BUFFER_SIZE - 1, b.available());
        b.onNext("a");
        assertEquals(Producer.BUFFER_SIZE - 2, b.available());
        // fill the queue
        for (int i = 0; i < Producer.BUFFER_SIZE - 2; i++) {
            b.onNext("one");
        }
        // now full and no outstanding requests
        assertEquals(0, b.requested());
        b.poll();
        assertEquals(1, b.available());
        // it should still be 0 requested as we haven't requested more
        assertEquals(0, b.requested());
        b.requestIfNeeded(s);
        // still only 0 as we have not dropped below the threshold
        assertEquals(0, b.requested());
        // drain to threshold
        final int DIFF = Producer.BUFFER_SIZE - RxRingBuffer.THRESHOLD - 1;
        System.out.println("DIFF: " + DIFF);
        for (int i = 0; i < DIFF; i++) {
            b.poll();
        }
        // DIFF+1 as we polled 1 above        
        assertEquals(DIFF + 1, b.available());
        assertEquals(Producer.BUFFER_SIZE - DIFF - 1, b.count());
        assertEquals(0, b.requested());
        System.out.println("---> Count: " + b.count() + "  Requested: " + b.requested());
        b.requestIfNeeded(s);
        // should still be the same since we're at the threshold
        assertEquals(DIFF + 1, b.available());
        System.out.println("---> Count: " + b.count() + "  Requested: " + b.requested());
        assertEquals(0, b.requested());
        // drop below threshold
        b.poll();
        System.out.println("---> Count: " + b.count() + "  Requested: " + b.requested());
        b.requestIfNeeded(s);
        System.out.println("---> Count: " + b.count() + "  Requested: " + b.requested());
        // the diff of SIZE - THRESHOLD + the one that dropped below the threshold is how much we will have requested
        assertEquals(DIFF + 2, b.requested());
        assertEquals(DIFF + 2, b.available());
        for (int i = 0; i < DIFF + 2; i++) {
            b.onNext("a");
        }
        // we are full and all requests are fulfilled
        assertEquals(0, b.requested());
        assertEquals(0, b.available());
        b.poll();
        assertEquals(1, b.available());
        // requested is still 0 as we have polled 1, but not requested any further
        assertEquals(0, b.requested());
        // we haven't requested anything so nothing should be sent, but there is space available so we can send
        b.onNext("a");
        assertEquals(0, b.available());
        assertEquals(0, b.requested());
        b.poll();
        assertEquals(1, b.available());
        b.poll();
        assertEquals(2, b.available());
        // finish draining
        for (int i = 0; i < Producer.BUFFER_SIZE - 2; i++) {
            b.poll();
        }
        // empty, both spots available, but nothing requested
        assertEquals(Producer.BUFFER_SIZE, b.available());
        assertEquals(0, b.requested());
        b.requestIfNeeded(s);
        assertEquals(Producer.BUFFER_SIZE, b.requested());
    }

    @Test
    public void roomForError() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        b.requestIfNeeded(s);
        for (int i = 0; i < Producer.BUFFER_SIZE; i++) {
            b.onNext("one");
        }
        // should act full now
        try {
            b.onNext("should-fail");
            fail("expected error");
        } catch (Exception e) {
            // we want this
            b.onError(new MissingBackpressureException());
        }
    }

    @Test
    public void multipleTerminalEventsOnComplete() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        b.requestIfNeeded(s);
        for (int i = 0; i < Producer.BUFFER_SIZE; i++) {
            b.onNext("one");
        }
        // queue is now full
        b.onError(new RuntimeException("an error"));
        try {
            b.onCompleted();
            // we ignore duplicate terminal events
        } catch (IllegalStateException e) {
            fail("we will ignore duplicate terminal events");
        }
    }

    @Test
    public void multipleTerminalEventsOnError() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        b.requestIfNeeded(s);
        for (int i = 0; i < Producer.BUFFER_SIZE; i++) {
            b.onNext("one");
        }
        // queue is now full
        b.onCompleted();
        try {
            b.onError(new RuntimeException("an error"));
            // we ignore duplicate terminal events
        } catch (IllegalStateException e) {
            fail("we will ignore duplicate terminal events");
        }
    }

    @Test(timeout = 500)
    public void testPollingTerminalState() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        b.onNext(1);
        b.onCompleted();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        Object o = null;
        while ((o = b.poll()) != null) {
            if (b.isCompleted(o)) {
                s.onCompleted();
            } else {
                s.onNext(o);
            }
        }

        s.awaitTerminalEvent();
        s.assertReceivedOnNext(Arrays.<Object> asList(1));
    }

}
