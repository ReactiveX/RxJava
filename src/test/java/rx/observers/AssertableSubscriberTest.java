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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Action0;
import rx.functions.Actions;

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
        assertTrue(ts.awaitValueCount(3, 5, TimeUnit.SECONDS));
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
    
}

