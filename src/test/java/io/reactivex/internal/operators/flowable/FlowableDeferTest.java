/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.flowable;

import static org.mockito.Mockito.*;

import java.util.concurrent.Callable;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;

@SuppressWarnings("unchecked")
public class FlowableDeferTest {

    @Test
    public void testDefer() throws Throwable {

        Callable<Flowable<String>> factory = mock(Callable.class);

        Flowable<String> firstObservable = Flowable.just("one", "two");
        Flowable<String> secondObservable = Flowable.just("three", "four");
        when(factory.call()).thenReturn(firstObservable, secondObservable);

        Flowable<String> deferred = Flowable.defer(factory);

        verifyZeroInteractions(factory);

        Subscriber<String> firstSubscriber = TestHelper.mockSubscriber();
        deferred.subscribe(firstSubscriber);

        verify(factory, times(1)).call();
        verify(firstSubscriber, times(1)).onNext("one");
        verify(firstSubscriber, times(1)).onNext("two");
        verify(firstSubscriber, times(0)).onNext("three");
        verify(firstSubscriber, times(0)).onNext("four");
        verify(firstSubscriber, times(1)).onComplete();

        Subscriber<String> secondSubscriber = TestHelper.mockSubscriber();
        deferred.subscribe(secondSubscriber);

        verify(factory, times(2)).call();
        verify(secondSubscriber, times(0)).onNext("one");
        verify(secondSubscriber, times(0)).onNext("two");
        verify(secondSubscriber, times(1)).onNext("three");
        verify(secondSubscriber, times(1)).onNext("four");
        verify(secondSubscriber, times(1)).onComplete();

    }

    @Test
    public void testDeferFunctionThrows() throws Exception {
        Callable<Flowable<String>> factory = mock(Callable.class);

        when(factory.call()).thenThrow(new TestException());

        Flowable<String> result = Flowable.defer(factory);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        result.subscribe(subscriber);

        verify(subscriber).onError(any(TestException.class));
        verify(subscriber, never()).onNext(any(String.class));
        verify(subscriber, never()).onComplete();
    }
}
