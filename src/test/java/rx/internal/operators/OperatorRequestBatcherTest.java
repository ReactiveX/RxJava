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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorRequestBatcherTest {
    @Test
    public void testMaxValueSubscriber() {
        final List<Long> requests = new ArrayList<Long>();
        
        Observable<Integer> source = Observable.range(0, 100)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testMaxValueSubscriber >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 5);
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        source.subscribe(ts);
        
        ts.assertValueCount(100);
        ts.assertCompleted();
        ts.assertNoErrors();
        
        Assert.assertEquals(Arrays.asList(20L, 15L, 15L, 15L, 15L, 15L, 15L), requests);
    }
    @Test
    public void testTotalGreaterValueSubscriber() {
        final List<Long> requests = new ArrayList<Long>();
        
        Observable<Integer> source = Observable.range(0, 100)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testTotalGreaterValueSubscriber >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 5);
        
        TestSubscriber<Integer> ts = TestSubscriber.create(200);
        
        source.subscribe(ts);
        
        ts.assertValueCount(100);
        ts.assertCompleted();
        ts.assertNoErrors();
        
        Assert.assertEquals(Arrays.asList(20L, 15L, 15L, 15L, 15L, 15L, 15L), requests);
    }
    
    @Test
    public void testLargerValueSubscriber() {
        final List<Long> requests = new ArrayList<Long>();
        
        Observable<Integer> source = Observable.range(0, 100)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testLargerValueSubscriber >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 5);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(30) {
            int remaining = 30;
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (--remaining == 0) {
                    remaining = 30;
                    request(30);
                }
            }
            @Override
            public void onError(Throwable e) {
                super.onError(e);
            }
            @Override
            public void onCompleted() {
                super.onCompleted();
            }
        };
        
        source.subscribe(ts);
        
        ts.assertValueCount(100);
        ts.assertCompleted();
        ts.assertNoErrors();
        
        Assert.assertEquals(Arrays.asList(20L, 10L, 20L, 10L, 20L, 10L, 20L), requests);
    }
    @Test
    public void testSmallerValueSubscriber() {
        final List<Long> requests = new ArrayList<Long>();
        
        Observable<Integer> source = Observable.range(0, 100)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testSmallerValueSubscriber >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 5);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(30) {
            int remaining = 10;
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (--remaining == 0) {
                    remaining = 10;
                    request(10);
                }
            }
            @Override
            public void onError(Throwable e) {
                super.onError(e);
            }
            @Override
            public void onCompleted() {
                super.onCompleted();
            }
        };
        
        source.subscribe(ts);
        
        ts.assertValueCount(100);
        ts.assertCompleted();
        ts.assertNoErrors();
        
        Assert.assertEquals(Arrays.asList(20L, 15L, 15L, 15L, 15L, 15L, 15L), requests);
    }
    
    @Test
    public void testBackpressureHonored() {
        final List<Long> requests = new ArrayList<Long>();
        Observable<Integer> source = Observable.range(0, 100)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testBackpressureHonored >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 5)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testBackpressureHonored 2 >> " + t);
                    }
                })
                ;
        
        TestSubscriber<Integer> ts = TestSubscriber.create(15);
        
        source.subscribe(ts);
        
        ts.assertValueCount(15);
        
        ts.requestMore(15);
        
        ts.assertValueCount(30);
        
        ts.requestMore(69);

        ts.assertValueCount(99);
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
    }
    @Test
    public void testBackpressureHonoredModulo() {
        final List<Long> requests = new ArrayList<Long>();
        Observable<Integer> source = Observable.range(0, 120)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testBackpressureHonored >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 5)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testBackpressureHonored 2 >> " + t);
                    }
                })
                ;
        
        TestSubscriber<Integer> ts = TestSubscriber.create(15);
        
        source.subscribe(ts);
        int tally = 15;
        for (int i = 0; i < 8; i++) {
            ts.assertValueCount(tally);
            tally += 15;
            ts.requestMore(15);
        }

        ts.assertValueCount(120);
        ts.assertCompleted();
        ts.assertNoErrors();
    }
    @Test
    public void testBackpressureHonoredAsync() {
        final List<Long> requests = new ArrayList<Long>();
        Observable<Integer> source = Observable.range(0, 100)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testBackpressureHonoredAsync >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 5)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testBackpressureHonoredAsync 2 >> " + t);
                    }
                })
                .observeOn(Schedulers.computation())
                ;
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        source.subscribe(ts);

        ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertValueCount(100);
    }
    
    @Test
    public void testZeroLevelValueSubscriber() {
        final List<Long> requests = new ArrayList<Long>();
        
        Observable<Integer> source = Observable.range(0, 100)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testZeroLevelValueSubscriber >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 0);
        
        TestSubscriber<Integer> ts = TestSubscriber.create(200);
        
        source.subscribe(ts);
        
        ts.assertValueCount(100);
        ts.assertCompleted();
        ts.assertNoErrors();
        
        Assert.assertEquals(Arrays.asList(20L, 20L, 20L, 20L, 20L, 20L), requests);
    }
    
    @Test
    public void testZeroLevelLargeRequestValueSubscriber() {
        final List<Long> requests = new ArrayList<Long>();
        
        Observable<Integer> source = Observable.range(0, 100)
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long t) {
                        System.out.println("testZeroLevelValueSubscriber >> " + t);
                        requests.add(t);
                    }
                })
                .requestBatching(20, 0);
        
        TestSubscriber<Integer> ts = TestSubscriber.create(50);
        
        source.subscribe(ts);

        ts.assertValueCount(50);
        
        ts.requestMore(50);
        
        ts.assertValueCount(100);
        ts.assertCompleted();
        ts.assertNoErrors();
        
        Assert.assertEquals(Arrays.asList(20L, 20L, 10L, 20L, 20L, 10L), requests);
    }
}
