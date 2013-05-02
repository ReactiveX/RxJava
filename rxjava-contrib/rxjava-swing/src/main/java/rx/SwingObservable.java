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
package rx;

import static rx.Observable.filter;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JComponent;

import rx.swing.sources.AbstractButtonSource;
import rx.swing.sources.KeyEventSource;
import rx.util.functions.Func1;

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
    public static Observable<KeyEvent> fromKeyEvents(JComponent component) {
        return KeyEventSource.fromKeyEventsOf(component);
    }

    /**
     * Creates an observable corresponding to raw key events, restricted a set of given key codes.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of key events.
     */
    public static Observable<KeyEvent> fromKeyEvents(JComponent component, final Set<Integer> keyCodes) {
        return filter(fromKeyEvents(component), new Func1<KeyEvent, Boolean>() {
            @Override
            public Boolean call(KeyEvent event) {
                return keyCodes.contains(event.getKeyCode());
            }
        });
    }
}
