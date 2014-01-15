/**
 * Copyright 2013 Netflix, Inc.
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
import static rx.operators.OperationAmb.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.IObservable;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.TestScheduler;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Action0;

public class OperationAmbTest {

    private TestScheduler scheduler;

    @Before
    public void setUp() {
        scheduler = new TestScheduler();
    }

    private IObservable<String> createObservable(final String[] values,
            final long interval, final Throwable e) {
        return new IObservable<String>() {

            @Override
            public Subscription subscribe(
                    final Observer<? super String> observer) {
                CompositeSubscription parentSubscription = new CompositeSubscription();
                long delay = interval;
                for (final String value : values) {
                    parentSubscription.add(scheduler.schedule(
                            new Action0() {
                                @Override
                                public void call() {
                                    observer.onNext(value);
                                }
                            }, delay, TimeUnit.MILLISECONDS));
                    delay += interval;
                }
                parentSubscription.add(scheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        if (e == null) {
                            observer.onCompleted();
                        } else {
                            observer.onError(e);
                        }
                    }
                }, delay, TimeUnit.MILLISECONDS));
                return parentSubscription;
            }
        };
    }

    @Test
    public void testAmb() {
        IObservable<String> observable1 = createObservable(new String[] {
                "1", "11", "111", "1111" }, 2000, null);
        IObservable<String> observable2 = createObservable(new String[] {
                "2", "22", "222", "2222" }, 1000, null);
        IObservable<String> observable3 = createObservable(new String[] {
                "3", "33", "333", "3333" }, 3000, null);

        IObservable<String> o = amb(observable1, observable2, observable3);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("2");
        inOrder.verify(observer, times(1)).onNext("22");
        inOrder.verify(observer, times(1)).onNext("222");
        inOrder.verify(observer, times(1)).onNext("2222");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAmb2() {
        IOException needHappenedException = new IOException(
                "fake exception");
        IObservable<String> observable1 = createObservable(new String[] {},
                2000, new IOException("fake exception"));
        IObservable<String> observable2 = createObservable(new String[] {
                "2", "22", "222", "2222" }, 1000, needHappenedException);
        IObservable<String> observable3 = createObservable(new String[] {},
                3000, new IOException("fake exception"));

        IObservable<String> o = amb(observable1, observable2, observable3);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("2");
        inOrder.verify(observer, times(1)).onNext("22");
        inOrder.verify(observer, times(1)).onNext("222");
        inOrder.verify(observer, times(1)).onNext("2222");
        inOrder.verify(observer, times(1)).onError(needHappenedException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAmb3() {
        IObservable<String> observable1 = createObservable(new String[] {
                "1" }, 2000, null);
        IObservable<String> observable2 = createObservable(new String[] {},
                1000, null);
        IObservable<String> observable3 = createObservable(new String[] {
                "3" }, 3000, null);

        IObservable<String> o = amb(observable1, observable2, observable3);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

}
