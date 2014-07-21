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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import rx.exceptions.MissingBackpressureException;
import rx.observers.TestSubscriber;

public abstract class RxRingBufferBase {

    protected abstract RxRingBuffer createRingBuffer();

    @Test
    public void missingBackpressureException() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        s.requestMore(RxRingBuffer.SIZE);
        for (int i = 0; i < RxRingBuffer.SIZE; i++) {
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

        try {
            for (int i = 0; i < RxRingBuffer.SIZE; i++) {
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
        b.onNext("o");
        b.onNext("o");
        b.poll();
        b.poll();
        b.onNext("o");
        b.onNext("o");
    }

    @Test
    public void roomForError() throws MissingBackpressureException {
        RxRingBuffer b = createRingBuffer();
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        s.requestMore(RxRingBuffer.SIZE);
        for (int i = 0; i < RxRingBuffer.SIZE; i++) {
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
        s.requestMore(RxRingBuffer.SIZE);
        for (int i = 0; i < RxRingBuffer.SIZE; i++) {
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
        s.requestMore(RxRingBuffer.SIZE);
        for (int i = 0; i < RxRingBuffer.SIZE; i++) {
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
