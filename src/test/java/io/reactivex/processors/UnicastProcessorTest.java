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

package io.reactivex.processors;

import org.junit.Test;

import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.subscribers.*;

public class UnicastProcessorTest {

    @Test
    public void fusionLive() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create();
        
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);
        
        ap.subscribe(ts);
        
        ts
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC));
        
        ts.assertNoValues().assertNoErrors().assertNotComplete();
        
        ap.onNext(1);

        ts.assertValue(1).assertNoErrors().assertNotComplete();
        
        ap.onComplete();
        
        ts.assertResult(1);
    }
    
    @Test
    public void fusionOfflie() {
        UnicastProcessor<Integer> ap = UnicastProcessor.create();
        ap.onNext(1);
        ap.onComplete();
        
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);
        
        ap.subscribe(ts);
        
        ts
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
        .assertResult(1);
    }}
