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

import static org.mockito.Mockito.*;

import java.util.concurrent.Future;

import org.junit.Test;

import rx.Observer;
import rx.Subscription;
import rx.observers.TestObserver;
import rx.operators.OperationToObservableFuture.ToObservableFuture;

public class OperationToObservableFutureTest {

    @Test
    public void testSuccess() throws Exception {
        Future<Object> future = mock(Future.class);
        Object value = new Object();
        when(future.get()).thenReturn(value);
        ToObservableFuture<Object> ob = new ToObservableFuture<Object>(future);
        Observer<Object> o = mock(Observer.class);

        Subscription sub = ob.onSubscribe(new TestObserver<Object>(o));
        sub.unsubscribe();

        verify(o, times(1)).onNext(value);
        verify(o, times(1)).onCompleted();
        verify(o, never()).onError(null);
        verify(future, never()).cancel(true);
    }

    @Test
    public void testFailure() throws Exception {
        Future<Object> future = mock(Future.class);
        RuntimeException e = new RuntimeException();
        when(future.get()).thenThrow(e);
        ToObservableFuture<Object> ob = new ToObservableFuture<Object>(future);
        Observer<Object> o = mock(Observer.class);

        Subscription sub = ob.onSubscribe(new TestObserver<Object>(o));
        sub.unsubscribe();

        verify(o, never()).onNext(null);
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(e);
        verify(future, never()).cancel(true);
    }
}
