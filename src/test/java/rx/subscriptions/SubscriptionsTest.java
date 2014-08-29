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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static rx.subscriptions.Subscriptions.create;

import org.junit.Test;

import rx.Subscription;
import rx.functions.Action0;

public class SubscriptionsTest {

    @Test
    public void testUnsubscribeOnlyOnce() {
        Action0 unsubscribe = mock(Action0.class);
        Subscription subscription = create(unsubscribe);
        subscription.unsubscribe();
        subscription.unsubscribe();
        verify(unsubscribe, times(1)).call();
    }
}
