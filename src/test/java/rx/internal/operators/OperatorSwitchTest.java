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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.*;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

public class OperatorSwitchTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private Observer<String> observer;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        observer = mock(Observer.class);
    }

    @Test
    public void testSwitchWhenOuterCompleteBeforeInner() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 70, "one");
                        publishNext(observer, 100, "two");
                        publishCompleted(observer, 200);
                    }
                }));
                publishCompleted(observer, 60);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(2)).onNext(anyString());
        inOrder.verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSwitchWhenInnerCompleteBeforeOuter() {
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 10, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 0, "one");
                        publishNext(observer, 10, "two");
                        publishCompleted(observer, 20);
                    }
                }));

                publishNext(observer, 100, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 0, "three");
                        publishNext(observer, 10, "four");
                        publishCompleted(observer, 20);
                    }
                }));
                publishCompleted(observer, 200);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
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
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> observer) {
                        publishNext(observer, 60, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 200, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> observer) {
                        publishNext(observer, 0, "three");
                        publishNext(observer, 100, "four");
                    }
                }));

                publishCompleted(observer, 250);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
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
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 200, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 0, "three");
                        publishNext(observer, 100, "four");
                    }
                }));

                publishError(observer, 250, new TestException());
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
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
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishCompleted(observer, 0);
                    }
                }));

                publishNext(observer, 150, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 50, "three");
                    }
                }));
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
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
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 50, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 50, "one");
                        publishNext(observer, 100, "two");
                    }
                }));

                publishNext(observer, 130, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishError(observer, 0, new TestException());
                    }
                }));

                publishNext(observer, 150, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 50, "three");
                    }
                }));

            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
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
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Observer<T> observer, long delay, final Throwable error) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Observer<T> observer, long delay, final T value) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSwitchIssue737() {
        // https://github.com/ReactiveX/RxJava/issues/737
        Observable<Observable<String>> source = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 0, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 10, "1-one");
                        publishNext(observer, 20, "1-two");
                        // The following events will be ignored
                        publishNext(observer, 30, "1-three");
                        publishCompleted(observer, 40);
                    }
                }));
                publishNext(observer, 25, Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> observer) {
                        publishNext(observer, 10, "2-one");
                        publishNext(observer, 20, "2-two");
                        publishNext(observer, 30, "2-three");
                        publishCompleted(observer, 40);
                    }
                }));
                publishCompleted(observer, 30);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
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

    @Test
    public void testBackpressure() {
        final Observable<String> o1 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> observer) {
                observer.setProducer(new Producer() {

                    private int emitted = 0;

                    @Override
                    public void request(long n) {
                        for(int i = 0; i < n && emitted < 10 && !observer.isUnsubscribed(); i++) {
                            scheduler.advanceTimeBy(5, TimeUnit.MILLISECONDS);
                            emitted++;
                            observer.onNext("a" + emitted);
                        }
                        if(emitted == 10) {
                            observer.onCompleted();
                        }
                    }
                });
            }
        });
        final Observable<String> o2 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> observer) {
                observer.setProducer(new Producer() {

                    private int emitted = 0;

                    @Override
                    public void request(long n) {
                        for(int i = 0; i < n && emitted < 10 && !observer.isUnsubscribed(); i++) {
                            scheduler.advanceTimeBy(5, TimeUnit.MILLISECONDS);
                            emitted++;
                            observer.onNext("b" + emitted);
                        }
                        if(emitted == 10) {
                            observer.onCompleted();
                        }
                    }
                });
            }
        });
        final Observable<String> o3 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> observer) {
                observer.setProducer(new Producer() {

                    private int emitted = 0;

                    @Override
                    public void request(long n) {
                        for(int i = 0; i < n && emitted < 10 && !observer.isUnsubscribed(); i++) {
                            emitted++;
                            observer.onNext("c" + emitted);
                        }
                        if(emitted == 10) {
                            observer.onCompleted();
                        }
                    }
                });
            }
        });
        Observable<Observable<String>> o = Observable.create(new Observable.OnSubscribe<Observable<String>>() {
            @Override
            public void call(Subscriber<? super Observable<String>> observer) {
                publishNext(observer, 10, o1);
                publishNext(observer, 20, o2);
                publishNext(observer, 30, o3);
                publishCompleted(observer, 30);
            }
        });
        final TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        Observable.switchOnNext(o).subscribe(new Subscriber<String>() {

            private int requested = 0;

            @Override
            public void onStart() {
                requested = 3;
                request(3);
            }

            @Override
            public void onCompleted() {
                testSubscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                testSubscriber.onError(e);
            }

            @Override
            public void onNext(String s) {
                testSubscriber.onNext(s);
                requested--;
                if(requested == 0) {
                    requested = 3;
                    request(3);
                }
            }
        });
        scheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS);
        testSubscriber.assertReceivedOnNext(Arrays.asList("a1", "b1", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10"));
        testSubscriber.assertNoErrors();
        testSubscriber.assertTerminalEvent();
    }

    @Test
    public void testUnsubscribe() {
        final AtomicBoolean isUnsubscribed = new AtomicBoolean();
        Observable.switchOnNext(
                Observable.create(new Observable.OnSubscribe<Observable<Integer>>() {
                    @Override
                    public void call(final Subscriber<? super Observable<Integer>> subscriber) {
                        subscriber.onNext(Observable.just(1));
                        isUnsubscribed.set(subscriber.isUnsubscribed());
                    }
                })
        ).take(1).subscribe();
        assertTrue("Switch doesn't propagate 'unsubscribe'", isUnsubscribed.get());
    }
}
