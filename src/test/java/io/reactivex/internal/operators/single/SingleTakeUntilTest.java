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

package io.reactivex.internal.operators.single;

import java.util.concurrent.CancellationException;

import org.junit.Test;

import io.reactivex.exceptions.TestException;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class SingleTakeUntilTest {

    @Test
    public void mainSuccessPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp)
        .test();
        
        source.onNext(1);
        source.onComplete();
        
        ts.assertResult(1);
    }

    @Test
    public void mainSuccessSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toSingle())
        .test();
        
        source.onNext(1);
        source.onComplete();
        
        ts.assertResult(1);
    }


    @Test
    public void mainSuccessCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toCompletable())
        .test();
        
        source.onNext(1);
        source.onComplete();
        
        ts.assertResult(1);
    }
    
    @Test
    public void mainErrorPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp)
        .test();
        
        source.onError(new TestException());
        
        ts.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toSingle())
        .test();
        
        source.onError(new TestException());
        
        ts.assertFailure(TestException.class);
    }

    @Test
    public void mainErrorCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toCompletable())
        .test();
        
        source.onError(new TestException());
        
        ts.assertFailure(TestException.class);
    }

    @Test
    public void otherOnNextPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp)
        .test();
        
        pp.onNext(1);
        
        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnNextSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toSingle())
        .test();
        
        pp.onNext(1);
        pp.onComplete();
        
        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnNextCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toCompletable())
        .test();
        
        pp.onNext(1);
        pp.onComplete();
        
        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnCompletePublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp)
        .test();
        
        pp.onComplete();
        
        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherOnCompleteCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toCompletable())
        .test();
        
        pp.onComplete();
        
        ts.assertFailure(CancellationException.class);
    }

    @Test
    public void otherErrorPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp)
        .test();
        
        pp.onError(new TestException());
        
        ts.assertFailure(TestException.class);
    }

    @Test
    public void otherErrorSingle() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toSingle())
        .test();
        
        pp.onError(new TestException());
        
        ts.assertFailure(TestException.class);
    }

    @Test
    public void otherErrorCompletable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = source.toSingle().takeUntil(pp.toCompletable())
        .test();
        
        pp.onError(new TestException());
        
        ts.assertFailure(TestException.class);
    }

}
