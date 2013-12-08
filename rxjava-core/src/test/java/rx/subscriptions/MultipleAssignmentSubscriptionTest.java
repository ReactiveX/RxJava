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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import rx.Subscription;
import static rx.subscriptions.Subscriptions.create;
import rx.util.functions.Action0;

public class MultipleAssignmentSubscriptionTest {
    Action0 unsubscribe;
    Subscription s;
    @Before
    public void before() {
         unsubscribe = mock(Action0.class);
         s = create(unsubscribe);
    }
    @Test
    public void testNoUnsubscribeWhenReplaced() {
        MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
        
        mas.setSubscription(s);
        mas.setSubscription(null);
        mas.unsubscribe();        
        
        verify(unsubscribe, never()).call();
        
    }
    @Test
    public void testUnsubscribeWhenParentUnsubscribes() {
        MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
        mas.setSubscription(s);
        mas.unsubscribe();
        mas.unsubscribe();
        
        verify(unsubscribe, times(1)).call();
        
        Assert.assertEquals(true, mas.isUnsubscribed());
    }
    @Test
    public void testUnsubscribedDoesntLeakSentinel() {
        MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();

        mas.setSubscription(s);
        mas.unsubscribe();
        
        Assert.assertEquals(true, mas.getSubscription() == Subscriptions.empty());
    }
}
