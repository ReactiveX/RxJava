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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.TestScheduler;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

/**
 * Transforms an Observable that emits Observables into a single Observable that
 * emits the items emitted by the most recently published of those Observables.
 * <p>
 * <img width="640" src=
 * "https://github.com/Netflix/RxJava/wiki/images/rx-operators/switchDo.png">
 */
public final class OperationSwitch {

    /**
     * This function transforms an {@link Observable} sequence of
     * {@link Observable} sequences into a single {@link Observable} sequence
     * which produces values from the most recently published {@link Observable}
     * .
     * 
     * @param sequences
     *            The {@link Observable} sequence consisting of
     *            {@link Observable} sequences.
     * @return A {@link Func1} which does this transformation.
     */
    public static <T> OnSubscribeFunc<T> switchDo(final Observable<? extends Observable<? extends T>> sequences) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new Switch<T>(sequences).onSubscribe(observer);
            }
        };
    }

    private static class Switch<T> implements OnSubscribeFunc<T> {

        private final Observable<? extends Observable<? extends T>> sequences;

        public Switch(Observable<? extends Observable<? extends T>> sequences) {
            this.sequences = sequences;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            SafeObservableSubscription subscription = new SafeObservableSubscription();
            subscription.wrap(sequences.subscribe(new SwitchObserver<T>(observer, subscription)));
            return subscription;
        }
    }

    private static class SwitchObserver<T> implements Observer<Observable<? extends T>> {

        private final Object                         gate;
        private final Observer<? super T>            observer;
        private final SafeObservableSubscription     parent;
        private final MultipleAssignmentSubscription innerSubscription;
        private long                                 latest;
        private boolean                              stopped;
        private boolean                              hasLatest;

        public SwitchObserver(Observer<? super T> observer, SafeObservableSubscription parent) {
            this.observer = observer;
            this.parent = parent;
            this.gate = new Object();
            this.innerSubscription = new MultipleAssignmentSubscription();
        }

        @Override
        public void onNext(Observable<? extends T> args) {
            final long id;
            synchronized (gate) {
                id = ++latest;
                hasLatest = true;
            }

            final SafeObservableSubscription sub;
            sub = new SafeObservableSubscription();
            sub.wrap(args.subscribe(new Observer<T>() {
                @Override
                public void onNext(T args) {
                    synchronized (gate) {
                        if (latest == id) {
                            observer.onNext(args);
                        }
                    }
                }

                @Override
                public void onError(Throwable e) {
                    synchronized (gate) {
                        sub.unsubscribe();
                        if (latest == id) {
                            observer.onError(e);
                            parent.unsubscribe();
                        }
                    }
                }

                @Override
                public void onCompleted() {
                    synchronized (gate) {
                        sub.unsubscribe();
                        if (latest == id) {
                            hasLatest = false;
                        }

                        if (stopped) {
                            observer.onCompleted();
                            parent.unsubscribe();
                        }

                    }
                }

            }));

            innerSubscription.setSubscription(sub);
        }

        @Override
        public void onError(Throwable e) {
            synchronized (gate) {
                observer.onError(e);
            }

            parent.unsubscribe();
        }

        @Override
        public void onCompleted() {
            synchronized (gate) {
                innerSubscription.unsubscribe();
                stopped = true;
                if (!hasLatest) {
                    observer.onCompleted();
                    parent.unsubscribe();
                }
            }
        }

    }

    public static class UnitTest {

        private TestScheduler    scheduler;
        private Observer<String> observer;

        @Before
        @SuppressWarnings("unchecked")
        public void before() {
            scheduler = new TestScheduler();
            observer = mock(Observer.class);
        }

        @Test
        public void testSwitchWhenOuterCompleteBeforeInner() {
            Observable<Observable<String>> source = Observable.create(new OnSubscribeFunc<Observable<String>>() {
                @Override
                public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new OnSubscribeFunc<String>() {
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
            Observable<Observable<String>> source = Observable.create(new OnSubscribeFunc<Observable<String>>() {
                @Override
                public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                    publishNext(observer, 10, Observable.create(new OnSubscribeFunc<String>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super String> observer) {
                            publishNext(observer, 0, "one");
                            publishNext(observer, 10, "two");
                            publishCompleted(observer, 20);
                            return Subscriptions.empty();
                        }
                    }));

                    publishNext(observer, 100, Observable.create(new OnSubscribeFunc<String>() {
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
            Observable<Observable<String>> source = Observable.create(new OnSubscribeFunc<Observable<String>>() {
                @Override
                public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new OnSubscribeFunc<String>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super String> observer) {
                            publishNext(observer, 60, "one");
                            publishNext(observer, 100, "two");
                            return Subscriptions.empty();
                        }
                    }));

                    publishNext(observer, 200, Observable.create(new OnSubscribeFunc<String>() {
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
            Observable<Observable<String>> source = Observable.create(new OnSubscribeFunc<Observable<String>>() {
                @Override
                public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new OnSubscribeFunc<String>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super String> observer) {
                            publishNext(observer, 50, "one");
                            publishNext(observer, 100, "two");
                            return Subscriptions.empty();
                        }
                    }));

                    publishNext(observer, 200, Observable.create(new OnSubscribeFunc<String>() {
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
            Observable<Observable<String>> source = Observable.create(new OnSubscribeFunc<Observable<String>>() {
                @Override
                public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new OnSubscribeFunc<String>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super String> observer) {
                            publishNext(observer, 50, "one");
                            publishNext(observer, 100, "two");
                            return Subscriptions.empty();
                        }
                    }));

                    publishNext(observer, 130, Observable.create(new OnSubscribeFunc<String>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super String> observer) {
                            publishCompleted(observer, 0);
                            return Subscriptions.empty();
                        }
                    }));

                    publishNext(observer, 150, Observable.create(new OnSubscribeFunc<String>() {
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
            Observable<Observable<String>> source = Observable.create(new OnSubscribeFunc<Observable<String>>() {
                @Override
                public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new OnSubscribeFunc<String>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super String> observer) {
                            publishNext(observer, 50, "one");
                            publishNext(observer, 100, "two");
                            return Subscriptions.empty();
                        }
                    }));

                    publishNext(observer, 130, Observable.create(new OnSubscribeFunc<String>() {
                        @Override
                        public Subscription onSubscribe(Observer<? super String> observer) {
                            publishError(observer, 0, new TestException());
                            return Subscriptions.empty();
                        }
                    }));

                    publishNext(observer, 150, Observable.create(new OnSubscribeFunc<String>() {
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
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onCompleted();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private <T> void publishError(final Observer<T> observer, long delay, final Throwable error) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onError(error);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private <T> void publishNext(final Observer<T> observer, long delay, final T value) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onNext(value);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        @SuppressWarnings("serial")
        private class TestException extends Throwable {
        }
    }
}
