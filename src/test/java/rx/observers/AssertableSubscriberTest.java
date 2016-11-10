/**
 * Copyright 2016 Netflix, Inc.
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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.*;
import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.*;

public class AssertableSubscriberTest {

    @Test
    public void testManyMaxValue() {
        AssertableSubscriber<Integer> ts = Observable.just(1, 2, 3)
            .test()
            .assertValues(1, 2, 3)
            .awaitTerminalEvent()
            .awaitTerminalEvent(5, TimeUnit.SECONDS)
            .awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS)
            .assertCompleted()
            .assertTerminalEvent()
            .assertNoErrors()
            .assertUnsubscribed()
            .assertReceivedOnNext(Arrays.asList(1, 2, 3))
            .assertValueCount(3);
        assertEquals(3, ts.getValueCount());
        assertEquals(1, ts.getCompletions());
        assertEquals(Thread.currentThread().getName(),  ts.getLastSeenThread().getName());
        assertEquals(Arrays.asList(1,2,3), ts.getOnNextEvents());
        ts.awaitValueCount(3, 5, TimeUnit.SECONDS);
    }

    @Test
    public void testManyWithInitialRequest() {
        AssertableSubscriber<Integer> ts = Observable.just(1, 2, 3)
            .test(1)
            .assertValue(1)
            .assertValues(1)
            .assertValuesAndClear(1)
            .assertNotCompleted()
            .assertNoErrors()
            .assertNoTerminalEvent()
            .requestMore(1)
            .assertValuesAndClear(2);
        ts.unsubscribe();
        ts.assertUnsubscribed();
        assertTrue(ts.isUnsubscribed());
    }

    @Test
    public void testEmpty() {
        Observable.empty()
            .test()
            .assertNoValues();
    }

    @Test
    public void testError() {
        IOException e = new IOException();
        AssertableSubscriber<Object> ts = Observable.error(e)
            .test()
            .assertError(e)
            .assertError(IOException.class);
        assertEquals(Arrays.asList(e), ts.getOnErrorEvents());
    }

    @Test
    public void toStringIsNotNull() {
        AssertableSubscriber<Object> ts = Observable.empty().test();
        assertNotNull(ts.toString());
    }

    @Test
    public void testPerform() {
        final AtomicBoolean performed = new AtomicBoolean(false);
        Observable.empty()
            .test()
            .perform(new Action0() {
                @Override
                public void call() {
                    performed.set(true);
                }
            });
        assertTrue(performed.get());
    }

    @Test
    public void testCompletable() {
        AssertableSubscriber<Void> ts = Completable
            .fromAction(Actions.empty())
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertValueCount(0)
            .assertValues()
            .assertTerminalEvent()
            .assertReceivedOnNext(Collections.<Void>emptyList())
            .awaitTerminalEvent()
            .awaitTerminalEvent(5, TimeUnit.SECONDS)
            .awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS)
            .assertCompleted()
            .assertUnsubscribed();
        assertEquals(1, ts.getCompletions());
        assertEquals(Thread.currentThread().getName(), ts.getLastSeenThread().getName());
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertTrue(ts.getOnNextEvents().isEmpty());
        assertEquals(0, ts.getValueCount());
    }

    @Test
    public void testSingle() {
        AssertableSubscriber<Integer> ts = Single
            .just(10)
            .test()
            .assertValue(10)
            .assertValues(10)
            .assertValueCount(1)
            .assertReceivedOnNext(Arrays.asList(10))
            .assertValuesAndClear(10)
            .assertNoValues()
            .assertTerminalEvent()
            .assertNoErrors()
            .assertCompleted()
            .assertUnsubscribed()
            .awaitTerminalEvent()
            .awaitTerminalEvent(5, TimeUnit.SECONDS)
            .awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS);
       assertEquals(1, ts.getCompletions());
       assertEquals(Thread.currentThread().getName(), ts.getLastSeenThread().getName());
       assertTrue(ts.getOnErrorEvents().isEmpty());
       assertTrue(ts.getOnNextEvents().isEmpty());
       assertEquals(1, ts.getValueCount());
    }

    @Test
    public void assertResult() {
        Observable.just(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void assertResultFail() {
        try {
            Observable.just(1)
            .test()
            .assertResult(2);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.just(1)
            .test()
            .assertResult();
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.never()
            .test()
            .assertResult(1);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.error(new TestException())
            .test()
            .assertResult(2);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertFailure() {
        Observable.error(new TestException())
        .test()
        .assertFailure(TestException.class);

        Observable.just(1).concatWith(Observable.<Integer>error(new TestException()))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void assertFailureFail() {
        try {
            Observable.error(new TestException())
            .test()
            .assertFailure(IOException.class);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.just(1)
            .test()
            .assertFailure(IOException.class);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.just(1)
            .test()
            .assertFailure(IOException.class, 1);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.empty()
            .test()
            .assertFailure(IOException.class, 1);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }
    }

    @Test
    public void assertFailureAndMessage() {
        Observable.error(new TestException("forced failure"))
        .test()
        .assertFailureAndMessage(TestException.class, "forced failure");

        Observable.just(1).concatWith(Observable.<Integer>error(new TestException("forced failure 2")))
        .test()
        .assertFailureAndMessage(TestException.class, "forced failure 2", 1);
    }

    @Test
    public void assertFailureAndMessageFail() {
        try {
            Observable.error(new TestException())
            .test()
            .assertFailureAndMessage(IOException.class, "forced failure");
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }
        try {
            Observable.error(new TestException("forced failure"))
            .test()
            .assertFailureAndMessage(IOException.class, "forced failure");
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.just(1)
            .test()
            .assertFailureAndMessage(IOException.class, "forced failure");
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.just(1)
            .test()
            .assertFailureAndMessage(IOException.class, "forced failure", 1);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.empty()
            .test()
            .assertFailureAndMessage(IOException.class, "forced failure", 1);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.error(new TestException("failure forced"))
            .test()
            .assertFailureAndMessage(TestException.class, "forced failure");
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.just(1).concatWith(Observable.<Integer>error(new TestException("failure forced")))
            .test()
            .assertFailureAndMessage(TestException.class, "forced failure", 1);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }

        try {
            Observable.just(1).concatWith(Observable.<Integer>error(new TestException()))
            .test()
            .assertFailureAndMessage(TestException.class, "forced failure", 1);
            throw new RuntimeException("Should have thrown!"); // fail() doesn't work here because it also throws AssertionError and may look like the test passed
        } catch (AssertionError ex) {
            // expected
        }
    }
}

