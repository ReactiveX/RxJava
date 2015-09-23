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

package io.reactivex.internal.operators.nbp;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;

public class NbpOperatorDefaultIfEmptyTest {

    @Test
    public void testDefaultIfEmpty() {
        NbpObservable<Integer> source = NbpObservable.just(1, 2, 3);
        NbpObservable<Integer> NbpObservable = source.defaultIfEmpty(10);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(10);
        verify(NbpObserver).onNext(1);
        verify(NbpObserver).onNext(2);
        verify(NbpObserver).onNext(3);
        verify(NbpObserver).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDefaultIfEmptyWithEmpty() {
        NbpObservable<Integer> source = NbpObservable.empty();
        NbpObservable<Integer> NbpObservable = source.defaultIfEmpty(10);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver).onNext(10);
        verify(NbpObserver).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }
    
    @Test
    @Ignore("Subscribers should not throw")
    public void testEmptyButClientThrows() {
        NbpSubscriber<Integer> o = TestHelper.mockNbpSubscriber();
        
        NbpObservable.<Integer>empty().defaultIfEmpty(1).subscribe(new NbpObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onComplete() {
                o.onComplete();
            }
        });
        
        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onComplete();
    }
}