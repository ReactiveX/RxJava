/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.disposables;

import static org.mockito.Mockito.*;

import org.junit.Test;


public class DisposablesTest {

    @Test
    public void testUnsubscribeOnlyOnce() {
        Runnable dispose = mock(Runnable.class);
        Disposable subscription = Disposables.from(dispose);
        subscription.dispose();
        subscription.dispose();
        verify(dispose, times(1)).run();
    }

    @Test
    public void testEmpty() {
        Disposable empty = Disposables.empty();
        // FIXME not assertable
//        assertFalse(empty.isUnsubscribed());
        empty.dispose();
        // FIXME not assertable
//        assertTrue(empty.isUnsubscribed());
    }

    @Test
    public void testUnsubscribed() {
        // FIXME not assertable
        /*Disposable disposed = */Disposables.disposed();
//        assertTrue(disposed.isUnsubscribed());
    }
}