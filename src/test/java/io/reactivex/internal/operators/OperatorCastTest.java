/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;

public class OperatorCastTest {

    @Test
    public void testCast() {
        Observable<?> source = Observable.just(1, 2);
        Observable<Integer> observable = source.cast(Integer.class);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(1);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testCastWithWrongType() {
        Observable<?> source = Observable.just(1, 2);
        Observable<Boolean> observable = source.cast(Boolean.class);

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onError(
                org.mockito.Matchers.any(ClassCastException.class));
    }
}