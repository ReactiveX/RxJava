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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Scheduler.Inner;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.TestScheduler;
import rx.subscriptions.Subscriptions;

public class OperationThrottleFirstTest {

    private TestScheduler scheduler;
    private Observer<String> observer;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        scheduler = new TestScheduler();
        observer = mock(Observer.class);
    }

    @Test
    public void testThrottlingWithCompleted() {
        Observable<String> source = Observable.create(new Observable.OnSubscribeFunc<String>() {
            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {
                publishNext(observer, 100, "one");    // publish as it's first
                publishNext(observer, 300, "two");    // skip as it's last within the first 400
                publishNext(observer, 900, "three");   // publish
                publishNext(observer, 905, "four");   // skip
                publishCompleted(observer, 1000);     // Should be published as soon as the timeout expires.

                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationThrottleFirst.throttleFirst(source, 400, TimeUnit.MILLISECONDS, scheduler));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(0)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(0)).onNext("four");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testThrottlingWithError() {
        Observable<String> source = Observable.create(new Observable.OnSubscribeFunc<String>() {
            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {
                Exception error = new TestException();
                publishNext(observer, 100, "one");    // Should be published since it is first
                publishNext(observer, 200, "two");    // Should be skipped since onError will arrive before the timeout expires
                publishError(observer, 300, error);   // Should be published as soon as the timeout expires.

                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationThrottleFirst.throttleFirst(source, 400, TimeUnit.MILLISECONDS, scheduler));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(400, TimeUnit.MILLISECONDS);
        inOrder.verify(observer).onNext("one");
        inOrder.verify(observer).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    private <T> void publishCompleted(final Observer<T> observer, long delay) {
        scheduler.schedule(new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                observer.onCompleted();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Observer<T> observer, long delay, final Exception error) {
        scheduler.schedule(new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                observer.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Observer<T> observer, long delay, final T value) {
        scheduler.schedule(new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("serial")
    private class TestException extends Exception {
    }
}
