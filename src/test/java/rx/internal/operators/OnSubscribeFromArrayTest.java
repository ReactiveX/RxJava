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

import org.junit.Test;

import rx.Observable;
import rx.observers.TestSubscriber;

public class OnSubscribeFromArrayTest {
    
    Observable<Integer> create(int n) {
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        return Observable.create(new OnSubscribeFromArray<Integer>(array));
    }
    @Test
    public void simple() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        create(1000).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValueCount(1000);
        ts.assertCompleted();
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        create(1000).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotCompleted();
        
        ts.requestMore(10);
        
        ts.assertNoErrors();
        ts.assertValueCount(10);
        ts.assertNotCompleted();
        
        ts.requestMore(1000);
        
        ts.assertNoErrors();
        ts.assertValueCount(1000);
        ts.assertCompleted();
    }

}
