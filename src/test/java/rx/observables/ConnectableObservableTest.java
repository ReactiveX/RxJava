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
package rx.observables;

import java.util.concurrent.atomic.*;

import org.junit.*;

import rx.*;
import rx.functions.*;
import rx.observers.TestSubscriber;

public class ConnectableObservableTest {
    @Test
    public void testAutoConnect() {
        final AtomicInteger run = new AtomicInteger();
        
        ConnectableObservable<Integer> co = Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(run.incrementAndGet());
            }
        }).publish();
        
        Observable<Integer> source = co.autoConnect();
        
        Assert.assertEquals(0, run.get());
        
        TestSubscriber<Integer> ts1 = TestSubscriber.create();
        source.subscribe(ts1);
        
        ts1.assertCompleted();
        ts1.assertNoErrors();
        ts1.assertValue(1);
        
        Assert.assertEquals(1, run.get());

        TestSubscriber<Integer> ts2 = TestSubscriber.create();
        source.subscribe(ts2);

        ts2.assertNotCompleted();
        ts2.assertNoErrors();
        ts2.assertNoValues();
        
        Assert.assertEquals(1, run.get());
    }
    @Test
    public void testAutoConnect0() {
        final AtomicInteger run = new AtomicInteger();
        
        ConnectableObservable<Integer> co = Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(run.incrementAndGet());
            }
        }).publish();
        
        Observable<Integer> source = co.autoConnect(0);
        
        Assert.assertEquals(1, run.get());
        
        TestSubscriber<Integer> ts1 = TestSubscriber.create();
        source.subscribe(ts1);
        
        ts1.assertNotCompleted();
        ts1.assertNoErrors();
        ts1.assertNoValues();
        
        Assert.assertEquals(1, run.get());

        TestSubscriber<Integer> ts2 = TestSubscriber.create();
        source.subscribe(ts2);

        ts2.assertNotCompleted();
        ts2.assertNoErrors();
        ts2.assertNoValues();
        
        Assert.assertEquals(1, run.get());
    }
    @Test
    public void testAutoConnect2() {
        final AtomicInteger run = new AtomicInteger();
        
        ConnectableObservable<Integer> co = Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(run.incrementAndGet());
            }
        }).publish();
        
        Observable<Integer> source = co.autoConnect(2);
        
        Assert.assertEquals(0, run.get());
        
        TestSubscriber<Integer> ts1 = TestSubscriber.create();
        source.subscribe(ts1);
        
        ts1.assertNotCompleted();
        ts1.assertNoErrors();
        ts1.assertNoValues();
        
        Assert.assertEquals(0, run.get());

        TestSubscriber<Integer> ts2 = TestSubscriber.create();
        source.subscribe(ts2);

        Assert.assertEquals(1, run.get());

        ts1.assertCompleted();
        ts1.assertNoErrors();
        ts1.assertValue(1);

        ts2.assertCompleted();
        ts2.assertNoErrors();
        ts2.assertValue(1);
        
    }
    
    @Test
    public void testAutoConnectUnsubscribe() {
        final AtomicInteger run = new AtomicInteger();
        
        ConnectableObservable<Integer> co = Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.range(run.incrementAndGet(), 10);
            }
        }).publish();
        
        final AtomicReference<Subscription> conn = new AtomicReference<Subscription>();
        
        Observable<Integer> source = co.autoConnect(1, new Action1<Subscription>() {
            @Override
            public void call(Subscription t) {
                conn.set(t);
            }
        });
        
        Assert.assertEquals(0, run.get());
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                Subscription s = conn.get();
                if (s != null) {
                    s.unsubscribe();
                } else {
                    onError(new NullPointerException("No connection reference"));
                }
            }
        };
        
        source.subscribe(ts);
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertValue(1);
        
        Assert.assertTrue("Connection not unsubscribed?", conn.get().isUnsubscribed());
    }
}
