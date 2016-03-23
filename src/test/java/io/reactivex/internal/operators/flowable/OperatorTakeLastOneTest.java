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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.subscribers.DefaultObserver;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorTakeLastOneTest {

    @Test
    public void testLastOfManyReturnsLast() {
        TestSubscriber<Integer> s = new TestSubscriber<Integer>();
        Flowable.range(1, 10).takeLast(1).subscribe(s);
        s.assertValue(10);
        s.assertNoErrors();
        s.assertTerminated();
        // NO longer assertable
//        s.assertUnsubscribed();
    }

    @Test
    public void testLastOfEmptyReturnsEmpty() {
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        Flowable.empty().takeLast(1).subscribe(s);
        s.assertNoValues();
        s.assertNoErrors();
        s.assertTerminated();
        // NO longer assertable
//      s.assertUnsubscribed();
    }

    @Test
    public void testLastOfOneReturnsLast() {
        TestSubscriber<Integer> s = new TestSubscriber<Integer>();
        Flowable.just(1).takeLast(1).subscribe(s);
        s.assertValue(1);
        s.assertNoErrors();
        s.assertTerminated();
        // NO longer assertable
//      s.assertUnsubscribed();
    }

    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Runnable unsubscribeAction = new Runnable() {
            @Override
            public void run() {
                unsubscribed.set(true);
            }
        };
        Flowable.just(1).doOnCancel(unsubscribeAction)
                .takeLast(1).subscribe();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void testLastWithBackpressure() {
        MySubscriber<Integer> s = new MySubscriber<Integer>(0);
        Flowable.just(1).takeLast(1).subscribe(s);
        assertEquals(0, s.list.size());
        s.requestMore(1);
        assertEquals(1, s.list.size());
    }
    
    @Test
    public void testTakeLastZeroProcessesAllItemsButIgnoresThem() {
        final AtomicInteger upstreamCount = new AtomicInteger();
        final int num = 10;
        long count = Flowable.range(1,num).doOnNext(new Consumer<Integer>() {

            @Override
            public void accept(Integer t) {
                upstreamCount.incrementAndGet();
            }})
            .takeLast(0).count().toBlocking().single();
        assertEquals(num, upstreamCount.get());
        assertEquals(0L, count);
    }
    
    private static class MySubscriber<T> extends DefaultObserver<T> {

        private long initialRequest;

        MySubscriber(long initialRequest) {
            this.initialRequest = initialRequest;
        }

        final List<T> list = new ArrayList<T>();

        public void requestMore(long n) {
            request(n);
        }

        @Override
        public void onStart() {
            request(initialRequest);
        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(T t) {
            list.add(t);
        }

    }

}