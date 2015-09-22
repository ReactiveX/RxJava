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

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;
import io.reactivex.subjects.nbp.NbpPublishSubject;

public class NbpOperatorAsObservableTest {
    @Test
    public void testHiding() {
        NbpPublishSubject<Integer> src = NbpPublishSubject.create();
        
        NbpObservable<Integer> dst = src.asObservable();
        
        assertFalse(dst instanceof NbpPublishSubject);
        
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        
        dst.subscribe(o);
        
        src.onNext(1);
        src.onComplete();
        
        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testHidingError() {
        NbpPublishSubject<Integer> src = NbpPublishSubject.create();
        
        NbpObservable<Integer> dst = src.asObservable();
        
        assertFalse(dst instanceof NbpPublishSubject);
        
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        
        dst.subscribe(o);
        
        src.onError(new TestException());
        
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
}