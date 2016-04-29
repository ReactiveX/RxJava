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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

public class OperatorTakeLastOneTest {

    @Test
    public void testLastOfManyReturnsLast() {
        TestSubscriber<Integer> s = new TestSubscriber<Integer>();
        Observable.range(1, 10).takeLast(1).subscribe(s);
        s.assertReceivedOnNext(Arrays.asList(10));
        s.assertNoErrors();
        s.assertTerminalEvent();
        s.assertUnsubscribed();
    }

    @Test
    public void testLastOfEmptyReturnsEmpty() {
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        Observable.empty().takeLast(1).subscribe(s);
        s.assertReceivedOnNext(Collections.emptyList());
        s.assertNoErrors();
        s.assertTerminalEvent();
        s.assertUnsubscribed();
    }

    @Test
    public void testLastOfOneReturnsLast() {
        TestSubscriber<Integer> s = new TestSubscriber<Integer>();
        Observable.just(1).takeLast(1).subscribe(s);
        s.assertReceivedOnNext(Arrays.asList(1));
        s.assertNoErrors();
        s.assertTerminalEvent();
        s.assertUnsubscribed();
    }

    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Action0 unsubscribeAction = new Action0() {
            @Override
            public void call() {
                unsubscribed.set(true);
            }
        };
        Observable.just(1).doOnUnsubscribe(unsubscribeAction)
                .takeLast(1).subscribe();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void testLastWithBackpressure() {
        MySubscriber<Integer> s = new MySubscriber<Integer>(0);
        Observable.just(1).takeLast(1).subscribe(s);
        assertEquals(0, s.list.size());
        s.requestMore(1);
        assertEquals(1, s.list.size());
    }
    
    @Test
    public void testTakeLastZeroProcessesAllItemsButIgnoresThem() {
        final AtomicInteger upstreamCount = new AtomicInteger();
        final int num = 10;
        int count = Observable.range(1,num).doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer t) {
                upstreamCount.incrementAndGet();
            }})
            .takeLast(0).count().toBlocking().single();
        assertEquals(num, upstreamCount.get());
        assertEquals(0, count);
    }
    
    private static class MySubscriber<T> extends Subscriber<T> {

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
        public void onCompleted() {

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
