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
package rx.observables.operations;


import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import rx.observables.Observable;
import rx.observables.Observer;
import rx.observables.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Func1;

public final class OperationConcat {

    public static <T> Func1<Observer<T>, Subscription> concat(final Observable<T>... sequences) {
        return new OperatorSubscribeFunction<T>() {

            @Override
            public Subscription call(Observer<T> observer) {
                return new Concat<T>(sequences).call(observer);
            }
        };
    }

    private static class Concat<T> implements OperatorSubscribeFunction<T> {
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
			String[] o = {"1", "3", "5", "7"};			        
			String[] e = {"2", "4", "6"};			         
			        
			final Observable<String> odds  = Observable.toObservable(o);			        
			final Observable<String> even = Observable.toObservable(e);
			         			        
			@SuppressWarnings("unchecked")
	        Observable<String> concat = Observable.create(concat(odds, even));			        
			concat.subscribe(observer);   	
			Assert.assertEquals(expected.length, index);
		 
		}
		
		@Test 
		public void testConcatUnsubscribe() {
            CountDownLatch callOnce = new CountDownLatch(1);
            CountDownLatch okToContinue = new CountDownLatch(1);
            TestObservable w1 = new TestObservable(null, null, "one", "two", "three");
            TestObservable w2 = new TestObservable(callOnce, okToContinue, "four", "five", "six");

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
	                super(new Func1<Observer<String>, Subscription>() {

	                    @Override
	                    public Subscription call(Observer<String> t1) {
	                        // do nothing as we are overriding subscribe for testing purposes
	                        return null;
	                    }
	                });
	                
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