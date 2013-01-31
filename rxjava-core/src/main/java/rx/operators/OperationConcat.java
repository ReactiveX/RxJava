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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public final class OperationConcat {

    /**
     * Combine the observable sequences from the list of Observables into one observable sequence without any transformation.
     * 
     * @param sequences
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of combining the output from the list of Observables.
     */	
	public static <T> Func1<Observer<T>, Subscription> concat(final Observable<T>... sequences) {
        return new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> observer) {
                return new Concat<T>(sequences).call(observer);
            }
        };
    }

    public static <T> Func1<Observer<T>, Subscription> concat(final List<Observable<T>> sequences) {
    	@SuppressWarnings("unchecked")
		Observable<T>[] o = sequences.toArray((Observable<T>[])Array.newInstance(Observable.class, sequences.size()));
    	return concat(o);
    }

    public static <T> Func1<Observer<T>, Subscription> concat(final Observable<Observable<T>> sequences) {   	
    	final List<Observable<T>> list = new ArrayList<Observable<T>>();
     	sequences.toList().subscribe(new Action1<List<Observable<T>>>(){
			@Override
			public void call(List<Observable<T>> t1) {
				list.addAll(t1);
			}
     		
     	});    	
     
    	return concat(list);
    }
    
    private static class Concat<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<T>[] sequences;
        private int num = 0;
        private int count = 0;
        private Subscription s;
        
        Concat(final Observable<T>... sequences) {
            this.sequences = sequences;
            this.num = sequences.length - 1;
        }
        private final AtomicObservableSubscription Subscription = new AtomicObservableSubscription();
        
        private final Subscription actualSubscription = new Subscription() {

			@Override
			public void unsubscribe() {
				if (null != s)
					s.unsubscribe();
			}       	
        };
        
        public Subscription call(Observer<T> observer) {
        	s = sequences[count].subscribe(new ConcatObserver(observer));
        	
            return Subscription.wrap(actualSubscription);
        }
        
        private class ConcatObserver implements Observer<T> {
            private final Observer<T> observer;

            ConcatObserver(Observer<T> observer) {
                this.observer = observer;
            }
            
			@Override
			public void onCompleted() {				
				if (num == count)
					observer.onCompleted();
				else {
					count++;
					s = sequences[count].subscribe(this);
				}				
			}

			@Override
			public void onError(Exception e) {
				observer.onError(e);
				
			}

			@Override
			public void onNext(T args) {
				observer.onNext(args);
				
			}       	
        }
    }
    
    public static class UnitTest {       
    	private final static String[] expected = {"1", "3", "5", "7", "2", "4", "6"};
    	private int index = 0;
     
        Observer<String> observer = new Observer<String>() {

			@Override
			public void onCompleted() {
			}
			
			@Override
			public void onError(Exception e) {
			// TODO Auto-generated method stub
			}
			
			@Override
			public void onNext(String args) {
				Assert.assertEquals(expected[index], args);
				index++;
			}		         
        };
		        
        @Before
        public void before() {
            index = 0;
        }
	
		@Test
		public void testConcat() {		        
			final String[] o = {"1", "3", "5", "7"};			        
			final String[] e = {"2", "4", "6"};			         
			        
			final Observable<String> odds  = Observable.toObservable(o);			        
			final Observable<String> even = Observable.toObservable(e);
			         			        
			@SuppressWarnings("unchecked")
	        Observable<String> concat = Observable.create(concat(odds, even));			        
			concat.subscribe(observer);   	
			Assert.assertEquals(expected.length, index);
		 
		}

		@Test
		public void testConcatWithList() {		        
			final String[] o = {"1", "3", "5", "7"};			        
			final String[] e = {"2", "4", "6"};			         
			        
			final Observable<String> odds  = Observable.toObservable(o);			        
			final Observable<String> even = Observable.toObservable(e);
			final List<Observable<String>> list = new ArrayList<Observable<String>>();
			list.add(odds);
			list.add(even);
			@SuppressWarnings("unchecked")
	        Observable<String> concat = Observable.create(concat(list));			        
			concat.subscribe(observer);   	
			Assert.assertEquals(expected.length, index);
		 
		}
		
		@Test 
		public void testConcatUnsubscribe() {
            final CountDownLatch callOnce = new CountDownLatch(1);
            final CountDownLatch okToContinue = new CountDownLatch(1);
            final TestObservable w1 = new TestObservable(null, null, "one", "two", "three");
            final TestObservable w2 = new TestObservable(callOnce, okToContinue, "four", "five", "six");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            @SuppressWarnings("unchecked")
            Observable<String> concat = Observable.create(concat(w1, w2));
            Subscription s1 = concat.subscribe(aObserver);
            
            try {
                //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext once.
            	callOnce.await();
                s1.unsubscribe();
                //Unblock the observable to continue.
                okToContinue.countDown();
                w1.t.join();   
                w2.t.join();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, times(1)).onNext("four");
            verify(aObserver, never()).onNext("five");
            verify(aObserver, never()).onNext("six");
 		}
		
        @Test
        public void testMergeObservableOfObservables() {
			final String[] o = {"1", "3", "5", "7"};			        
			final String[] e = {"2", "4", "6"};			         
			        
			final Observable<String> odds  = Observable.toObservable(o);			        
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
			@SuppressWarnings("unchecked")
	        Observable<String> concat = Observable.create(concat(observableOfObservables));			        
			concat.subscribe(observer);   	
			Assert.assertEquals(expected.length, index);
       }

		
	    private static class TestObservable extends Observable<String> {

	            private final Subscription s = new Subscription() {

					@Override
					public void unsubscribe() {
						// TODO Auto-generated method stub
						subscribed = false;
					}
	            	
	            };
	            private final String[] values;
	            private Thread t = null;
	            private int count = 0;
	            private boolean subscribed = true;
	            private final CountDownLatch once; 
	            private final CountDownLatch okToContinue; 
	            
	            public TestObservable(CountDownLatch once, CountDownLatch okToContinue, String... values) {
	                this.values = values;
	                this.once = once;
	                this.okToContinue = okToContinue;
	            } 
	            
	            @Override
	            public Subscription subscribe(final Observer<String> observer) {
	                t = new Thread(new Runnable() {

	                    @Override
	                    public void run() {
	                        try {
		                        while(count < values.length && subscribed) {
	                                observer.onNext(values[count]);
	                                count++;
	                                //Unblock the main thread to call unsubscribe.
	                                if (null != once)
	                                	once.countDown();
	                                //Block until the main thread has called unsubscribe.
	                                if (null != once)
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