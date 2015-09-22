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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.exceptions.TestException;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorIgnoreElementsTest {

    @Test
    public void testWithEmpty() {
        assertTrue(NbpObservable.empty().ignoreElements().isEmpty().toBlocking().single());
    }

    @Test
    public void testWithNonEmpty() {
        assertTrue(NbpObservable.just(1, 2, 3).ignoreElements().isEmpty().toBlocking().single());
    }

    @Test
    public void testUpstreamIsProcessedButIgnored() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        long count = NbpObservable.range(1, num)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                .ignoreElements()
                .count().toBlocking().single();
        assertEquals(num, upstreamCount.get());
        assertEquals(0, count);
    }
    
    @Test
    public void testCompletedOk() {
        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>();
        NbpObservable.range(1, 10).ignoreElements().subscribe(ts);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
    }
    
    @Test
    public void testErrorReceived() {
        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>();
        TestException ex = new TestException("boo");
        NbpObservable.error(ex).ignoreElements().subscribe(ts);
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
        ts.assertError(TestException.class);
        ts.assertErrorMessage("boo");
    }
    
    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsub = new AtomicBoolean();
        NbpObservable.range(1, 10).doOnCancel(new Runnable() {
            @Override
            public void run() {
                unsub.set(true);
            }})
            .subscribe();
        assertTrue(unsub.get());
    }
}