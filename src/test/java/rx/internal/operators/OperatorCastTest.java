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

import static org.mockito.Mockito.*;

import org.junit.*;

import rx.*;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorCastTest {

    @Test
    public void testCast() {
        Observable<?> source = Observable.just(1, 2);
        Observable<Integer> observable = source.cast(Integer.class);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(1);
        verify(observer, never()).onError(
                org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testCastWithWrongType() {
        Observable<?> source = Observable.just(1, 2);
        Observable<Boolean> observable = source.cast(Boolean.class);

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onError(
                org.mockito.Matchers.any(ClassCastException.class));
    }
    
    @Test
    public void castCrashUnsubscribes() {
        
        PublishSubject<Integer> ps = PublishSubject.create();
        
        TestSubscriber<String> ts = TestSubscriber.create();
        
        ps.cast(String.class).unsafeSubscribe(ts);
        
        Assert.assertTrue("Not subscribed?", ps.hasObservers());
        
        ps.onNext(1);
        
        Assert.assertFalse("Subscribed?", ps.hasObservers());
        
        ts.assertError(ClassCastException.class);
    }
}
