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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observers.TestObserver;

public class OperatorToObservableFutureTest {

    @Test
    public void testSuccess() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        Object value = new Object();
        when(future.get()).thenReturn(value);
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Subscription sub = Observable.from(future).subscribe(new TestObserver<Object>(o));
        sub.unsubscribe();

        verify(o, times(1)).onNext(value);
        verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
        verify(future, times(1)).cancel(true);
    }

    @Test
    public void testFailure() throws Exception {
        @SuppressWarnings("unchecked")
        Future<Object> future = mock(Future.class);
        RuntimeException e = new RuntimeException();
        when(future.get()).thenThrow(e);
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Subscription sub = Observable.from(future).subscribe(new TestObserver<Object>(o));
        sub.unsubscribe();

        verify(o, never()).onNext(null);
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(e);
        verify(future, times(1)).cancel(true);
    }
}
