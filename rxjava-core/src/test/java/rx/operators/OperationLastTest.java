/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import static org.junit.Assert.*;

import org.junit.Test;

import rx.Observable;

public class OperationLastTest {

    @Test
    public void testLastWithElements() {
        Observable<Integer> last = Observable.create(OperationLast.last(Observable.from(1, 2, 3)));
        assertEquals(3, last.toBlockingObservable().single().intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLastWithNoElements() {
        Observable<?> last = Observable.create(OperationLast.last(Observable.empty()));
        last.toBlockingObservable().single();
    }
    
    @Test
    public void testLastMultiSubscribe() {
        Observable<Integer> last = Observable.create(OperationLast.last(Observable.from(1, 2, 3)));
        assertEquals(3, last.toBlockingObservable().single().intValue());
        assertEquals(3, last.toBlockingObservable().single().intValue());
    }

    @Test
    public void testLastViaObservable() {
        Observable.from(1, 2, 3).last();
    }
}
