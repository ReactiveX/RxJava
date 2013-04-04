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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.util.AtomicObservableSubscription;
import rx.util.Exceptions;
import rx.util.functions.Func1;

public final class OperationConcat {

    /**
     * Combine the observable sequences from the list of Observables into one
     * observable sequence without any transformation.  If either the outer
     * observable or an inner observable calls onError, we will call onError.
     *
     * <p/>
     *
     * The outer observable might run on a separate thread from (one of) the
     * inner observables; in this case care must be taken to avoid a deadlock.
     * The Concat operation may block the outer thread while servicing an inner
     * thread in order to ensure a well-defined ordering of elements; therefore
     * none of the inner threads must be implemented in a way that might wait on
     * the outer thread.
     *
     * <p/>
     *
     * Beware that concat(o1,o2).subscribe() is a blocking call from
     * which it is impossible to unsubscribe if observables are running on same thread.
     *
     * @param sequences An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of combining the output from the list of Observables.
     */
    public static <T> Func1<Observer<T>, Subscription> concat(final Observable<T>... sequences) {
        return concat(Observable.toObservable(sequences));
    }

    public static <T> Func1<Observer<T>, Subscription> concat(final List<Observable<T>> sequences) {
        return concat(Observable.toObservable(sequences));
    }

    public static <T> Func1<Observer<T>, Subscription> concat(final Observable<Observable<T>> sequences) {
        return new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> observer) {
                return new ConcatSubscription<T>(sequences, observer);
            }
        };
    }

    private static class ConcatSubscription<T> extends BooleanSubscription {
        // Might be updated by an inner thread's onError during the outer
        // thread's onNext, then read in the outer thread's onComplete.
        final AtomicBoolean innerError = new AtomicBoolean(false);

        public ConcatSubscription(Observable<Observable<T>> sequences, final Observer<T> observer) {
            final AtomicObservableSubscription outerSubscription = new AtomicObservableSubscription();
            outerSubscription.wrap(sequences.subscribe(new Observer<Observable<T>>() {
                @Override
                public void onNext(Observable<T> nextSequence) {
                    // We will not return from onNext until the inner observer completes.
                    // NB: while we are in onNext, the well-behaved outer observable will not call onError or onCompleted.
                    final CountDownLatch latch = new CountDownLatch(1);
                    final AtomicObservableSubscription innerSubscription = new AtomicObservableSubscription();
                    innerSubscription.wrap(nextSequence.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T item) {
                            // Make our best-effort to release resources in the face of unsubscribe.
                            if (isUnsubscribed()) {
                                innerSubscription.unsubscribe();
                                outerSubscription.unsubscribe();
                            } else {
                                observer.onNext(item);
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            outerSubscription.unsubscribe();
                            innerError.set(true);
                            observer.onError(e);
                            latch.countDown();
                        }
                        @Override
                        public void onCompleted() {
                            // Continue on to the next sequence
                            latch.countDown();
                        }
                    }));
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw Exceptions.propagate(e);
                    }
                }
                @Override
                public void onError(Exception e) {
                    // NB: a well-behaved observable will not interleave on{Next,Error,Completed} calls.
                    observer.onError(e);
                }
                @Override
                public void onCompleted() {
                    // NB: a well-behaved observable will not interleave on{Next,Error,Completed} calls.
                    if (!innerError.get()) {
                        observer.onCompleted();
                    }
                }
            }));
        }
    }

    public static class UnitTest {

        @Test
        public void testConcat() {
            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);

            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };

            final Observable<String> odds = Observable.toObservable(o);
            final Observable<String> even = Observable.toObservable(e);

            @SuppressWarnings("unchecked")
            Observable<String> concat = Observable.create(concat(odds, even));
            concat.subscribe(observer);

            verify(observer, times(7)).onNext(anyString());
        }

        @Test
        public void testConcatWithList() {
            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);

            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };

            final Observable<String> odds = Observable.toObservable(o);
            final Observable<String> even = Observable.toObservable(e);
            final List<Observable<String>> list = new ArrayList<Observable<String>>();
            list.add(odds);
            list.add(even);
            Observable<String> concat = Observable.create(concat(list));
            concat.subscribe(observer);

            verify(observer, times(7)).onNext(anyString());
        }

        @Test
        public void testConcatObservableOfObservables() {
            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);

            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };

            final Observable<String> odds = Observable.toObservable(o);
            final Observable<String> even = Observable.toObservable(e);

            Observable<Observable<String>> observableOfObservables = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {

                    @Override
                    public Subscription call(Observer<Observable<String>> observer) {
                        // simulate what would happen in an observable
                        observer.onNext(odds);
                        observer.onNext(even);
                        observer.onCompleted();

                        return new Subscription() {

                            @Override
                            public void unsubscribe() {
                                // unregister ... will never be called here since we are executing synchronously
                            }

                        };
                    }

                });
            Observable<String> concat = Observable.create(concat(observableOfObservables));           
            
            concat.subscribe(observer);
  
            verify(observer, times(7)).onNext(anyString());
        }

        /**
         * Simple concat of 2 asynchronous observables ensuring it emits in correct order.
         */
        @SuppressWarnings("unchecked")
        @Test
        public void testSimpleAsyncConcat() {
            Observer<String> observer = mock(Observer.class);

            TestObservable<String> o1 = new TestObservable<String>("one", "two", "three");
            TestObservable<String> o2 = new TestObservable<String>("four", "five", "six");

            Observable.concat(o1, o2).subscribe(observer);

            try {
                // wait for async observables to complete
                o1.t.join();
                o2.t.join();
            } catch (Exception e) {
                throw new RuntimeException("failed waiting on threads");
            }

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext("one");
            inOrder.verify(observer, times(1)).onNext("two");
            inOrder.verify(observer, times(1)).onNext("three");
            inOrder.verify(observer, times(1)).onNext("four");
            inOrder.verify(observer, times(1)).onNext("five");
            inOrder.verify(observer, times(1)).onNext("six");
        }

        /**
         * Test an async Observable that emits more async Observables
         */
        @SuppressWarnings("unchecked")
        @Test
        public void testNestedAsyncConcat() throws Exception {
            Observer<String> observer = mock(Observer.class);

            final TestObservable<String> o1 = new TestObservable<String>("one", "two", "three");
            final TestObservable<String> o2 = new TestObservable<String>("four", "five", "six");
            final TestObservable<String> o3 = new TestObservable<String>("seven", "eight", "nine");
            final CountDownLatch allowThird = new CountDownLatch(1);

            final AtomicReference<Thread> parent = new AtomicReference<Thread>();
            Observable<Observable<String>> observableOfObservables = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {

                    @Override
                    public Subscription call(final Observer<Observable<String>> observer) {
                        final BooleanSubscription s = new BooleanSubscription();
                        parent.set(new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        // emit first
                                        if (!s.isUnsubscribed()) {
                                            System.out.println("Emit o1");
                                            observer.onNext(o1);
                                        }
                                        // emit second
                                        if (!s.isUnsubscribed()) {
                                            System.out.println("Emit o2");
                                            observer.onNext(o2);
                                        }

                                        // wait until sometime later and emit third
                                        try {
                                            allowThird.await();
                                        } catch (InterruptedException e) {
                                            observer.onError(e);
                                        }
                                        if (!s.isUnsubscribed()) {
                                            System.out.println("Emit o3");
                                            observer.onNext(o3);
                                        }

                                    } catch (Exception e) {
                                        observer.onError(e);
                                    } finally {
                                        System.out.println("Done parent Observable");
                                        observer.onCompleted();
                                    }
                                }
                            }));
                        parent.get().start();
                        return s;
                    }
                });

            Observable.create(concat(observableOfObservables)).subscribe(observer);

            // wait for parent to start
            while (parent.get() == null) {
                Thread.sleep(1);
            }

            try {
                // wait for first 2 async observables to complete
                while (o1.t == null) {
                    Thread.sleep(1);
                }
                System.out.println("Thread1 started ... waiting for it to complete ...");
                o1.t.join();
                while (o2.t == null) {
                    Thread.sleep(1);
                }
                System.out.println("Thread2 started ... waiting for it to complete ...");
                o2.t.join();
            } catch (Exception e) {
                throw new RuntimeException("failed waiting on threads", e);
            }

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext("one");
            inOrder.verify(observer, times(1)).onNext("two");
            inOrder.verify(observer, times(1)).onNext("three");
            inOrder.verify(observer, times(1)).onNext("four");
            inOrder.verify(observer, times(1)).onNext("five");
            inOrder.verify(observer, times(1)).onNext("six");
            // we shouldn't have the following 3 yet
            inOrder.verify(observer, never()).onNext("seven");
            inOrder.verify(observer, never()).onNext("eight");
            inOrder.verify(observer, never()).onNext("nine");
            // we should not be completed yet
            verify(observer, never()).onCompleted();
            verify(observer, never()).onError(any(Exception.class));

            // now allow the third
            allowThird.countDown();

            try {
                while (o3.t == null) {
                    Thread.sleep(1);
                }
                // wait for 3rd to complete
                o3.t.join();
            } catch (Exception e) {
                throw new RuntimeException("failed waiting on threads", e);
            }

            inOrder.verify(observer, times(1)).onNext("seven");
            inOrder.verify(observer, times(1)).onNext("eight");
            inOrder.verify(observer, times(1)).onNext("nine");

            inOrder.verify(observer, times(1)).onCompleted();
            verify(observer, never()).onError(any(Exception.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testBlockedObservableOfObservables() {
            Observer<String> observer = mock(Observer.class);

            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };
            final Observable<String> odds = Observable.toObservable(o);
            final Observable<String> even = Observable.toObservable(e);
            final CountDownLatch callOnce = new CountDownLatch(1);
            final CountDownLatch okToContinue = new CountDownLatch(1);
            TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(callOnce, okToContinue, odds, even);
            Func1<Observer<String>, Subscription> concatF = concat(observableOfObservables);
            Observable<String> concat = Observable.create(concatF);
            concat.subscribe(observer);
            try {
                //Block main thread to allow observables to serve up o1.
                callOnce.await();
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
            // The concated observable should have served up all of the odds.
            verify(observer, times(1)).onNext("1");
            verify(observer, times(1)).onNext("3");
            verify(observer, times(1)).onNext("5");
            verify(observer, times(1)).onNext("7");

            try {
                // unblock observables so it can serve up o2 and complete
                okToContinue.countDown();
                observableOfObservables.t.join();
            } catch (Exception ex) {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
            // The concatenated observable should now have served up all the evens.
            verify(observer, times(1)).onNext("2");
            verify(observer, times(1)).onNext("4");
            verify(observer, times(1)).onNext("6");
        }
        
        @Test
		public void testConcatConcurrentWithInfinity() {
            final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
            //This observable will send "hello" MAX_VALUE time.
            final TestObservable<String> w2 = new TestObservable<String>("hello", Integer.MAX_VALUE);

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            @SuppressWarnings("unchecked")
 			TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(w1, w2);
            Func1<Observer<String>, Subscription> concatF = concat(observableOfObservables);
            
            Observable<String> concat = Observable.create(concatF);
            
            concat.take(50).subscribe(aObserver);

            //Wait for the thread to start up.
            try {
				Thread.sleep(25);
				w1.t.join();
				w2.t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            InOrder inOrder = inOrder(aObserver);
            inOrder.verify(aObserver, times(1)).onNext("one");   
            inOrder.verify(aObserver, times(1)).onNext("two");
            inOrder.verify(aObserver, times(1)).onNext("three");
            inOrder.verify(aObserver, times(47)).onNext("hello");
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, never()).onError(any(Exception.class));
            
 		}
        
        
        /**
         * The outer observable is running on the same thread and subscribe() in this case is a blocking call. Calling unsubscribe() is no-op because the sequence is complete. 
         */
        @Test
        public void testConcatUnsubscribe() {
            final CountDownLatch callOnce = new CountDownLatch(1);
            final CountDownLatch okToContinue = new CountDownLatch(1);
            final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
            final TestObservable<String> w2 = new TestObservable<String>(callOnce, okToContinue, "four", "five", "six");

            @SuppressWarnings("unchecked")
            final Observer<String> aObserver = mock(Observer.class);
            @SuppressWarnings("unchecked")
            final Observable<String> concat = Observable.create(concat(w1, w2));
            final AtomicObservableSubscription s1 = new AtomicObservableSubscription();
            Thread t = new Thread() {
                    @Override
                    public void run() {
                        // NB: this statement does not complete until after "six" has been delivered.
                        s1.wrap(concat.subscribe(aObserver));
                    }
                };
            t.start();
            try {
                //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext once.
                callOnce.await();
                // NB: This statement has no effect, since s1 cannot possibly
                // wrap anything until "six" has been delivered, which cannot
                // happen until we okToContinue.countDown()
                s1.unsubscribe();
                //Unblock the observable to continue.
                okToContinue.countDown();
                w1.t.join();
                w2.t.join();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            InOrder inOrder = inOrder(aObserver);
            inOrder.verify(aObserver, times(1)).onNext("one");
            inOrder.verify(aObserver, times(1)).onNext("two");
            inOrder.verify(aObserver, times(1)).onNext("three");
            inOrder.verify(aObserver, times(1)).onNext("four");
            // NB: you might hope that five and six are not delivered, but see above.
            inOrder.verify(aObserver, times(1)).onNext("five");
            inOrder.verify(aObserver, times(1)).onNext("six");
            inOrder.verify(aObserver, times(1)).onCompleted();

        }
       
        /**
         * All observables will be running in different threads so subscribe() is unblocked.  CountDownLatch is only used in order to call unsubscribe() in a predictable manner.  
         */
        @Test
		public void testConcatUnsubscribeConcurrent() {
            final CountDownLatch callOnce = new CountDownLatch(1);
            final CountDownLatch okToContinue = new CountDownLatch(1);
            final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
            final TestObservable<String> w2 = new TestObservable<String>(callOnce, okToContinue, "four", "five", "six");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            @SuppressWarnings("unchecked")
 			TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(w1, w2);
            Func1<Observer<String>, Subscription> concatF = concat(observableOfObservables);
            
            Observable<String> concat = Observable.create(concatF);
           
            Subscription s1 = concat.subscribe(aObserver);
            
            try {
                //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext exactly once.
            	callOnce.await();
            	//"four" from w2 has been processed by onNext()
                s1.unsubscribe();
                //"five" and "six" will NOT be processed by onNext()
                //Unblock the observable to continue.
                okToContinue.countDown();
                w1.t.join();   
                w2.t.join();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            InOrder inOrder = inOrder(aObserver);
            inOrder.verify(aObserver, times(1)).onNext("one");
            inOrder.verify(aObserver, times(1)).onNext("two");
            inOrder.verify(aObserver, times(1)).onNext("three");
            inOrder.verify(aObserver, times(1)).onNext("four");
            inOrder.verify(aObserver, never()).onNext("five");
            inOrder.verify(aObserver, never()).onNext("six");
            verify(aObserver, never()).onCompleted();
            verify(aObserver, never()).onError(any(Exception.class));
        }
        
        private static class TestObservable<T> extends Observable<T> {

            private final Subscription s = new Subscription() {

                    @Override
                    public void unsubscribe() {
                        subscribed = false;
                    }

                };
            private final List<T> values;
            private Thread t = null;
            private int count = 0;
            private boolean subscribed = true;
            private final CountDownLatch once;
            private final CountDownLatch okToContinue;
            private final T seed;
            private final int size;
            
            public TestObservable(T... values) {
                this(null, null, values);
            }

            public TestObservable(CountDownLatch once, CountDownLatch okToContinue, T... values) {
                this.values = Arrays.asList(values);
                this.size = this.values.size();
                this.once = once;
                this.okToContinue = okToContinue;
                this.seed = null;
            }

            public TestObservable(T seed, int size) {
            	values = null;
            	once = null;
            	okToContinue = null;
            	this.seed = seed;
            	this.size = size;
            }
            
            
            @Override
            public Subscription subscribe(final Observer<T> observer) {
                t = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                while (count < size && subscribed) {
                                	if (null != values)
                                		observer.onNext(values.get(count));
                                	else
                                		observer.onNext(seed);
                                    count++;
                                    //Unblock the main thread to call unsubscribe.
                                    if (null != once)
                                        once.countDown();
                                    //Block until the main thread has called unsubscribe.
                                    if (null != okToContinue)
                                        okToContinue.await();
                                }
                                if (subscribed)
                                    observer.onCompleted();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                fail(e.getMessage());
                            }
                        }

                    });
                t.start();
                return s;
            }

        }

    }
}