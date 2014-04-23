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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static rx.operators.OperatorAmb.amb;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.TestScheduler;
import rx.subscriptions.CompositeSubscription;

public class OperatorAmbTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void setUp() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    private Observable<String> createObservable(final String[] values,
            final long interval, final Throwable e) {
        return Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(final Subscriber<? super String> subscriber) {
                CompositeSubscription parentSubscription = new CompositeSubscription();
                subscriber.add(parentSubscription);
                long delay = interval;
                for (final String value : values) {
                    parentSubscription.add(innerScheduler.schedule(new Action0() {
                        @Override
                        public void call() {
                            subscriber.onNext(value);
                        }
                    }, delay, TimeUnit.MILLISECONDS));
                    delay += interval;
                }
                parentSubscription.add(innerScheduler.schedule(new Action0() {
                    @Override
                    public void call() {
                        if (e == null) {
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(e);
                        }
                    }
                }, delay, TimeUnit.MILLISECONDS));
            }
        });
    }

    @Test
    public void testAmb() {
        Observable<String> observable1 = createObservable(new String[] {
                "1", "11", "111", "1111" }, 2000, null);
        Observable<String> observable2 = createObservable(new String[] {
                "2", "22", "222", "2222" }, 1000, null);
        Observable<String> observable3 = createObservable(new String[] {
                "3", "33", "333", "3333" }, 3000, null);

        Observable<String> o = Observable.create(amb(observable1,
                observable2, observable3));

        @SuppressWarnings("unchecked")
        Observer<String> observer = (Observer<String>) mock(Observer.class);
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
        IOException expectedException = new IOException(
                "fake exception");
        Observable<String> observable1 = createObservable(new String[] {},
                2000, new IOException("fake exception"));
        Observable<String> observable2 = createObservable(new String[] {
                "2", "22", "222", "2222" }, 1000, expectedException);
        Observable<String> observable3 = createObservable(new String[] {},
                3000, new IOException("fake exception"));

        Observable<String> o = Observable.create(amb(observable1,
                observable2, observable3));

        @SuppressWarnings("unchecked")
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("2");
        inOrder.verify(observer, times(1)).onNext("22");
        inOrder.verify(observer, times(1)).onNext("222");
        inOrder.verify(observer, times(1)).onNext("2222");
        inOrder.verify(observer, times(1)).onError(expectedException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAmb3() {
        Observable<String> observable1 = createObservable(new String[] {
                "1" }, 2000, null);
        Observable<String> observable2 = createObservable(new String[] {},
                1000, null);
        Observable<String> observable3 = createObservable(new String[] {
                "3" }, 3000, null);

        Observable<String> o = Observable.create(amb(observable1,
                observable2, observable3));

        @SuppressWarnings("unchecked")
        Observer<String> observer = (Observer<String>) mock(Observer.class);
        o.subscribe(observer);

        scheduler.advanceTimeBy(100000, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

}
