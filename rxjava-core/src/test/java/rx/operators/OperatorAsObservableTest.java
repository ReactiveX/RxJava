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

package rx.operators;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.subjects.PublishSubject;

public class OperatorAsObservableTest {
    @Test
    public void testHiding() {
        PublishSubject<Integer> src = PublishSubject.create();
        
        Observable<Integer> dst = src.asObservable();
        
        assertFalse(dst instanceof PublishSubject);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        dst.subscribe(o);
        
        src.onNext(1);
        src.onCompleted();
        
        verify(o).onNext(1);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testHidingError() {
        PublishSubject<Integer> src = PublishSubject.create();
        
        Observable<Integer> dst = src.asObservable();
        
        assertFalse(dst instanceof PublishSubject);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        dst.subscribe(o);
        
        src.onError(new TestException());
        
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }
}
