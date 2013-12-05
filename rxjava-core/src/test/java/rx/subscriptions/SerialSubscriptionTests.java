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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import rx.Subscription;

public class SerialSubscriptionTests {
    private SerialSubscription serialSubscription;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        serialSubscription = new SerialSubscription();
    }

    @Test
    public void unsubscribingWithoutUnderlyingDoesNothing() {
        serialSubscription.unsubscribe();
    }

    @Test
    public void unsubscribingWithSingleUnderlyingUnsubscribes() {
        Subscription underlying = mock(Subscription.class);
        serialSubscription.setSubscription(underlying);
        underlying.unsubscribe();
        verify(underlying).unsubscribe();
    }

    @Test
    public void replacingFirstUnderlyingCausesUnsubscription() {
        Subscription first = mock(Subscription.class);
        serialSubscription.setSubscription(first);
        Subscription second = mock(Subscription.class);
        serialSubscription.setSubscription(second);
        verify(first).unsubscribe();
    }

    @Test
    public void whenUnsubscribingSecondUnderlyingUnsubscribed() {
        Subscription first = mock(Subscription.class);
        serialSubscription.setSubscription(first);
        Subscription second = mock(Subscription.class);
        serialSubscription.setSubscription(second);
        serialSubscription.unsubscribe();
        verify(second).unsubscribe();
    }

    @Test
    public void settingUnderlyingWhenUnsubscribedCausesImmediateUnsubscription() {
        serialSubscription.unsubscribe();
        Subscription underlying = mock(Subscription.class);
        serialSubscription.setSubscription(underlying);
        verify(underlying).unsubscribe();
    }

    @Test(timeout = 1000)
    public void settingUnderlyingWhenUnsubscribedCausesImmediateUnsubscriptionConcurrently()
            throws InterruptedException {
        final Subscription firstSet = mock(Subscription.class);
        serialSubscription.setSubscription(firstSet);

        final CountDownLatch start = new CountDownLatch(1);

        final int count = 10;
        final CountDownLatch end = new CountDownLatch(count);

        final List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0 ; i < count ; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        start.await();
                        serialSubscription.unsubscribe();
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    } finally {
                        end.countDown();
                    }
                }
            };
            t.start();
            threads.add(t);
        }

        final Subscription underlying = mock(Subscription.class);
        start.countDown();
        serialSubscription.setSubscription(underlying);
        end.await();
        verify(firstSet).unsubscribe();
        verify(underlying).unsubscribe();

        for (final Thread t : threads) {
            t.interrupt();
        }
    }
}
