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

package io.reactivex.subjects;

import org.junit.Test;

import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.observers.*;

public class UnicastSubjectTest {

    @Test
    public void fusionLive() {
        UnicastSubject<Integer> ap = UnicastSubject.create();
        
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);
        
        ap.subscribe(ts);
        
        ts
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC));
        
        ts.assertNoValues().assertNoErrors().assertNotComplete();
        
        ap.onNext(1);

        ts.assertValue(1).assertNoErrors().assertNotComplete();
        
        ap.onComplete();
        
        ts.assertResult(1);
    }
    
    @Test
    public void fusionOfflie() {
        UnicastSubject<Integer> ap = UnicastSubject.create();
        ap.onNext(1);
        ap.onComplete();
        
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ANY);
        
        ap.subscribe(ts);
        
        ts
        .assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult(1);
    }}
