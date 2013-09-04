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
package rx.swing.sources;

import static java.util.Arrays.*;
import static org.mockito.Mockito.*;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func2;

public enum KeyEventSource { ; // no instances

    /**
     * @see SwingObservable.fromKeyEvents(Component)
     */
    public static Observable<KeyEvent> fromKeyEventsOf(final Component component) {
        return Observable.create(new OnSubscribeFunc<KeyEvent>() {
            @Override
            public Subscription onSubscribe(final Observer<? super KeyEvent> observer) {
                final KeyListener listener = new KeyListener() {
                    @Override
                    public void keyPressed(KeyEvent event) {
                        observer.onNext(event);
                    }
  
                    @Override
                    public void keyReleased(KeyEvent event) {
                        observer.onNext(event);
                    }
  
                    @Override
                    public void keyTyped(KeyEvent event) {
                        observer.onNext(event);
                    }
                };
                component.addKeyListener(listener);
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        component.removeKeyListener(listener);
                    }
                });
            }
        });
    }

    /**
     * @see SwingObservable.fromKeyEvents(Component, Set)
     */
    public static Observable<Set<Integer>> currentlyPressedKeysOf(Component component) {
        return fromKeyEventsOf(component).<Set<Integer>>scan(new HashSet<Integer>(), new Func2<Set<Integer>, KeyEvent, Set<Integer>>() {
            @Override
            public Set<Integer> call(Set<Integer> pressedKeys, KeyEvent event) {
                Set<Integer> afterEvent = new HashSet<Integer>(pressedKeys);
                switch (event.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        afterEvent.add(event.getKeyCode());
                        break;
                        
                    case KeyEvent.KEY_RELEASED:
                        afterEvent.remove(event.getKeyCode());
                        break;
                      
                    default: // nothing to do
                }
                return afterEvent;
            }
        });
    }
    
    public static class UnitTest {
        private Component comp = new JPanel();
        
        @Test
        public void testObservingKeyEvents() {
            @SuppressWarnings("unchecked")
            Action1<KeyEvent> action = mock(Action1.class);
            @SuppressWarnings("unchecked")
            Action1<Throwable> error = mock(Action1.class);
            Action0 complete = mock(Action0.class);
            
            final KeyEvent event = mock(KeyEvent.class);
            
            Subscription sub = fromKeyEventsOf(comp).subscribe(action, error, complete);
            
            verify(action, never()).call(Matchers.<KeyEvent>any());
            verify(error, never()).call(Matchers.<Throwable>any());
            verify(complete, never()).call();
            
            fireKeyEvent(event);
            verify(action, times(1)).call(Matchers.<KeyEvent>any());
            
            fireKeyEvent(event);
            verify(action, times(2)).call(Matchers.<KeyEvent>any());
            
            sub.unsubscribe();
            fireKeyEvent(event);
            verify(action, times(2)).call(Matchers.<KeyEvent>any());
            verify(error, never()).call(Matchers.<Throwable>any());
            verify(complete, never()).call();
        }
        
        @Test
        public void testObservingPressedKeys() {
            @SuppressWarnings("unchecked")
            Action1<Set<Integer>> action = mock(Action1.class);
            @SuppressWarnings("unchecked")
            Action1<Throwable> error = mock(Action1.class);
            Action0 complete = mock(Action0.class);
            
            Subscription sub = currentlyPressedKeysOf(comp).subscribe(action, error, complete);
            
            InOrder inOrder = inOrder(action);
            inOrder.verify(action, times(1)).call(Collections.<Integer>emptySet());
            verify(error, never()).call(Matchers.<Throwable>any());
            verify(complete, never()).call();
            
            fireKeyEvent(keyEvent(1, KeyEvent.KEY_PRESSED));
            inOrder.verify(action, times(1)).call(new HashSet<Integer>(asList(1)));
            verify(error, never()).call(Matchers.<Throwable>any());
            verify(complete, never()).call();

            fireKeyEvent(keyEvent(2, KeyEvent.KEY_PRESSED));
            inOrder.verify(action, times(1)).call(new HashSet<Integer>(asList(1, 2)));

            fireKeyEvent(keyEvent(2, KeyEvent.KEY_RELEASED));
            inOrder.verify(action, times(1)).call(new HashSet<Integer>(asList(1)));

            fireKeyEvent(keyEvent(3, KeyEvent.KEY_RELEASED));
            inOrder.verify(action, times(1)).call(new HashSet<Integer>(asList(1)));

            fireKeyEvent(keyEvent(1, KeyEvent.KEY_RELEASED));
            inOrder.verify(action, times(1)).call(Collections.<Integer>emptySet());

            sub.unsubscribe();

            fireKeyEvent(keyEvent(1, KeyEvent.KEY_PRESSED));
            inOrder.verify(action, never()).call(Matchers.<Set<Integer>>any());
            verify(error, never()).call(Matchers.<Throwable>any());
            verify(complete, never()).call();
        }

        private KeyEvent keyEvent(int keyCode, int id) {
            return new KeyEvent(comp, id, -1L, 0, keyCode, ' ');
        }
        
        private void fireKeyEvent(KeyEvent event) {
            for (KeyListener listener: comp.getKeyListeners()) {
                listener.keyTyped(event);
            }
        }
    }
}
