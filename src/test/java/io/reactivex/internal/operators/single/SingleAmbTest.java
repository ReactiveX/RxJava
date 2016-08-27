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

import static org.junit.Assert.*;

import java.util.NoSuchElementException;

import org.junit.Test;

import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class SingleAmbTest {
    @Test
    public void ambWithFirstFires() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = pp1.toSingle().ambWith(pp2.toSingle()).test();
        
        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());
        
        pp1.onNext(1);
        pp1.onComplete();
        
        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());
        
        ts.assertResult(1);
    
    }

    @Test
    public void ambWithSecondFires() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = pp1.toSingle().ambWith(pp2.toSingle()).test();
        
        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());
        
        pp2.onNext(2);
        pp2.onComplete();
        
        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());
        
        ts.assertResult(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayEmpty() {
        Single.ambArray()
        .test()
        .assertFailure(NoSuchElementException.class);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void ambSingleSource() {
        assertSame(Single.never(), Single.ambArray(Single.never()));
    }
}
