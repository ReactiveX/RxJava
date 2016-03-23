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

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Consumer;
import io.reactivex.subscribers.*;

public class OperatorIgnoreElementsTest {

    @Test
    public void testWithEmpty() {
        assertTrue(Flowable.empty().ignoreElements().isEmpty().toBlocking().single());
    }

    @Test
    public void testWithNonEmpty() {
        assertTrue(Flowable.just(1, 2, 3).ignoreElements().isEmpty().toBlocking().single());
    }

    @Test
    public void testUpstreamIsProcessedButIgnored() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        long count = Flowable.range(1, num)
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
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        Flowable.range(1, 10).ignoreElements().subscribe(ts);
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertTerminated();
        // FIXME no longer testable
//        ts.assertUnsubscribed();
    }
    
    @Test
    public void testErrorReceived() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        TestException ex = new TestException("boo");
        Flowable.error(ex).ignoreElements().subscribe(ts);
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
        Flowable.range(1, 10).doOnCancel(new Runnable() {
            @Override
            public void run() {
                unsub.set(true);
            }})
            .subscribe();
        assertTrue(unsub.get());
    }

    @Test(timeout = 10000)
    public void testDoesNotHangAndProcessesAllUsingBackpressure() {
        final AtomicInteger upstreamCount = new AtomicInteger();
        final AtomicInteger count = new AtomicInteger(0);
        int num = 10;
        Flowable.range(1, num)
        //
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                //
                .ignoreElements()
                //
                .doOnNext(new Consumer<Integer>() {

                    @Override
                    public void accept(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                //
                .subscribe(new DefaultObserver<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                    }
                });
        assertEquals(num, upstreamCount.get());
        assertEquals(0, count.get());
    }
    
}