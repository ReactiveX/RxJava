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
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.JMenu;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.MenuSelectionManager;
import javax.swing.ButtonModel;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SingleSelectionModel;
import javax.swing.DefaultSingleSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.AbstractSpinnerModel;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.text.Caret;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.DefaultCaret;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import rx.Observable;
import rx.swing.sources.*;
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
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param w
     *            Wrapper around the component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(ChangeEventSource.ChangeEventComponentWrapper w) {
        return ChangeEventSource.fromChangeEventsOf(w);
    }
    
    // There is no common base interface for all components with {add/remove}ChangeListener methods.
    // So we have to add a fromChangeEvents overload for each component which fires ChangeEvents.
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final AbstractButton component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JTabbedPane component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JViewport component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JMenu component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JProgressBar component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JSlider component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JSpinner component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final MenuSelectionManager component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final ButtonModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final BoundedRangeModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final DefaultButtonModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final DefaultBoundedRangeModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final SingleSelectionModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final DefaultSingleSelectionModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final SpinnerModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final AbstractSpinnerModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final ColorSelectionModel component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final Caret component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final Style component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final StyleContext component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }
    
    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final DefaultCaret component) {
        return fromChangeEvents(new ChangeEventSource.ChangeEventComponentWrapper() {
            @Override
            public void addChangeListener(ChangeListener l) {
                component.addChangeListener(l);
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
                component.removeChangeListener(l);
            }
        });
    }

}
