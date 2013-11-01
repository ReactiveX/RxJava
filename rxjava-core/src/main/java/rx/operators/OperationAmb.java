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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.TestScheduler;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Action0;

/**
 * Propagates the observable sequence that reacts first.
 */
public class OperationAmb {

    public static <T> OnSubscribeFunc<T> amb(Observable<? extends T>... sources) {
        return amb(Arrays.asList(sources));
    }

    public static <T> OnSubscribeFunc<T> amb(
            final Iterable<? extends Observable<? extends T>> sources) {
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(final Observer<? super T> observer) {
                AtomicInteger choice = new AtomicInteger(AmbObserver.NONE);
                int index = 0;
                CompositeSubscription parentSubscription = new CompositeSubscription();
                for (Observable<? extends T> source : sources) {
                    SafeObservableSubscription subscription = new SafeObservableSubscription();
                    AmbObserver<T> ambObserver = new AmbObserver<T>(
                            subscription, observer, index, choice);
                    parentSubscription.add(subscription.wrap(source
                            .subscribe(ambObserver)));
                    index++;
                }
                return parentSubscription;
            }
        };
    }

    private static class AmbObserver<T> implements Observer<T> {

        private static final int NONE = -1;

        private Subscription subscription;
        private Observer<? super T> observer;
        private int index;
        private AtomicInteger choice;

        private AmbObserver(Subscription subscription,
                Observer<? super T> observer, int index, AtomicInteger choice) {
            this.subscription = subscription;
            this.observer = observer;
            this.choice = choice;
            this.index = index;
        }

        @Override
        public void onNext(T args) {
            if (!isSelected()) {
                subscription.unsubscribe();
                return;
            }
            observer.onNext(args);
        }

        @Override
        public void onCompleted() {
            if (!isSelected()) {
                subscription.unsubscribe();
                return;
            }
            observer.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            if (!isSelected()) {
                subscription.unsubscribe();
                return;
            }
            observer.onError(e);
        }

        private boolean isSelected() {
            if (choice.get() == NONE) {
                return choice.compareAndSet(NONE, index);
            }
            return choice.get() == index;
        }
    }

    public static class UnitTest {
        private TestScheduler scheduler;

        @Before
        public void setUp() {
            scheduler = new TestScheduler();
        }

        private Observable<String> createObservable(final String[] values,
                final long interval, final Throwable e) {
            return Observable.create(new OnSubscribeFunc<String>() {

                @Override
                public Subscription onSubscribe(
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

            @SuppressWarnings("unchecked")
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
            IOException needHappenedException = new IOException(
                    "fake exception");
            Observable<String> observable1 = createObservable(new String[] {},
                    2000, new IOException("fake exception"));
            Observable<String> observable2 = createObservable(new String[] {
                    "2", "22", "222", "2222" }, 1000, needHappenedException);
            Observable<String> observable3 = createObservable(new String[] {},
                    3000, new IOException("fake exception"));

            @SuppressWarnings("unchecked")
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
            inOrder.verify(observer, times(1)).onError(needHappenedException);
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

            @SuppressWarnings("unchecked")
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
}
