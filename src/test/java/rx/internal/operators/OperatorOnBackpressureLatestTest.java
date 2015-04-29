/**
 * Copyright 2014 Netflix, Inc.
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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OperatorOnBackpressureLatestTest {
    @Test
    public void testSimple() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.range(1, 5).onBackpressureLatest().subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5));
    }
    @Test
    public void testSimpleError() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.range(1, 5).concatWith(Observable.<Integer>error(new TestException()))
        .onBackpressureLatest().subscribe(ts);
        
        ts.assertTerminalEvent();
        Assert.assertEquals(1, ts.getOnErrorEvents().size());
        Assert.assertTrue(ts.getOnErrorEvents().get(0) instanceof TestException);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5));
    }
    @Test
    public void testSimpleBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                request(2);
            }
        };
        
        Observable.range(1, 5).onBackpressureLatest().subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertReceivedOnNext(Arrays.asList(1, 2));
        Assert.assertTrue(ts.getOnCompletedEvents().isEmpty());
    }
    @Test
    public void testSynchronousDrop() {
        PublishSubject<Integer> source = PublishSubject.create();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                request(0);
            }
        };
        
        source.onBackpressureLatest().subscribe(ts);

        ts.assertReceivedOnNext(Collections.<Integer>emptyList());

        source.onNext(1);
        ts.requestMore(2);
        
        ts.assertReceivedOnNext(Arrays.asList(1));
        
        source.onNext(2);

        ts.assertReceivedOnNext(Arrays.asList(1, 2));

        source.onNext(3);
        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        ts.requestMore(2);

        ts.assertReceivedOnNext(Arrays.asList(1, 2, 6));
        
        source.onNext(7);

        ts.assertReceivedOnNext(Arrays.asList(1, 2, 6, 7));
        
        source.onNext(8);
        source.onNext(9);
        source.onCompleted();
        
        ts.requestMore(1);
        
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 6, 7, 9));
        ts.assertNoErrors();
        ts.assertTerminalEvent();
    }
    @Test
    public void testAsynchronousDrop() throws InterruptedException {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            final Random rnd = new Random();
            @Override
            public void onStart() {
                request(1);
            }
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (rnd.nextDouble() < 0.001) {
                    try {
                        Thread.sleep(1);
                    } catch(InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                request(1);
            }
        };
        int m = 100000;
        Observable.range(1, m)
        .subscribeOn(Schedulers.computation())
        .onBackpressureLatest()
        .observeOn(Schedulers.io())
        .subscribe(ts);
        
        ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
        ts.assertTerminalEvent();
        int n = ts.getOnNextEvents().size();
        System.out.println("testAsynchronousDrop -> " + n);
        Assert.assertTrue("All events received?", n < m);
    }
}
