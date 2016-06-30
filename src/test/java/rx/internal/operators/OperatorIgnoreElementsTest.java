/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

public class OperatorIgnoreElementsTest {

    @Test
    public void testWithEmpty() {
        assertTrue(Observable.empty().ignoreElements().isEmpty().toBlocking().single());
    }

    @Test
    public void testWithNonEmpty() {
        assertTrue(Observable.just(1, 2, 3).ignoreElements().isEmpty().toBlocking().single());
    }

    @Test
    public void testUpstreamIsProcessedButIgnored() {
        final int num = 10;
        final AtomicInteger upstreamCount = new AtomicInteger();
        int count = Observable.range(1, num)
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer t) {
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
        Observable.range(1, 10).ignoreElements().subscribe(ts);
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList());
        ts.assertTerminalEvent();
        ts.assertUnsubscribed();
    }
    
    @Test
    public void testErrorReceived() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        RuntimeException ex = new RuntimeException("boo");
        Observable.error(ex).ignoreElements().subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList());
        ts.assertTerminalEvent();
        ts.assertUnsubscribed();
        assertEquals(1, ts.getOnErrorEvents().size());
        assertEquals("boo", ts.getOnErrorEvents().get(0).getMessage());
    }
    
    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsub = new AtomicBoolean();
        Observable.range(1, 10).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
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
        Observable.range(1, num)
        //
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                //
                .ignoreElements()
                //
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer t) {
                        upstreamCount.incrementAndGet();
                    }
                })
                //
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onCompleted() {

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
