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

public class OperatorMergeWithTest {
    @Test
    public void mergeLargeAmountOfSources() {
        Observable<Integer> source = Observable.range(1, 2);
        
        Observable<Integer> result = source;
        int n = 5000;
        
        for (int i = 0; i < n; i++) {
            result = result.mergeWith(source);
        }
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        long t = System.nanoTime();
        
        result.subscribe(ts);
        
        ts.assertValueCount((n + 1) * 2);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        t = System.nanoTime() - t;
        
        System.out.printf("Merging took: %,d ns%n", t);
    }
}
