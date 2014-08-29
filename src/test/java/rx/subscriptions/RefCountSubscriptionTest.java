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
package rx.subscriptions;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static rx.subscriptions.Subscriptions.create;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Subscription;
import rx.functions.Action0;

public class RefCountSubscriptionTest {
    Action0 main;
    RefCountSubscription rcs;

    @Before
    public void before() {
        main = mock(Action0.class);
        rcs = new RefCountSubscription(create(main));
    }

    @Test
    public void testImmediateUnsubscribe() {
        InOrder inOrder = inOrder(main);

        rcs.unsubscribe();

        inOrder.verify(main, times(1)).call();

        rcs.unsubscribe();

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRCSUnsubscribeBeforeClient() {
        InOrder inOrder = inOrder(main);

        Subscription s = rcs.get();

        rcs.unsubscribe();

        inOrder.verify(main, never()).call();

        s.unsubscribe();

        inOrder.verify(main, times(1)).call();

        rcs.unsubscribe();
        s.unsubscribe();

        inOrder.verifyNoMoreInteractions();

    }

    @Test
    public void testMultipleClientsUnsubscribeFirst() {
        InOrder inOrder = inOrder(main);

        Subscription s1 = rcs.get();
        Subscription s2 = rcs.get();

        s1.unsubscribe();
        inOrder.verify(main, never()).call();
        s2.unsubscribe();
        inOrder.verify(main, never()).call();

        rcs.unsubscribe();
        inOrder.verify(main, times(1)).call();

        s1.unsubscribe();
        s2.unsubscribe();
        rcs.unsubscribe();

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMultipleClientsMainUnsubscribeFirst() {
        InOrder inOrder = inOrder(main);

        Subscription s1 = rcs.get();
        Subscription s2 = rcs.get();

        rcs.unsubscribe();
        inOrder.verify(main, never()).call();
        s1.unsubscribe();
        inOrder.verify(main, never()).call();
        s2.unsubscribe();

        inOrder.verify(main, times(1)).call();

        s1.unsubscribe();
        s2.unsubscribe();
        rcs.unsubscribe();

        inOrder.verifyNoMoreInteractions();
    }
}
