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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;


public class FlowablePublishFunctionTest {
    @Test
    public void concatTakeFirstLastCompletes() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.range(1, 3).publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return Flowable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);
        
        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void concatTakeFirstLastBackpressureCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0L);
        
        Flowable.range(1, 6).publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return Flowable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.request(1); // make sure take() doesn't go unbounded
        ts.request(4);
        
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.request(5);

        ts.assertValues(1, 2, 3, 4, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void canBeCancelled() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return Flowable.concat(o.take(5), o.takeLast(5));
            }
        }).subscribe(ts);
        
        ps.onNext(1);
        ps.onNext(2);
        
        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.cancel();
        
        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }
    
    @Test
    public void invalidPrefetch() {
        try {
            Flowable.<Integer>never().publish(
                    Functions.<Flowable<Integer>>identity(), -99);
            fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }
    }
    
    @Test
    public void takeCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o.take(1);
            }
        }).subscribe(ts);
        
        ps.onNext(1);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
        
        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
        
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

        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o.take(1);
            }
        }).subscribe(ts);
        
        Assert.assertEquals(1, startCount.get());
    }
    
    @Test
    public void takeCompletesUnsafe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o.take(1);
            }
        }).subscribe(ts);
        
        ps.onNext(1);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
        
        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }

    @Test
    public void directCompletesUnsafe() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o;
            }
        }).subscribe(ts);
        
        ps.onNext(1);
        ps.onComplete();
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertComplete();
        
        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }
    
    @Test
    public void overflowMissingBackpressureException() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps.publish(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o;
            }
        }).subscribe(ts);
        
        for (int i = 0; i < Flowable.bufferSize() * 2; i++) {
            ps.onNext(i);
        }
        
        ts.assertNoValues();
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();
        
        Assert.assertEquals("Could not emit value due to lack of requests", 
                ts.errors().get(0).getMessage());
        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }
    
    @Test
    public void overflowMissingBackpressureExceptionDelayed() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        new FlowablePublishMulticast<Integer, Integer>(ps, new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> o) {
                return o;
            }
        }, Flowable.bufferSize(), true).subscribe(ts);
        
        for (int i = 0; i < Flowable.bufferSize() * 2; i++) {
            ps.onNext(i);
        }
        
        ts.request(Flowable.bufferSize());
        
        ts.assertValueCount(Flowable.bufferSize());
        ts.assertError(MissingBackpressureException.class);
        ts.assertNotComplete();
        
        Assert.assertEquals("Could not emit value due to lack of requests", ts.errors().get(0).getMessage());
        Assert.assertFalse("Source has subscribers?", ps.hasSubscribers());
    }
    
    @Test
    public void emptyIdentityMapped() {
        Flowable.empty()
        .publish(Functions.<Flowable<Object>>identity())
        .test()
        .assertResult()
        ;
    }
    
    @Test
    public void independentlyMapped() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = pp.publish(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) throws Exception {
                return Flowable.range(1, 5);
            }
        }).test(0);

        assertTrue("pp has no Subscribers?!", pp.hasSubscribers());

        ts.assertNoValues()
        .assertNoErrors()
        .assertNotComplete();

        ts.request(5);
        
        ts.assertResult(1, 2, 3, 4, 5);
        
        assertFalse("pp has Subscribers?!", pp.hasSubscribers());
    }
}
