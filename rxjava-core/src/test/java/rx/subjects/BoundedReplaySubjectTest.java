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
package rx.subjects;

import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;
import rx.Observer;
import rx.Subscription;

public class BoundedReplaySubjectTest {
    @Test
    public void testAsPublishSubject() {
        BoundedReplaySubject<Object> brs = BoundedReplaySubject.create(0);
        
        @SuppressWarnings("unchecked")
        Observer<Object> o1 = mock(Observer.class);
        
        Subscription s = brs.subscribe(o1);
        
        brs.onNext(1);

        s.unsubscribe();
        
        brs.onCompleted();
        
        InOrder inOrder1 = inOrder(o1);
        verify(o1, never()).onError(any(Throwable.class));
        inOrder1.verify(o1).onNext(1);
        inOrder1.verifyNoMoreInteractions();

        // subscribe after completion
        
        @SuppressWarnings("unchecked")
        Observer<Object> o2 = mock(Observer.class);
        
        brs.subscribe(o2);

        verify(o2, never()).onError(any(Throwable.class));
        verify(o2, never()).onNext(any());
        verify(o2).onCompleted();
    }
    
    @Test
    public void test1Buffered() {
        @SuppressWarnings("unchecked")
        Observer<Object> o1 = mock(Observer.class);

        @SuppressWarnings("unchecked")
        Observer<Object> o2 = mock(Observer.class);

        @SuppressWarnings("unchecked")
        Observer<Object> o3 = mock(Observer.class);

        InOrder inOrder1 = inOrder(o1);
        InOrder inOrder2 = inOrder(o2);
        InOrder inOrder3 = inOrder(o3);

        // --------------

        BoundedReplaySubject<Object> brs = BoundedReplaySubject.create(1);
        brs.onNext(0);
        
        Subscription s = brs.subscribe(o1);
        
        brs.onNext(1);

        s.unsubscribe();

        
        brs.subscribe(o2);
        
        brs.onNext(2);
        
        brs.onCompleted();

        
        brs.subscribe(o3);
        
        // --------------------------------
        
        verify(o1, never()).onError(any(Throwable.class));
        inOrder1.verify(o1).onNext(0);
        inOrder1.verify(o1).onNext(1);
        inOrder1.verifyNoMoreInteractions();
        
        verify(o2, never()).onError(any(Throwable.class));
        inOrder2.verify(o2).onNext(1);
        inOrder2.verify(o2).onNext(2);
        inOrder2.verify(o2).onCompleted();
        inOrder2.verifyNoMoreInteractions();

        // subscribe after completion
        
        verify(o3, never()).onError(any(Throwable.class));
        inOrder3.verify(o3).onNext(2);
        inOrder3.verify(o3).onCompleted();
        inOrder3.verifyNoMoreInteractions();
    }
}
