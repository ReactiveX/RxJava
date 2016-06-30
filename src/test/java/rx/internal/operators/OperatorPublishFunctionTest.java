/**
 * Copyright 2016 Netflix, Inc.
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

import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import rx.Observable;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Func1;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorPublishFunctionTest {
    @Test
    public void concatTakeFirstLastCompletes() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.range(1, 3).publish(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return Observable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);
        
        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatTakeFirstLastBackpressureCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        
        Observable.range(1, 6).publish(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return Observable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(5);
        
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(5);

        ts.assertValues(1, 2, 3, 4, 5, 6);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void canBeCancelled() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.publish(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return Observable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);
        
        ps.onNext(1);
        ps.onNext(2);
        
        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.unsubscribe();
        
        Assert.assertFalse("Source has subscribers?", ps.hasObservers());
    }
    
    @Test
    public void invalidPrefetch() {
        try {
            new OnSubscribePublishMulticast<Integer>(-99, false);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }
    }
    
    @Test
    public void takeCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.publish(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return o.take(1);
            }
        }).subscribe(ts);
        
        ps.onNext(1);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        Assert.assertFalse("Source has subscribers?", ps.hasObservers());
        
    }
    
    @Test
    public void oneStartOnly() {
        
        final AtomicInteger startCount = new AtomicInteger();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                startCount.incrementAndGet();
            }
        };

        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.publish(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return o.take(1);
            }
        }).subscribe(ts);
        
        Assert.assertEquals(1, startCount.get());
    }
    
    @Test
    public void takeCompletesUnsafe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.publish(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return o.take(1);
            }
        }).unsafeSubscribe(ts);
        
        ps.onNext(1);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        Assert.assertFalse("Source has subscribers?", ps.hasObservers());
    }

    @Test
    public void directCompletesUnsafe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.publish(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return o;
            }
        }).unsafeSubscribe(ts);
        
        ps.onNext(1);
        ps.onCompleted();
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        Assert.assertFalse("Source has subscribers?", ps.hasObservers());
    }
    
    @Test
    public void overflowMissingBackpressureException() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps.publish(new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return o;
            }
        }).unsafeSubscribe(ts);
        
        for (int i = 0; i < RxRingBuffer.SIZE * 2; i++) {
            ps.onNext(i);
        }
        
        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotCompleted();
        
        Assert.assertEquals("Queue full?!", ts.getOnErrorEvents().get(0).getMessage());
        Assert.assertFalse("Source has subscribers?", ps.hasObservers());
    }
    
    @Test
    public void overflowMissingBackpressureExceptionDelayed() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishSubject<Integer> ps = PublishSubject.create();
        
        OperatorPublish.create(ps, new Func1<Observable<Integer>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<Integer> o) {
                return o;
            }
        }, true).unsafeSubscribe(ts);
        
        for (int i = 0; i < RxRingBuffer.SIZE * 2; i++) {
            ps.onNext(i);
        }
        
        ts.requestMore(RxRingBuffer.SIZE);
        
        ts.assertValueCount(RxRingBuffer.SIZE);
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotCompleted();
        
        Assert.assertEquals("Queue full?!", ts.getOnErrorEvents().get(0).getMessage());
        Assert.assertFalse("Source has subscribers?", ps.hasObservers());
    }
}
