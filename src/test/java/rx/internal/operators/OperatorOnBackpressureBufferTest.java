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
package rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorOnBackpressureBufferTest {

    @Test
    public void testNoBackpressureSupport() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>();
        // this will be ignored
        ts.requestMore(100);
        // we take 500 so it unsubscribes
        infinite.take(500).subscribe(ts);
        // it completely ignores the `request(100)` and we get 500
        assertEquals(500, ts.getOnNextEvents().size());
        ts.assertNoErrors();
    }

    @Test(timeout = 500)
    public void testFixBackpressureWithBuffer() throws InterruptedException {
        final CountDownLatch l1 = new CountDownLatch(100);
        final CountDownLatch l2 = new CountDownLatch(150);
        TestSubscriber<Long> ts = new TestSubscriber<Long>(new Observer<Long>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Long t) {
                l1.countDown();
                l2.countDown();
            }

        });
        // this will be ignored
        ts.requestMore(100);
        // we take 500 so it unsubscribes
        infinite.subscribeOn(Schedulers.computation()).onBackpressureBuffer().take(500).subscribe(ts);
        // it completely ignores the `request(100)` and we get 500
        l1.await();
        assertEquals(100, ts.getOnNextEvents().size());
        ts.requestMore(50);
        l2.await();
        assertEquals(150, ts.getOnNextEvents().size());
        ts.requestMore(350);
        ts.awaitTerminalEvent();
        assertEquals(500, ts.getOnNextEvents().size());
        ts.assertNoErrors();
        assertEquals(0, ts.getOnNextEvents().get(0).intValue());
        assertEquals(499, ts.getOnNextEvents().get(499).intValue());
    }

    static final Observable<Long> infinite = Observable.create(new OnSubscribe<Long>() {

        @Override
        public void call(Subscriber<? super Long> s) {
            long i = 0;
            while (!s.isUnsubscribed()) {
                s.onNext(i++);
            }
        }

    });
}
