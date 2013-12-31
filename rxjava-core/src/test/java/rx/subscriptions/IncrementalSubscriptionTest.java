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
import org.junit.Before;
import org.junit.Test;

public class IncrementalSubscriptionTest {
    IncrementalSubscription is;
    @Before
    public void before() {
        is = new IncrementalSubscription();
    }
    @Test
    public void testSimple() {
        assertFalse(is.isUnsubscribed());
        
        BooleanSubscription b1 = new BooleanSubscription();
        
        is.setSubscription(b1, false);
        
        assertEquals(1L, is.index());
        
        long index = is.nextIndex();

        assertTrue(is.compareExchange(index, b1, false));

        assertFalse(is.compareExchange(index, b1, false));
        
        assertEquals(2L, is.index());
        
        index = is.nextIndex();
        assertTrue(is.compareExchange(index, b1, false));
        
        is.unsubscribe();
        
        BooleanSubscription b2 = new BooleanSubscription();
        
        assertFalse(is.compareExchange(index, b2, false));
        
        assertTrue(is.isUnsubscribed());
        assertTrue(b2.isUnsubscribed());
        
        assertEquals(3L, is.index());
    }
    @Test
    public void testSwapOrder() {
        BooleanSubscription b1 = new BooleanSubscription();
        
        long idx1 = is.nextIndex();
        
        long idx2 = is.nextIndex();
        assertTrue(is.compareExchange(idx2, b1, false));
        
        assertFalse(is.compareExchange(idx1, b1, false));
    }
}
