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
package rx.subscriptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import rx.Subscription;

public class ForwardSubscriptionTest {
    @Test
    public void testSimple() {
        BooleanSubscription b1 = new BooleanSubscription();
        BooleanSubscription b2 = new BooleanSubscription();
        
        ForwardSubscription fs = new ForwardSubscription(b1);
        
        assertFalse(fs.isUnsubscribed());
        
        Subscription old = fs.replace(b2);
        
        assertEquals(old, b1);
        assertFalse(b1.isUnsubscribed());
        
        fs.unsubscribe();
        
        assertTrue(fs.isUnsubscribed());
        assertTrue(b2.isUnsubscribed());
        assertFalse(b1.isUnsubscribed());
    }
    @Test
    public void testExchange() {
        BooleanSubscription b1 = new BooleanSubscription();
        BooleanSubscription b2 = new BooleanSubscription();
        
        ForwardSubscription fs = new ForwardSubscription();
        
        assertTrue(fs.compareExchange(null, b1));
        
        assertFalse(fs.compareExchange(null, b2));
        assertFalse(b2.isUnsubscribed());
        
        assertTrue(fs.compareExchange(b1, b2));
        
        assertFalse(b1.isUnsubscribed());
    }
    @Test
    public void testSetAndUnsubscribe() {
        BooleanSubscription b1 = new BooleanSubscription();
        BooleanSubscription b2 = new BooleanSubscription();
        
        ForwardSubscription fs = new ForwardSubscription(b1);
        
        fs.setSubscription(b2);
        assertFalse(b1.isUnsubscribed());

        fs.setSubscription(b1, true);
        
        assertTrue(b2.isUnsubscribed());
    }
    @Test
    public void testExchangeAndUnsubscribe() {
        BooleanSubscription b1 = new BooleanSubscription();
        BooleanSubscription b2 = new BooleanSubscription();
        
        ForwardSubscription fs = new ForwardSubscription();
        
        assertTrue(fs.compareExchange(null, b1, true));
        
        assertFalse(fs.compareExchange(null, b2, true));
        
        assertFalse(b1.isUnsubscribed());
        assertFalse(b2.isUnsubscribed());
        
        assertTrue(fs.compareExchange(b1, b2, true));
        
        assertTrue(b1.isUnsubscribed());
    }
    @Test
    public void testSetUnsubscribed() {
        BooleanSubscription b1 = new BooleanSubscription();
        ForwardSubscription fs = new ForwardSubscription();
        fs.unsubscribe();
        fs.setSubscription(b1);
        
        assertTrue(b1.isUnsubscribed());
    }
    @Test
    public void testExchangeUnsubscribed() {
        BooleanSubscription b1 = new BooleanSubscription();
        ForwardSubscription fs = new ForwardSubscription();
        fs.unsubscribe();
        fs.compareExchange(null, b1);
        
        assertTrue(b1.isUnsubscribed());
    }
    @Test
    public void testExchangeUnsubscribed2() {
        BooleanSubscription b1 = new BooleanSubscription();
        ForwardSubscription fs = new ForwardSubscription();
        fs.unsubscribe();
        fs.compareExchange(null, b1, true);
        
        assertTrue(b1.isUnsubscribed());
    }
}
