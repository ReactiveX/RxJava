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

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum KeyEventSource { ; // no instances

    /**
     * @see rx.observables.SwingObservable#fromKeyEvents(Component)
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
     * @see rx.observables.SwingObservable#fromPressedKeys(Component)
     */
    public static Observable<Set<Integer>> currentlyPressedKeysOf(Component component) {
        class CollectKeys implements Func2<Set<Integer>, KeyEvent, Set<Integer>>{
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
        }
        
        Observable<KeyEvent> filteredKeyEvents = fromKeyEventsOf(component).filter(new Func1<KeyEvent, Boolean>() {
            @Override
            public Boolean call(KeyEvent event) {
                return event.getID() == KeyEvent.KEY_PRESSED || event.getID() == KeyEvent.KEY_RELEASED;
            }
        });
        
        return filteredKeyEvents.scan(Collections.<Integer>emptySet(), new CollectKeys());
    }

}
