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

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

public class SingleAssignmentSubscriptionTest {
    SingleAssignmentSubscription sas;
    @Before
    public void before() {
        sas = new SingleAssignmentSubscription();
        
        Assert.assertEquals(false, sas.isUnsubscribed());
    }
    @Test(expected = IllegalStateException.class)
    public void setTwice() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        sas.setSubscription(s1);
        
        sas.setSubscription(s2);
    }
    @Test
    public void setToAnUnsubscribed() {
        BooleanSubscription s1 = new BooleanSubscription();
        
        sas.unsubscribe();
        
        Assert.assertEquals(true, sas.isUnsubscribed());
        
        sas.setSubscription(s1);
        
        Assert.assertEquals(true, s1.isUnsubscribed());
    }
    @Test
    public void setUnsubscribeSet() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        sas.setSubscription(s1);
        
        sas.unsubscribe();
        
        sas.setSubscription(s2);
        
        Assert.assertEquals(true, sas.isUnsubscribed());
        Assert.assertEquals(true, s1.isUnsubscribed());
        Assert.assertEquals(true, s2.isUnsubscribed());
        
    }
}
