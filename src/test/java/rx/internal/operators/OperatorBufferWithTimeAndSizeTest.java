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

import org.junit.Test;

import rx.Observable;
import rx.exceptions.MissingBackpressureException;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperatorBufferWithTimeAndSizeTest {
    @Test
    public void testNoRequest() {
        TestScheduler scheduler = new TestScheduler();
        Observable<List<Integer>> source = Observable.range(1, 100).buffer(100, TimeUnit.MILLISECONDS, 10, scheduler);
        
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0);
        
        source.subscribe(ts);
        
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotCompleted();
    }
    @Test
    public void testSingleRequest() {
        TestScheduler scheduler = new TestScheduler();
        Observable<List<Integer>> source = Observable.range(1, 100).buffer(100, TimeUnit.MILLISECONDS, 10, scheduler);
        
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0);
        
        source.subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotCompleted();

        ts.requestMore(1);
        
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        
        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
        
    }
    
    @Test
    public void testMissingBackpressure() {
        TestScheduler scheduler = new TestScheduler();
        PublishSubject<Integer> ps = PublishSubject.create();
        Observable<List<Integer>> source = ps.buffer(100, TimeUnit.MILLISECONDS, 10, scheduler);
        
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0);
        
        source.subscribe(ts);
        
        ps.onNext(1);
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(MissingBackpressureException.class);
    }

    @Test
    public void testSingleRequestWithTimeout() {
        TestScheduler scheduler = new TestScheduler();
        PublishSubject<Integer> ps = PublishSubject.create();
        Observable<List<Integer>> source = ps.buffer(100, TimeUnit.MILLISECONDS, 10, scheduler);
        
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0);
        
        source.subscribe(ts);
        
        ts.requestMore(1);
        
        ps.onNext(1);
        
        ts.assertNoValues();
        ts.assertNoTerminalEvent();
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        ts.assertValue(Arrays.asList(1));
        ts.assertNoTerminalEvent();

        ps.onNext(2);
        ps.onNext(3);
        
        ts.assertValue(Arrays.asList(1));
        ts.assertNoTerminalEvent();
    }
    @Test
    public void testUpstreamOverflowBuffer() {
        TestScheduler scheduler = new TestScheduler();
        PublishSubject<Integer> ps = PublishSubject.create();
        Observable<List<Integer>> source = ps.buffer(100, TimeUnit.MILLISECONDS, 10, scheduler);
        
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>(0);
        
        source.subscribe(ts);
        
        ts.requestMore(1);
        
        ps.onNext(1);
        
        ts.assertNoValues();
        ts.assertNoTerminalEvent();
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        ts.assertValue(Arrays.asList(1));
        ts.assertNoTerminalEvent();

        for (int i = 2; i <= 11; i++) {
            ps.onNext(i);
        }
        
        ts.assertValue(Arrays.asList(1));
        ts.assertError(MissingBackpressureException.class);
    }
}
