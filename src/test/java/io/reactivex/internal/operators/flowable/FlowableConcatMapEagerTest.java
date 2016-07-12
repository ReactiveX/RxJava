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

import org.junit.Test;
import org.reactivestreams.Publisher;
import static org.junit.Assert.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableConcatMapEagerTest {

    @Test
    public void normal() {
        Flowable.range(1, 5)
        .concatMapEager(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        })
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalBackpressured() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .concatMapEager(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        })
        .test(3);
        
        ts.assertValues(1, 2, 2);
        
        ts.request(1);
        
        ts.assertValues(1, 2, 2, 3);
        
        ts.request(1);

        ts.assertValues(1, 2, 2, 3, 3);

        ts.request(5);

        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalDelayBoundary() {
        Flowable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        }, false)
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalDelayBoundaryBackpressured() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        }, false)
        .test(3);
        
        ts.assertValues(1, 2, 2);
        
        ts.request(1);
        
        ts.assertValues(1, 2, 2, 3);
        
        ts.request(1);

        ts.assertValues(1, 2, 2, 3, 3);

        ts.request(5);

        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalDelayEnd() {
        Flowable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        }, true)
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalDelayEndBackpressured() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        }, true)
        .test(3);
        
        ts.assertValues(1, 2, 2);
        
        ts.request(1);
        
        ts.assertValues(1, 2, 2, 3);
        
        ts.request(1);

        ts.assertValues(1, 2, 2, 3, 3);

        ts.request(5);

        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void mainErrorsDelayBoundary() {
        PublishProcessor<Integer> main = PublishProcessor.create();
        final PublishProcessor<Integer> inner = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = main.concatMapEagerDelayError(
                new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer t) {
                        return inner;
                    }
                }, false).test();
        
        main.onNext(1);
        
        inner.onNext(2);
        
        ts.assertValue(2);
        
        main.onError(new TestException("Forced failure"));
        
        ts.assertNoErrors();
        
        inner.onNext(3);
        inner.onComplete();
        
        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3);
    }

    @Test
    public void mainErrorsDelayEnd() {
        PublishProcessor<Integer> main = PublishProcessor.create();
        final PublishProcessor<Integer> inner = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = main.concatMapEagerDelayError(
                new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer t) {
                        return inner;
                    }
                }, true).test();
        
        main.onNext(1);
        main.onNext(2);
        
        inner.onNext(2);
        
        ts.assertValue(2);
        
        main.onError(new TestException("Forced failure"));
        
        ts.assertNoErrors();
        
        inner.onNext(3);
        inner.onComplete();
        
        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3, 2, 3);
    }
    
    @Test
    public void mainErrorsImmediate() {
        PublishProcessor<Integer> main = PublishProcessor.create();
        final PublishProcessor<Integer> inner = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = main.concatMapEager(
                new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer t) {
                        return inner;
                    }
                }).test();
        
        main.onNext(1);
        main.onNext(2);
        
        inner.onNext(2);
        
        ts.assertValue(2);
        
        main.onError(new TestException("Forced failure"));

        assertFalse("inner has subscribers?", inner.hasSubscribers());
        
        inner.onNext(3);
        inner.onComplete();
        
        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2);
    }
    
    @Test
    public void longEager() {
        
        Flowable.range(1, 2 * Flowable.bufferSize())
        .concatMapEager(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) {
                return Flowable.just(1);
            }
        })
        .test()
        .assertValueCount(2 * Flowable.bufferSize())
        .assertNoErrors()
        .assertComplete();
    }
}
