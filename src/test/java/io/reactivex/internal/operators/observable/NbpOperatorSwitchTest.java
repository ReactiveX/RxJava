/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.disposables.BooleanDisposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;

public class NbpOperatorSwitchTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;
    private Observer<String> NbpObserver;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
        NbpObserver = TestHelper.mockNbpSubscriber();
    }

    @Test
    public void testSwitchWhenOuterCompleteBeforeInner() {
        Observable<Observable<String>> source = Observable.create(new NbpOnSubscribe<Observable<String>>() {
            @Override
            public void accept(Observer<? super Observable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                publishNext(NbpObserver, 50, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 70, "one");
                        publishNext(NbpObserver, 100, "two");
                        publishCompleted(NbpObserver, 200);
                    }
                }));
                publishCompleted(NbpObserver, 60);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(2)).onNext(anyString());
        inOrder.verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSwitchWhenInnerCompleteBeforeOuter() {
        Observable<Observable<String>> source = Observable.create(new NbpOnSubscribe<Observable<String>>() {
            @Override
            public void accept(Observer<? super Observable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                publishNext(NbpObserver, 10, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 0, "one");
                        publishNext(NbpObserver, 10, "two");
                        publishCompleted(NbpObserver, 20);
                    }
                }));

                publishNext(NbpObserver, 100, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 0, "three");
                        publishNext(NbpObserver, 10, "four");
                        publishCompleted(NbpObserver, 20);
                    }
                }));
                publishCompleted(NbpObserver, 200);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(150, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onComplete();
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        inOrder.verify(NbpObserver, times(1)).onNext("four");

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyString());
        inOrder.verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSwitchWithComplete() {
        Observable<Observable<String>> source = Observable.create(new NbpOnSubscribe<Observable<String>>() {
            @Override
            public void accept(Observer<? super Observable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                publishNext(NbpObserver, 50, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(final Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 60, "one");
                        publishNext(NbpObserver, 100, "two");
                    }
                }));

                publishNext(NbpObserver, 200, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(final Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 0, "three");
                        publishNext(NbpObserver, 100, "four");
                    }
                }));

                publishCompleted(NbpObserver, 250);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyString());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("four");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithError() {
        Observable<Observable<String>> source = Observable.create(new NbpOnSubscribe<Observable<String>>() {
            @Override
            public void accept(Observer<? super Observable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                publishNext(NbpObserver, 50, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(final Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 50, "one");
                        publishNext(NbpObserver, 100, "two");
                    }
                }));

                publishNext(NbpObserver, 200, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 0, "three");
                        publishNext(NbpObserver, 100, "four");
                    }
                }));

                publishError(NbpObserver, 250, new TestException());
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyString());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyString());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testSwitchWithSubsequenceComplete() {
        Observable<Observable<String>> source = Observable.create(new NbpOnSubscribe<Observable<String>>() {
            @Override
            public void accept(Observer<? super Observable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                publishNext(NbpObserver, 50, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 50, "one");
                        publishNext(NbpObserver, 100, "two");
                    }
                }));

                publishNext(NbpObserver, 130, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishCompleted(NbpObserver, 0);
                    }
                }));

                publishNext(NbpObserver, 150, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 50, "three");
                    }
                }));
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyString());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSwitchWithSubsequenceError() {
        Observable<Observable<String>> source = Observable.create(new NbpOnSubscribe<Observable<String>>() {
            @Override
            public void accept(Observer<? super Observable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                publishNext(NbpObserver, 50, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 50, "one");
                        publishNext(NbpObserver, 100, "two");
                    }
                }));

                publishNext(NbpObserver, 130, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishError(NbpObserver, 0, new TestException());
                    }
                }));

                publishNext(NbpObserver, 150, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 50, "three");
                    }
                }));

            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(90, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyString());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, times(1)).onError(any(TestException.class));
    }

    private <T> void publishCompleted(final Observer<T> NbpObserver, long delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                NbpObserver.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Observer<T> NbpObserver, long delay, final Throwable error) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                NbpObserver.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Observer<T> NbpObserver, long delay, final T value) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                NbpObserver.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSwitchIssue737() {
        // https://github.com/ReactiveX/RxJava/issues/737
        Observable<Observable<String>> source = Observable.create(new NbpOnSubscribe<Observable<String>>() {
            @Override
            public void accept(Observer<? super Observable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                publishNext(NbpObserver, 0, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 10, "1-one");
                        publishNext(NbpObserver, 20, "1-two");
                        // The following events will be ignored
                        publishNext(NbpObserver, 30, "1-three");
                        publishCompleted(NbpObserver, 40);
                    }
                }));
                publishNext(NbpObserver, 25, Observable.create(new NbpOnSubscribe<String>() {
                    @Override
                    public void accept(Observer<? super String> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        publishNext(NbpObserver, 10, "2-one");
                        publishNext(NbpObserver, 20, "2-two");
                        publishNext(NbpObserver, 30, "2-three");
                        publishCompleted(NbpObserver, 40);
                    }
                }));
                publishCompleted(NbpObserver, 30);
            }
        });

        Observable<String> sampled = Observable.switchOnNext(source);
        sampled.subscribe(NbpObserver);

        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("1-one");
        inOrder.verify(NbpObserver, times(1)).onNext("1-two");
        inOrder.verify(NbpObserver, times(1)).onNext("2-one");
        inOrder.verify(NbpObserver, times(1)).onNext("2-two");
        inOrder.verify(NbpObserver, times(1)).onNext("2-three");
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testUnsubscribe() {
        final AtomicBoolean isUnsubscribed = new AtomicBoolean();
        Observable.switchOnNext(
                Observable.create(new NbpOnSubscribe<Observable<Integer>>() {
                    @Override
                    public void accept(final Observer<? super Observable<Integer>> NbpSubscriber) {
                        BooleanDisposable bs = new BooleanDisposable();
                        NbpSubscriber.onSubscribe(bs);
                        NbpSubscriber.onNext(Observable.just(1));
                        isUnsubscribed.set(bs.isDisposed());
                    }
                })
        ).take(1).subscribe();
        assertTrue("Switch doesn't propagate 'unsubscribe'", isUnsubscribed.get());
    }
    /** The upstream producer hijacked the switch producer stopping the requests aimed at the inner observables. */
    @Test
    public void testIssue2654() {
        Observable<String> oneItem = Observable.just("Hello").mergeWith(Observable.<String>never());
        
        Observable<String> src = oneItem.switchMap(new Function<String, Observable<String>>() {
            @Override
            public Observable<String> apply(final String s) {
                return Observable.just(s)
                        .mergeWith(Observable.interval(10, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, String>() {
                            @Override
                            public String apply(Long i) {
                                return s + " " + i;
                            }
                        })).take(250);
            }
        })
        .share()
        ;
        
        TestObserver<String> ts = new TestObserver<String>() {
            @Override
            public void onNext(String t) {
                super.onNext(t);
                if (valueCount() == 250) {
                    onComplete();
                    dispose();
                }
            }
        };
        src.subscribe(ts);
        
        ts.awaitTerminalEvent(10, TimeUnit.SECONDS);
        
        System.out.println("> testIssue2654: " + ts.valueCount());
        
        ts.assertTerminated();
        ts.assertNoErrors();
        
        Assert.assertEquals(250, ts.valueCount());
    }
}