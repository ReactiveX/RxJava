/**
 * Copyright 2016 Netflix, Inc.
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

import org.junit.*;
import org.mockito.InOrder;

public class RefCountDisposableTest {
    Runnable main;
    RefCountDisposable rcs;

    @Before
    public void before() {
        main = mock(Runnable.class);
        rcs = new RefCountDisposable(Disposables.from(main));
    }

    @Test
    public void testImmediateUnsubscribe() {
        InOrder inOrder = inOrder(main);

        rcs.dispose();

        inOrder.verify(main, times(1)).run();

        rcs.dispose();

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRCSUnsubscribeBeforeClient() {
        InOrder inOrder = inOrder(main);

        Disposable s = rcs.get();

        rcs.dispose();

        inOrder.verify(main, never()).run();

        s.dispose();

        inOrder.verify(main, times(1)).run();

        rcs.dispose();
        s.dispose();

        inOrder.verifyNoMoreInteractions();

    }

    @Test
    public void testMultipleClientsUnsubscribeFirst() {
        InOrder inOrder = inOrder(main);

        Disposable s1 = rcs.get();
        Disposable s2 = rcs.get();

        s1.dispose();
        inOrder.verify(main, never()).run();
        s2.dispose();
        inOrder.verify(main, never()).run();

        rcs.dispose();
        inOrder.verify(main, times(1)).run();

        s1.dispose();
        s2.dispose();
        rcs.dispose();

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testMultipleClientsMainUnsubscribeFirst() {
        InOrder inOrder = inOrder(main);

        Disposable s1 = rcs.get();
        Disposable s2 = rcs.get();

        rcs.dispose();
        inOrder.verify(main, never()).run();
        s1.dispose();
        inOrder.verify(main, never()).run();
        s2.dispose();

        inOrder.verify(main, times(1)).run();

        s1.dispose();
        s2.dispose();
        rcs.dispose();

        inOrder.verifyNoMoreInteractions();
    }
}