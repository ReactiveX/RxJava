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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;


/**
 * This operation transforms an {@link Observable} sequence of {@link Observable} sequences into a single 
 * {@link Observable} sequence which only produces values from the most recently published {@link Observable} 
 * sequence in the sequence. 
 */
public final class OperationSwitch {

    /**
     * This function transforms an {@link Observable} sequence of {@link Observable} sequences into a single 
     * {@link Observable} sequence which produces values from the most recently published {@link Observable}.
     * 
     * @param sequences   The {@link Observable} sequence consisting of {@link Observable} sequences.
     * @return A {@link Func1} which does this transformation.
     */
    public static <T> Func1<Observer<T>, Subscription> switchDo(final Observable<Observable<T>> sequences) {
        return new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(Observer<T> observer) {
                return new Switch<T>(sequences).call(observer);
            }
        };
    }

    private static class Switch<T> implements Func1<Observer<T>, Subscription> {

        private final Observable<Observable<T>> sequences;

        public Switch(Observable<Observable<T>> sequences) {
            this.sequences = sequences;
        }

        @Override
        public Subscription call(Observer<T> observer) {
            return sequences.subscribe(new SwitchObserver<T>(observer));
        }
    }

    private static class SwitchObserver<T> implements Observer<Observable<T>> {

        private final AtomicReference<Subscription> subscription = new AtomicReference<Subscription>();
        
        private final Observer<T> observer;

        public SwitchObserver(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void onCompleted() {
            observer.onCompleted();
        }

        @Override
        public void onError(Exception e) {
            observer.onError(e);
        }

        @Override
        public void onNext(Observable<T> args) {
            synchronized (subscription) {
                Subscription previousSubscription = subscription.get();
                if (previousSubscription != null) {
                    previousSubscription.unsubscribe();
                }
                
                subscription.set(args.subscribe(new Observer<T>() {
                    @Override
                    public void onCompleted() {
                        // Do nothing.
                    }

                    @Override
                    public void onError(Exception e) {
                        // Do nothing.
                    }

                    @Override
                    public void onNext(T args) {
                        observer.onNext(args);
                    }
                }));
            }
        }
    }
    
    public static class UnitTest {

        private TestScheduler scheduler;
        private Observer<String> observer;

        @Before
        @SuppressWarnings("unchecked")
        public void before() {
            scheduler = new TestScheduler();
            observer = mock(Observer.class);
        }

        @Test
        public void testSwitchWithComplete() {
            Observable<Observable<String>> source = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {
                @Override
                public Subscription call(Observer<Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
                            publishNext(observer, 50, "one");
                            publishNext(observer, 100, "two");
                            return Subscriptions.empty();
                        }
                    }));
                    
                    publishNext(observer, 200, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
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
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("one");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
            
            scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("two");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("three");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
            
            scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyString());
            verify(observer, times(1)).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
        }

        @Test
        public void testSwitchWithError() {
            Observable<Observable<String>> source = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {
                @Override
                public Subscription call(Observer<Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
                            publishNext(observer, 50, "one");
                            publishNext(observer, 100, "two");
                            return Subscriptions.empty();
                        }
                    }));
                    
                    publishNext(observer, 200, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
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
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("one");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
            
            scheduler.advanceTimeTo(175, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("two");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(225, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("three");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
            
            scheduler.advanceTimeTo(350, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, never()).onNext(anyString());
            verify(observer, never()).onCompleted();
            verify(observer, times(1)).onError(any(TestException.class));
        }

        @Test
        public void testSwitchWithSubsequenceComplete() {
            Observable<Observable<String>> source = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {
                @Override
                public Subscription call(Observer<Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
                            publishNext(observer, 50, "one");
                            publishNext(observer, 100, "two");
                            return Subscriptions.empty();
                        }
                    }));
                    
                    publishNext(observer, 130, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
                            publishCompleted(observer, 0);
                            return Subscriptions.empty();
                        }
                    }));
                    
                    publishNext(observer, 150, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
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
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("one");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
            
            scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("two");
            inOrder.verify(observer, times(1)).onNext("three");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
        }

        @Test
        public void testSwitchWithSubsequenceError() {
            Observable<Observable<String>> source = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {
                @Override
                public Subscription call(Observer<Observable<String>> observer) {
                    publishNext(observer, 50, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
                            publishNext(observer, 50, "one");
                            publishNext(observer, 100, "two");
                            return Subscriptions.empty();
                        }
                    }));
                    
                    publishNext(observer, 130, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
                            publishError(observer, 0, new TestException());
                            return Subscriptions.empty();
                        }
                    }));
                    
                    publishNext(observer, 150, Observable.create(new Func1<Observer<String>, Subscription>() {
                        @Override
                        public Subscription call(Observer<String> observer) {
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
            verify(observer, never()).onError(any(Exception.class));

            scheduler.advanceTimeTo(125, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("one");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
            
            scheduler.advanceTimeTo(250, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("two");
            inOrder.verify(observer, times(1)).onNext("three");
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
        }

        private <T> void publishCompleted(final Observer<T> observer, long delay) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onCompleted();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private <T> void publishError(final Observer<T> observer, long delay, final Exception error) {
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
        private class TestException extends Exception { }
    }
}