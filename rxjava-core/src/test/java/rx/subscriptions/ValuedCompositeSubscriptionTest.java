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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rx.Subscription;

public class ValuedCompositeSubscriptionTest {
    ValuedCompositeSubscription<Object> vcs;
    @Before
    public void before() {
        vcs = new ValuedCompositeSubscription<Object>();

        Assert.assertEquals(false, vcs.isUnsubscribed());
    }
    @Test
    public void testAddAndTerminate() {

        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.add(s2, 2);
        
        Assert.assertEquals(2, vcs.size());
        
        vcs.unsubscribe();
        
        Assert.assertEquals(true, vcs.isUnsubscribed());
        
        Assert.assertEquals(true, s1.isUnsubscribed());
        Assert.assertEquals(true, s2.isUnsubscribed());
    }
    @Test
    public void testUnsubscribeTwice() {
        BooleanSubscription s1 = new BooleanSubscription();
        vcs.add(s1, 1);

        vcs.unsubscribe();

        Assert.assertEquals(true, vcs.isUnsubscribed());
        
        vcs.unsubscribe();
        
        Assert.assertEquals(true, vcs.isUnsubscribed());
        Assert.assertEquals(0, vcs.size());
    }
    @Test
    public void testAddAfterTerminate() {
        vcs.unsubscribe();

        BooleanSubscription s1 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        
        Assert.assertEquals(0, vcs.size());
        
        Assert.assertEquals(true, s1.isUnsubscribed());
    }
    @Test
    public void testTryAddAfterTerminate() {
        vcs.unsubscribe();

        BooleanSubscription s1 = new BooleanSubscription();
        Assert.assertEquals(false, vcs.tryAdd(s1, 1));
        
        Assert.assertEquals(false, s1.isUnsubscribed());
    }
    @Test
    public void testPutIfAbsend() {
        BooleanSubscription s1 = new BooleanSubscription();
        vcs.add(s1, 1);
        
        Assert.assertEquals(false, vcs.putIfAbsent(s1, 2));
        
        Assert.assertEquals(1, vcs.get(s1));
    }
    @Test
    public void testReplaceValue() {
        BooleanSubscription s1 = new BooleanSubscription();
        vcs.add(s1, 1);

        Assert.assertEquals(1, vcs.put(s1, 2));

        Assert.assertEquals(2, vcs.get(s1));

        Assert.assertEquals(1, vcs.size());
    }
    @Test
    public void testGetKeysAndUnsubscribe() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.add(s2, 2);
        
        Collection<Subscription> coll = vcs.getKeysAndUnsubscribe();
        
        Assert.assertEquals(true, vcs.isUnsubscribed());
        Assert.assertEquals(0, vcs.size());
        
        Assert.assertEquals(Arrays.asList(s1, s2), coll);
        
        Assert.assertEquals(false, s1.isUnsubscribed());
        Assert.assertEquals(false, s2.isUnsubscribed());
    }
    @Test
    public void testGetValuesAndUnsubscribe() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.add(s2, 2);
        
        Collection<Object> coll = vcs.getValuesAndUnsubscribe();
        
        Assert.assertEquals(true, vcs.isUnsubscribed());
        Assert.assertEquals(0, vcs.size());
        
        Assert.assertEquals(Arrays.asList(1, 2), coll);
        
        Assert.assertEquals(false, s1.isUnsubscribed());
        Assert.assertEquals(false, s2.isUnsubscribed());
    }
    @Test
    public void testGetEntriesAndUnsubscribe() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.add(s2, 2);
        
        Map<Subscription, Object> coll = vcs.getEntriesAndUnsubscribe();
        
        Assert.assertEquals(true, vcs.isUnsubscribed());
        Assert.assertEquals(0, vcs.size());
        
        Map<Subscription, Object> expectedMap = new LinkedHashMap<Subscription, Object>();
        expectedMap.put(s1, 1);
        expectedMap.put(s2, 2);
        
        Assert.assertEquals(expectedMap, coll);
        
        Assert.assertEquals(false, s1.isUnsubscribed());
        Assert.assertEquals(false, s2.isUnsubscribed());
    }
    @Test
    public void testUnsubscribeAndGet() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.add(s2, 2);
        
        Collection<Object> coll = vcs.unsubscribeAndGet();
        
        Assert.assertEquals(true, vcs.isUnsubscribed());
        Assert.assertEquals(0, vcs.size());
        
        
        Assert.assertEquals(Arrays.asList(1, 2), coll);
        
        Assert.assertEquals(true, s1.isUnsubscribed());
        Assert.assertEquals(true, s2.isUnsubscribed());
    }
    @Test
    public void testDeleteUnsubscribe() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.add(s2, 2);

        vcs.deleteUnsubscribe();
        
        Assert.assertEquals(true, vcs.isUnsubscribed());
        Assert.assertEquals(0, vcs.size());
        
        Assert.assertEquals(false, s1.isUnsubscribed());
        Assert.assertEquals(false, s2.isUnsubscribed());
    }
    @Test
    public void testClear() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.add(s2, 2);

        vcs.clear();
        
        Assert.assertEquals(false, vcs.isUnsubscribed());
        Assert.assertEquals(0, vcs.size());
        
        Assert.assertEquals(true, s1.isUnsubscribed());
        Assert.assertEquals(true, s2.isUnsubscribed());
    }
    @Test
    public void testDeleteClear() {
        BooleanSubscription s1 = new BooleanSubscription();
        BooleanSubscription s2 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.add(s2, 2);

        vcs.deleteClear();
        
        Assert.assertEquals(false, vcs.isUnsubscribed());
        Assert.assertEquals(0, vcs.size());
        
        Assert.assertEquals(false, s1.isUnsubscribed());
        Assert.assertEquals(false, s2.isUnsubscribed());
    }
    @Test
    public void testDelete() {
        BooleanSubscription s1 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        
        Assert.assertEquals(1, vcs.delete(s1));
        
        Assert.assertEquals(false, s1.isUnsubscribed());
    }
    @Test
    public void testRemove() {
        BooleanSubscription s1 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        
        Assert.assertEquals(1, vcs.remove(s1));
        
        Assert.assertEquals(true, s1.isUnsubscribed());
    }
    @Test
    public void testGetOnTerminated() {
        BooleanSubscription s1 = new BooleanSubscription();
        
        vcs.add(s1, 1);
        vcs.unsubscribe();
        
        Assert.assertEquals(null, vcs.get(s1));
    }
    @Test
    public void testGetOrDefault() {
        BooleanSubscription s1 = new BooleanSubscription();

        Assert.assertEquals(1, vcs.getOrDefault(s1, 1));
    }
    @Test
    public void testGetOrDefaultTerminated() {
        vcs.unsubscribe();
        
        BooleanSubscription s1 = new BooleanSubscription();
        Assert.assertEquals(1, vcs.getOrDefault(s1, 1));
    }
    @Test
    public void testUnsubscribeAndGetTwice() {
        BooleanSubscription s1 = new BooleanSubscription();
        vcs.add(s1, 1);

        vcs.unsubscribeAndGet();

        Assert.assertEquals(true, vcs.isUnsubscribed());
        
        Collection<Object> coll = vcs.unsubscribeAndGet();
        
        Assert.assertEquals(true, vcs.isUnsubscribed());
        Assert.assertEquals(true, coll.isEmpty());
    }
}
