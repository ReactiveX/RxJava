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
package rx.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.internal.util.SubscriptionList;

public class SubscriptionListTest {

    @Test
    public void testSuccess() {
        final AtomicInteger counter = new AtomicInteger();
        final int numberOfSubscriptions = 2;
        SubscriptionList subscriptions = createSubscriptions(counter, numberOfSubscriptions);

        subscriptions.unsubscribe();

        assertEquals(2, counter.get());
    }

    @Test(timeout = 1000)
    public void shouldUnsubscribeAll() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        final int numberOfSubscriptions = 10;
        
        SubscriptionList subscriptions = createSubscriptions(counter, numberOfSubscriptions);
        
        final CountDownLatch start = new CountDownLatch(1);
        
        final List<Thread> threads = createUnsubscribeThreads(start, subscriptions, numberOfSubscriptions);
        
        start.countDown();
        
        joinThreads(threads);

        assertEquals(numberOfSubscriptions, counter.get());
    }

    @Test
    public void testRuntimeException() {
        
        final AtomicInteger counter = new AtomicInteger();
        final int numberOfSubscriptions = 1;
        
        SubscriptionList subscriptions = createSubscriptions(counter, numberOfSubscriptions);
        addThrowableSubscriptions(subscriptions, new RuntimeException("failed on first one"), numberOfSubscriptions);
        
        try {
        	subscriptions.unsubscribe();
            fail("Expecting an exception");
        } catch (RuntimeException e) {
            // we expect this
            assertEquals(e.getMessage(), "failed on first one");
        }

        // we should still have unsubscribed to the second one
        assertEquals(1, counter.get());
    }

    @Test
    public void testCompositeException() {
    	final AtomicInteger counter = new AtomicInteger();
        final int numberOfSubscriptions = 1;
        
        SubscriptionList subscriptions = createSubscriptions(counter, numberOfSubscriptions);
        addThrowableSubscriptions(subscriptions, new RuntimeException("failed on first one"), numberOfSubscriptions);
        addThrowableSubscriptions(subscriptions, new RuntimeException("failed on second one too"), numberOfSubscriptions);

        try {
        	subscriptions.unsubscribe();
            fail("Expecting an exception");
        } catch (CompositeException e) {
            // we expect this
            assertEquals(e.getExceptions().size(), 2);
        }

        // we should still have unsubscribed to the second one
        assertEquals(1, counter.get());
    }


    @Test
    public void testUnsubscribeIdempotence() {
    	final AtomicInteger counter = new AtomicInteger();
        final int numberOfSubscriptions = 1;
        
        SubscriptionList subscriptions = createSubscriptions(counter, numberOfSubscriptions);

        subscriptions.unsubscribe();
        subscriptions.unsubscribe();
        subscriptions.unsubscribe();

        // we should have only unsubscribed once
        assertEquals(1, counter.get());
    }

    @Test(timeout = 1000)
    public void testUnsubscribeIdempotenceConcurrently()
            throws InterruptedException {
    	final AtomicInteger counter = new AtomicInteger();
        final int numberOfSubscriptions = 1;

        SubscriptionList subscriptions = createSubscriptions(counter, numberOfSubscriptions);

       
        final CountDownLatch start = new CountDownLatch(1);
        final int numberOfThreads = 10;
        final List<Thread> threads = createUnsubscribeThreads(start, subscriptions, numberOfThreads);
        
        start.countDown();
        
        joinThreads(threads);
        

        start.countDown();

        // we should have only unsubscribed once
        assertEquals(1, counter.get());
    }
    
    @Test
    public void testRemoveSuccess(){
    	SubscriptionList subscriptions = new SubscriptionList();
    	Subscription subscription = createSubscribedSubscription();
    	
    	subscriptions.add(subscription);
    	Assert.assertFalse(subscription.isUnsubscribed());
    	
    	subscriptions.remove(subscription);
    	Assert.assertTrue(subscription.isUnsubscribed());
    }
    
    @Test
    public void testHasSubscriptionsAfterRemove(){
    	SubscriptionList subscriptions = new SubscriptionList();
    	Subscription subscription = createSubscribedSubscription();
    	
    	subscriptions.add(subscription);
    	Assert.assertTrue(subscriptions.hasSubscriptions());
    	
    	subscriptions.remove(subscription);
    	Assert.assertFalse(subscriptions.hasSubscriptions());
    }
    
    @Test
    public void testHasSubscriptionsAfterUnsubscribe(){
    	SubscriptionList subscriptions = new SubscriptionList();
    	Subscription subscription = createSubscribedSubscription();
    	
    	subscriptions.add(subscription);
    	Assert.assertTrue(subscriptions.hasSubscriptions());
    	
    	subscriptions.unsubscribe();
    	Assert.assertFalse(subscriptions.hasSubscriptions());
    }
    
    @Test
    public void testClearSuccess(){
    	SubscriptionList subscriptions = new SubscriptionList();
    	Subscription subscription = createSubscribedSubscription();
    	subscriptions.add(subscription);
    	
    	Assert.assertFalse(subscription.isUnsubscribed());
    	
    	subscriptions.clear();
    	
    	Assert.assertTrue(subscription.isUnsubscribed());
    	Assert.assertFalse(subscriptions.hasSubscriptions());
    }
    
    private SubscriptionList createSubscriptions(final AtomicInteger counter, final int numberOfSubscriptions){
    	SubscriptionList subscriptions = new SubscriptionList();
    	for(int i = 0; i < numberOfSubscriptions; ++i){
    		subscriptions.add(new Subscription() {

    			@Override
    			public void unsubscribe() {
    				counter.incrementAndGet();
    			}

    			@Override
    			public boolean isUnsubscribed() {
    				return false;
    			}
    		});
    	}
    	
        return subscriptions;
    }
    
	private SubscriptionList addThrowableSubscriptions(final SubscriptionList subscriptions, final RuntimeException runtimeException, final int numberOfSubscriptions){
    	for(int i = 0; i < numberOfSubscriptions; ++i){
    		subscriptions.add(new Subscription() {

    			@Override
    			public void unsubscribe() {
    				throw runtimeException;
    			}

    			@Override
    			public boolean isUnsubscribed() {
    				return false;
    			}
    		});
    	}
    	
        return subscriptions;
    }
	
	private List<Thread> createUnsubscribeThreads(final CountDownLatch start, final SubscriptionList subscriptions, final int numberOfThreads){
		final List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < numberOfThreads; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        start.await();
                        subscriptions.unsubscribe();
                    } catch (final InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
            };
            t.start();
            threads.add(t);
        }
        return threads;
	}
	
	private void joinThreads(List<Thread> threads) throws InterruptedException{
		for (final Thread t : threads) {
            t.join();
        }
	}
	
	private Subscription createSubscribedSubscription(){
		return new Subscription() {
			
    		private boolean unsubscribed = false;
    		
			@Override
			public void unsubscribe() {
				unsubscribed = true;
			}
			
			@Override
			public boolean isUnsubscribed() {
				return unsubscribed;
			}
		};
	}
    
}
