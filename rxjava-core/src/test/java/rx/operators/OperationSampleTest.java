/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

public class OperationSampleTest {
    private TestScheduler scheduler;
    private Observer<Long> observer;

    @Before
    @SuppressWarnings("unchecked")
    // due to mocking
    public void before() {
        scheduler = new TestScheduler();
        observer = mock(Observer.class);
    }

    @Test
    public void testSample() {
        Observable<Long> source = Observable.create(new Observable.OnSubscribeFunc<Long>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Long> observer1) {
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        observer1.onNext(1L);
                    }
                }, 1, TimeUnit.SECONDS);
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        observer1.onNext(2L);
                    }
                }, 2, TimeUnit.SECONDS);
                scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        observer1.onCompleted();
                    }
                }, 3, TimeUnit.SECONDS);

                return Subscriptions.empty();
            }
        });

        Observable<Long> sampled = Observable.create(OperationSample.sample(source, 400L, TimeUnit.MILLISECONDS, scheduler));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(800L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(any(Long.class));
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1200L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1600L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        verify(observer, never()).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(1L);
        inOrder.verify(observer, times(2)).onNext(2L);
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }
}
