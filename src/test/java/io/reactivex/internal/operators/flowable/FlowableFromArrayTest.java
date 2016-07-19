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

import org.junit.*;

import io.reactivex.Flowable;
import io.reactivex.internal.fuseable.ScalarCallable;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableFromArrayTest {
    
    Flowable<Integer> create(int n) {
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        return Flowable.fromArray(array);
    }
    @Test
    public void simple() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        create(1000).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValueCount(1000);
        ts.assertComplete();
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        create(1000).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotComplete();
        
        ts.request(10);
        
        ts.assertNoErrors();
        ts.assertValueCount(10);
        ts.assertNotComplete();
        
        ts.request(1000);
        
        ts.assertNoErrors();
        ts.assertValueCount(1000);
        ts.assertComplete();
    }

    @Test
    public void empty() {
        Assert.assertSame(Flowable.empty(), Flowable.fromArray(new Object[0]));
    }

    @Test
    public void just() {
        Flowable<Integer> source = Flowable.fromArray(new Integer[] { 1 });
        Assert.assertTrue(source.getClass().toString(), source instanceof ScalarCallable);
    }

}
