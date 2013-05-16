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
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Ignore;
import org.junit.Test;

import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.util.Exceptions;
import rx.util.functions.Func1;

public final class OperationConcat {

    /**
     * Combine the observable sequences from the list of Observables into one
     * observable sequence without any transformation. If either the outer
     * observable or an inner observable calls onError, we will call onError.
     * <p/>
     *
     * @param sequences An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of combining the output from the list of Observables.
     */
    public static <T> Func1<Observer<T>, Subscription> concat(final Observable<T>... sequences) {
        return concat(Observable.from(sequences));
    }

    public static <T> Func1<Observer<T>, Subscription> concat(final List<Observable<T>> sequences) {
        return concat(Observable.from(sequences));
    }

    public static <T> Func1<Observer<T>, Subscription> concat(final Observable<Observable<T>> sequences) {
        return new Concat<T>(sequences);
    }

    private static class Concat<T> implements Func1<Observer<T>, Subscription> {
        private Observable<Observable<T>> sequences;
        private AtomicObservableSubscription innerSubscription = null;

        public Concat(Observable<Observable<T>> sequences) {
            this.sequences = sequences;
        }

        public Subscription call(final Observer<T> observer) {
            final AtomicBoolean completedOrErred = new AtomicBoolean(false);
            final AtomicBoolean allSequencesReceived = new AtomicBoolean(false);
            final Queue<Observable<T>> nextSequences = new ConcurrentLinkedQueue<Observable<T>>();
            final AtomicObservableSubscription outerSubscription = new AtomicObservableSubscription();

            final Observer<T> reusableObserver = new Observer<T>() {
                @Override
                public void onNext(T item) {
                    observer.onNext(item);
                }
                @Override
                public void onError(Exception e) {
                    if (completedOrErred.compareAndSet(false, true)) {
                        outerSubscription.unsubscribe();
                        observer.onError(e);
                    }
                }
                @Override
                public void onCompleted() {
                    synchronized (nextSequences) {
                        if (nextSequences.isEmpty()) {
                            // No new sequences available at the moment
                            innerSubscription = null;
                            if (allSequencesReceived.get()) {
                                // No new sequences are coming, we are finished
                                if (completedOrErred.compareAndSet(false, true)) {
                                    observer.onCompleted();
                                }
                            }
                        } else {
                            // Continue on to the next sequence
                            innerSubscription = new AtomicObservableSubscription();
                            innerSubscription.wrap(nextSequences.poll().subscribe(this));
                        }
                    }
                }
            };

            outerSubscription.wrap(sequences.subscribe(new Observer<Observable<T>>() {
                @Override
                public void onNext(Observable<T> nextSequence) {
                    synchronized (nextSequences) {
                        if (innerSubscription == null) {
                            // We are currently not subscribed to any sequence
                            innerSubscription = new AtomicObservableSubscription();
                            innerSubscription.wrap(nextSequence.subscribe(reusableObserver));
                        } else {
                            // Put this sequence at the end of the queue
                            nextSequences.add(nextSequence);
                        }
                    }
                }
                @Override
                public void onError(Exception e) {
                    if (completedOrErred.compareAndSet(false, true)) {
                        if (innerSubscription != null) {
                            innerSubscription.unsubscribe();
                        }
                        observer.onError(e);
                    }
                }
                @Override
                public void onCompleted() {
                    allSequencesReceived.set(true);
                    if (innerSubscription == null) {
                        // We are not subscribed to any sequence, and none are coming anymore
                        if (completedOrErred.compareAndSet(false, true)) {
                            observer.onCompleted();
                        }
                    }
                }
            }));

            return new Subscription() {
                @Override
                public void unsubscribe() {
                    synchronized (nextSequences) {
                        if (innerSubscription != null)
                            innerSubscription.unsubscribe();
                        outerSubscription.unsubscribe();
                    }
                }
            };
        }
    }

    public static class UnitTest {

        @Test
        public void testConcat() {
            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);

            final String[] o = { "1", "3", "5", "7" };
            final String[] e = { "2", "4", "6" };

            final Observable<String> odds = Observable.from(o);
            final Observable<String> even = Observable.from(e);

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

            final Observable<String> odds = Observable.from(o);
            final Observable<String> even = Observable.from(e);
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

            final Observable<String> odds = Observable.from(o);
            final Observable<String> even = Observable.from(e);

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
            final Observable<String> odds = Observable.from(o);
            final Observable<String> even = Observable.from(e);
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
        
      
       
        @Test
 		public void testConcatUnSubscribeNotBlockingObservables() {
 
        	final CountDownLatch okToContinueW1 = new CountDownLatch(1);
        	final CountDownLatch okToContinueW2 = new CountDownLatch(1);
        	
           	final TestObservable<String> w1 = new TestObservable<String>(null, okToContinueW1, "one", "two", "three");
           	final TestObservable<String> w2 = new TestObservable<String>(null, okToContinueW2, "four", "five", "six");

             @SuppressWarnings("unchecked")
             Observer<String> aObserver = mock(Observer.class);
             Observable<Observable<String>> observableOfObservables = Observable.create(new Func1<Observer<Observable<String>>, Subscription>() {

                 @Override
                 public Subscription call(Observer<Observable<String>> observer) {
                     // simulate what would happen in an observable
                     observer.onNext(w1);
                     observer.onNext(w2);
                     observer.onCompleted();

                     return new Subscription() {

                         @Override
                         public void unsubscribe() {
                         }

                     };
                 }

             });
             Observable<String> concat = Observable.create(concat(observableOfObservables));           
         
             concat.subscribe(aObserver);
             
             verify(aObserver, times(0)).onCompleted();

             
             //Wait for the thread to start up.
             try {
 				Thread.sleep(25);
 				w1.t.join();
 				w2.t.join();
 				okToContinueW1.countDown();
 				okToContinueW2.countDown();
			} catch (InterruptedException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
             
            InOrder inOrder = inOrder(aObserver);
            inOrder.verify(aObserver, times(1)).onNext("one");   
            inOrder.verify(aObserver, times(1)).onNext("two");
            inOrder.verify(aObserver, times(1)).onNext("three");
            inOrder.verify(aObserver, times(1)).onNext("four");   
            inOrder.verify(aObserver, times(1)).onNext("five");
            inOrder.verify(aObserver, times(1)).onNext("six");
            verify(aObserver, times(1)).onCompleted();
          
             
  		}
        
        
        /**
         * Test unsubscribing the concatenated Observable in a single thread.
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

            try {
                // Subscribe
                s1.wrap(concat.subscribe(aObserver));
                //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext once.
                callOnce.await();
                // Unsubcribe
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
            inOrder.verify(aObserver, never()).onNext("five");
            inOrder.verify(aObserver, never()).onNext("six");
            inOrder.verify(aObserver, never()).onCompleted();

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
                                        okToContinue.await(1, TimeUnit.SECONDS);
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