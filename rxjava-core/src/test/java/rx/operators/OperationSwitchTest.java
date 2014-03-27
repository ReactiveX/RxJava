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

public class OperationSwitchTest {

    private TestScheduler scheduler;
    private Observer<String> observer;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        scheduler = new TestScheduler();
        observer = mock(Observer.class);
    }

    @Test
    public void testSwitchWhenOuterCompleteBeforeInner() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 70, "one");
                        publishNext(observer, 100, "two");
                        publishCompleted(observer, 200);
                        return Subscriptions.empty();
                    }
                }));
                publishCompleted(observer, 60);

                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationSwitch.switchDo(source));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(2)).onNext(anyString());
        inOrder.verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSwitchWhenInnerCompleteBeforeOuter() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                publishNext(observer, 10, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 0, "one");
                        publishNext(observer, 10, "two");
                        publishCompleted(observer, 20);
                        return Subscriptions.empty();
                    }
                }));

                publishNext(observer, 100, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 0, "three");
                        publishNext(observer, 10, "four");
                        publishCompleted(observer, 20);
                        return Subscriptions.empty();
                    }
                }));
                publishCompleted(observer, 200);

                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationSwitch.switchDo(source));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(150, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onCompleted();
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        inOrder.verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSwitchWithComplete() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 60, "one");
                        publishNext(observer, 100, "two");
                        return Subscriptions.empty();
                    }
                }));

                publishNext(observer, 200, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 0, "three");
                        publishNext(observer, 100, "four");
                        return Subscriptions.empty();
                    }
                }));

                publishCompleted(observer, 250);

                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationSwitch.switchDo(source));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("four");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithError() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                        return Subscriptions.empty();
                    }
                }));

                publishNext(observer, 200, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 0, "three");
                        publishNext(observer, 100, "four");
                        return Subscriptions.empty();
                    }
                }));

                publishError(observer, 250, new TestException());

                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationSwitch.switchDo(source));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("two");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testSwitchWithSubsequenceComplete() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                        return Subscriptions.empty();
                    }
                }));

                publishNext(observer, 130, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishCompleted(observer, 0);
                        return Subscriptions.empty();
                    }
                }));

                publishNext(observer, 150, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 50, "three");
                        return Subscriptions.empty();
                    }
                }));

                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationSwitch.switchDo(source));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithSubsequenceError() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                        return Subscriptions.empty();
                    }
                }));

                publishNext(observer, 130, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishError(observer, 0, new TestException());
                        return Subscriptions.empty();
                    }
                }));

                publishNext(observer, 150, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 50, "three");
                        return Subscriptions.empty();
                    }
                }));

                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationSwitch.switchDo(source));
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyString());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext("one");
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext("three");
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(TestException.class));
    }

    private <T> void publishCompleted(final Observer<T> observer, long delay) {
        scheduler.schedule(new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                observer.onCompleted();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Observer<T> observer, long delay, final Throwable error) {
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
    private class TestException extends Throwable {
    }

    @Test
    public void testSwitchIssue737() {
        // https://github.com/Netflix/RxJava/issues/737
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {
            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                publishNext(observer, 0, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 10, "1-one");
                        publishNext(observer, 20, "1-two");
                        // The following events will be ignored
                        publishNext(observer, 30, "1-three");
                        publishCompleted(observer, 40);
                        return Subscriptions.empty();
                    }
                }));
                publishNext(observer, 25, Observable.create(new Observable.OnSubscribeFunc<String>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super String> observer) {
                        publishNext(observer, 10, "2-one");
                        publishNext(observer, 20, "2-two");
                        publishNext(observer, 30, "2-three");
                        publishCompleted(observer, 40);
                        return Subscriptions.empty();
                    }
                }));
                publishCompleted(observer, 30);
                return Subscriptions.empty();
            }
        });

        Observable<String> sampled = Observable.create(OperationSwitch.switchDo(source));
        sampled.subscribe(observer);

        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("1-one");
        inOrder.verify(observer, times(1)).onNext("1-two");
        inOrder.verify(observer, times(1)).onNext("2-one");
        inOrder.verify(observer, times(1)).onNext("2-two");
        inOrder.verify(observer, times(1)).onNext("2-three");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
}
