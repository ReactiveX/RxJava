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
package rx.observables;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import rx.Observable;
import rx.functions.Func1;
import rx.swing.sources.AbstractButtonSource;
import rx.swing.sources.ComponentEventSource;
import rx.swing.sources.KeyEventSource;
import rx.swing.sources.MouseEventSource;

/**
 * Allows creating observables from various sources specific to Swing. 
 */
public enum SwingObservable { ; // no instances

    /**
     * Creates an observable corresponding to a Swing button action.
     * 
     * @param button 
     *            The button to register the observable for.
     * @return Observable of action events.
     */
    public static Observable<ActionEvent> fromButtonAction(AbstractButton button) {
        return AbstractButtonSource.fromActionOf(button);
    }

    /**
     * Creates an observable corresponding to raw key events.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of key events.
     */
    public static Observable<KeyEvent> fromKeyEvents(Component component) {
        return KeyEventSource.fromKeyEventsOf(component);
    }

    /**
     * Creates an observable corresponding to raw key events, restricted a set of given key codes.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of key events.
     */
    public static Observable<KeyEvent> fromKeyEvents(Component component, final Set<Integer> keyCodes) {
        return fromKeyEvents(component).filter(new Func1<KeyEvent, Boolean>() {
            @Override
            public Boolean call(KeyEvent event) {
                return keyCodes.contains(event.getKeyCode());
            }
        });
    }

    /**
     * Creates an observable that emits the set of all currently pressed keys each time
     * this set changes. 
     * @param component
     *            The component to register the observable for.
     * @return Observable of currently pressed keys.
     */
    public static Observable<Set<Integer>> fromPressedKeys(Component component) {
        return KeyEventSource.currentlyPressedKeysOf(component);
    }

    /**
     * Creates an observable corresponding to raw mouse events (excluding mouse motion events).
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of mouse events.
     */
    public static Observable<MouseEvent> fromMouseEvents(Component component) {
        return MouseEventSource.fromMouseEventsOf(component);
    }

    /**
     * Creates an observable corresponding to raw mouse motion events.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of mouse motion events.
     */
    public static Observable<MouseEvent> fromMouseMotionEvents(Component component) {
        return MouseEventSource.fromMouseMotionEventsOf(component);
    }
    
    /**
     * Creates an observable corresponding to relative mouse motion.
     * @param component
     *            The component to register the observable for.
     * @return A point whose x and y coordinate represent the relative horizontal and vertical mouse motion.
     */
    public static Observable<Point> fromRelativeMouseMotion(Component component) {
        return MouseEventSource.fromRelativeMouseMotion(component);
    }
    
    /**
     * Creates an observable corresponding to raw component events.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of component events.
     */
    public static Observable<ComponentEvent> fromComponentEvents(Component component) {
        return ComponentEventSource.fromComponentEventsOf(component);
    }

    /**
     * Creates an observable corresponding to component resize events.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable emitting the current size of the given component after each resize event.
     */
    public static Observable<Dimension> fromResizing(Component component) {
        return ComponentEventSource.fromResizing(component);
    }

    /**
     * Check if the current thead is the event dispatch thread.
     * 
     * @throws IllegalStateException if the current thread is not the event dispatch thread.
     */
    public static void assertEventDispatchThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Need to run in the event dispatch thread, but was " + Thread.currentThread());
        }
    }
}
