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

package io.reactivex.internal.operators.observable;

import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.flowable.TestHelper;

public class NbpOperatorCastTest {

    @Test
    public void testCast() {
        Observable<?> source = Observable.just(1, 2);
        Observable<Integer> NbpObservable = source.cast(Integer.class);

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        
        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testCastWithWrongType() {
        Observable<?> source = Observable.just(1, 2);
        Observable<Boolean> NbpObservable = source.cast(Boolean.class);

        Observer<Boolean> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onError(
                org.mockito.Matchers.any(ClassCastException.class));
    }
}