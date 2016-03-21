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

public class MultipleAssignmentDisposableTest {

    Runnable dispose;
    Disposable s;

    @Before
    public void before() {
        dispose = mock(Runnable.class);
        s = Disposables.from(dispose);
    }

    @Test
    public void testNoUnsubscribeWhenReplaced() {
        MultipleAssignmentDisposable mas = new MultipleAssignmentDisposable();

        mas.set(s);
        mas.set(Disposables.empty());
        mas.dispose();

        verify(dispose, never()).run();
    }

    @Test
    public void testUnsubscribeWhenParentUnsubscribes() {
        MultipleAssignmentDisposable mas = new MultipleAssignmentDisposable();
        mas.set(s);
        mas.dispose();
        mas.dispose();

        verify(dispose, times(1)).run();

        Assert.assertEquals(true, mas.isDisposed());
    }

    @Test
    public void subscribingWhenUnsubscribedCausesImmediateUnsubscription() {
        MultipleAssignmentDisposable mas = new MultipleAssignmentDisposable();
        mas.dispose();
        Disposable underlying = mock(Disposable.class);
        mas.set(underlying);
        verify(underlying).dispose();
    }

    @Test
    public void testDisposableRemainsAfterUnsubscribe() {
        MultipleAssignmentDisposable mas = new MultipleAssignmentDisposable();

        mas.set(s);
        mas.dispose();
        // FIXME MultipleAssignmentDisposable no longer retains the reference after diposing
        Assert.assertNotSame(s, mas.get());
    }
}