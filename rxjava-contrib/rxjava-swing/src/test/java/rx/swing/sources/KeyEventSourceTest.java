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
package rx.swing.sources;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.swing.JPanel;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

public class KeyEventSourceTest {
    private Component comp = new JPanel();

    @Test
    public void testObservingKeyEvents() throws Throwable {
        SwingTestHelper.create().runInEventDispatchThread(new Action0() {

            @Override
            public void call() {
                @SuppressWarnings("unchecked")
                Action1<KeyEvent> action = mock(Action1.class);
                @SuppressWarnings("unchecked")
                Action1<Throwable> error = mock(Action1.class);
                Action0 complete = mock(Action0.class);

                final KeyEvent event = mock(KeyEvent.class);

                Subscription sub = KeyEventSource.fromKeyEventsOf(comp)
                        .subscribe(action, error, complete);

                verify(action, never()).call(Matchers.<KeyEvent> any());
                verify(error, never()).call(Matchers.<Throwable> any());
                verify(complete, never()).call();

                fireKeyEvent(event);
                verify(action, times(1)).call(Matchers.<KeyEvent> any());

                fireKeyEvent(event);
                verify(action, times(2)).call(Matchers.<KeyEvent> any());

                sub.unsubscribe();
                fireKeyEvent(event);
                verify(action, times(2)).call(Matchers.<KeyEvent> any());
                verify(error, never()).call(Matchers.<Throwable> any());
                verify(complete, never()).call();
            }

        }).awaitTerminal();
    }

    @Test
    public void testObservingPressedKeys() throws Throwable {
        SwingTestHelper.create().runInEventDispatchThread(new Action0() {

            @Override
            public void call() {
                @SuppressWarnings("unchecked")
                Action1<Set<Integer>> action = mock(Action1.class);
                @SuppressWarnings("unchecked")
                Action1<Throwable> error = mock(Action1.class);
                Action0 complete = mock(Action0.class);

                Subscription sub = KeyEventSource.currentlyPressedKeysOf(comp)
                        .subscribe(action, error, complete);

                InOrder inOrder = inOrder(action);
                fireKeyEvent(keyEvent(1, KeyEvent.KEY_PRESSED));
                inOrder.verify(action, times(1)).call(
                        new HashSet<Integer>(asList(1)));
                verify(error, never()).call(Matchers.<Throwable> any());
                verify(complete, never()).call();

                fireKeyEvent(keyEvent(2, KeyEvent.KEY_PRESSED));
                fireKeyEvent(keyEvent(KeyEvent.VK_UNDEFINED, KeyEvent.KEY_TYPED));
                inOrder.verify(action, times(1)).call(
                        new HashSet<Integer>(asList(1, 2)));

                fireKeyEvent(keyEvent(2, KeyEvent.KEY_RELEASED));
                inOrder.verify(action, times(1)).call(
                        new HashSet<Integer>(asList(1)));

                fireKeyEvent(keyEvent(3, KeyEvent.KEY_RELEASED));
                inOrder.verify(action, times(1)).call(
                        new HashSet<Integer>(asList(1)));

                fireKeyEvent(keyEvent(1, KeyEvent.KEY_RELEASED));
                inOrder.verify(action, times(1)).call(
                        Collections.<Integer> emptySet());

                sub.unsubscribe();

                fireKeyEvent(keyEvent(1, KeyEvent.KEY_PRESSED));
                inOrder.verify(action, never()).call(
                        Matchers.<Set<Integer>> any());
                verify(error, never()).call(Matchers.<Throwable> any());
                verify(complete, never()).call();
            }

        }).awaitTerminal();
    }

    private KeyEvent keyEvent(int keyCode, int id) {
        return new KeyEvent(comp, id, -1L, 0, keyCode, ' ');
    }

    private void fireKeyEvent(KeyEvent event) {
        for (KeyListener listener : comp.getKeyListeners()) {
            listener.keyTyped(event);
        }
    }
}
