/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.schedulers;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import rx.Subscription;
import rx.functions.Actions;
import rx.subscriptions.Subscriptions;

public class SubscriptionQueueTest {
    SubscriptionQueue sq;
    @Before
    public void before() {
        sq = new SubscriptionQueue();
    }
    @Test
    public void testSimpleQueueing() {
        Subscription s1 = Subscriptions.create(Actions.empty());
        sq.enqueue(s1);
        sq.dequeue(s1);
        
        assertFalse(s1.isUnsubscribed());
        assertEquals(0, sq.size);
        
        sq.dequeue(s1);
        assertEquals(0, sq.size);
    }
    @Test
    public void testEnqueueAndUnsubscribe() {
        Subscription s1 = Subscriptions.create(Actions.empty());
        sq.enqueue(s1);
        
        sq.unsubscribe();
        
        assertTrue(sq.isUnsubscribed());
        assertTrue(s1.isUnsubscribed());
        assertEquals(0, sq.size);
    }
    
    @Test
    public void testUnsubscribeAndEnqueue() {
        Subscription s1 = Subscriptions.create(Actions.empty());
        
        sq.unsubscribe();
        
        sq.enqueue(s1);

        assertTrue(sq.isUnsubscribed());
        assertTrue(s1.isUnsubscribed());
        assertEquals(0, sq.size);
    }
    
    @Test
    public void testDequeueNotQueuedItem() {
        Subscription s1 = Subscriptions.create(Actions.empty());
        Subscription s2 = Subscriptions.create(Actions.empty());
        
        sq.enqueue(s1);
        
        sq.dequeue(s2);
        
        assertEquals(1, sq.size);
        
        sq.dequeue(s1);
        
        assertEquals(0, sq.size);
    }
    @Test
    public void testDequeueInDifferentOrder() {
        Subscription s1 = Subscriptions.create(Actions.empty());
        Subscription s2 = Subscriptions.create(Actions.empty());
        Subscription s3 = Subscriptions.create(Actions.empty());
        
        sq.enqueue(s1);
        sq.enqueue(s2);
        sq.enqueue(s3);
        
        assertEquals(3, sq.size);
        
        sq.dequeue(s2);

        assertEquals(2, sq.size);

        sq.dequeue(s1);

        assertEquals(1, sq.size);
        
        sq.dequeue(s3);
        
        assertEquals(0, sq.size);
    }
    @Test
    public void testMultipleProducerSingleConsumer() throws InterruptedException {
        final int n = 1000;
        final List<Subscription> list = new ArrayList<Subscription>();
        final int k = 4;
        
        Thread[] threads = new Thread[k];
        
        for (int i = 0; i < k; i++) {
            final int j = i;
            threads[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    int p = n / k;
                    for (int m = 0; m < p; m++) {
                        sq.enqueue(list.get(p * j + m));
                    }
                }
                
            });
            threads[i].start();
        }
        
        for (int i = 0; i < k; i++) {
            threads[i].join();
        }
        
        int r = n;
        for (Subscription s : list) {
            sq.dequeue(s);
            assertEquals(--r, sq.size);
        }
    }
}
