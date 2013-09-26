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
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.AbstractSpinnerModel;
import javax.swing.BoundedRangeModel;
import javax.swing.ButtonModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultSingleSelectionModel;
import javax.swing.JMenu;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.MenuSelectionManager;
import javax.swing.SingleSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.swing.sources.ChangeEventSource;
import rx.swing.sources.ComponentEventSource;
import rx.swing.sources.KeyEventSource;
import rx.swing.sources.MouseEventSource;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

/**
 * Allows creating observables from various sources specific to Swing.
 */
public enum SwingObservable {
    ; // no instances

    public static <E extends EventObject> Observable<E> fromListenerFor(final Class<E> eventClass, final Object onComponent) {
        Method[] methods = onComponent.getClass().getMethods();

        Method potentialAddMethod = null;
        Method potentialRemoveMethod = null;
        Class<?> potentialListenerClass = null;

        for (Method method : methods) {
            if (!method.getName().endsWith("Listener"))
                continue;

            // check that the method takes an interface that has methods that take our event class
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1)
                continue;
            potentialListenerClass = parameterTypes[0];
            if (!potentialListenerClass.isInterface())
                continue;
            if (!potentialListenerClass.isInstance(EventListener.class))
                continue;

            boolean hasMatchingEventMethods = false;
            for (Method eventMethod : potentialListenerClass.getMethods()) {
                Class<?>[] eventMethodParameterTypes = eventMethod.getParameterTypes();
                if (eventMethodParameterTypes.length != 1)
                    continue;
                if (!eventMethodParameterTypes[0].isInstance(eventClass))
                    continue;

                hasMatchingEventMethods = true;
            }

            if (!hasMatchingEventMethods)
                continue;

            if (method.getName().startsWith("add")) {
                if (potentialAddMethod == null)
                    potentialAddMethod = method;
                else
                    throw new AmbiguousMethodException(potentialAddMethod, method);
            }
            if (method.getName().startsWith("remove")) {
                if (potentialRemoveMethod == null)
                    potentialRemoveMethod = method;
                else
                    throw new AmbiguousMethodException(potentialRemoveMethod, method);
            }
        }

        final Method addMethod = potentialAddMethod;
        final Method removeMethod = potentialRemoveMethod;
        final Class<?> listenerClass = potentialListenerClass;

        return Observable.create(new OnSubscribeFunc<E>() {
            @Override
            public Subscription onSubscribe(final Observer<? super E> observer) {
                Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { listenerClass }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
                        try {
                            String methodName = method.getName();
                            if (method.getDeclaringClass() == Object.class) {
                                // Handle the Object public methods.
                                if (methodName.equals("hashCode")) {
                                    return new Integer(System.identityHashCode(proxy));
                                } else if (methodName.equals("equals")) {
                                    return (proxy == arguments[0] ? Boolean.TRUE : Boolean.FALSE);
                                } else if (methodName.equals("toString")) {
                                    return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
                                }
                            }

                            if (arguments.length != 1)
                                throw new UnsupportedOperationException();
                            if (arguments[0].getClass().isInstance(eventClass))
                                throw new UnsupportedOperationException();
                            if (method.getReturnType() != Void.TYPE)
                                throw new UnsupportedOperationException();

                            observer.onNext(eventClass.cast(arguments[0]));
                        } catch (Throwable e) {
                            observer.onError(e);
                        }

                        return null;
                    }
                });

                final ActionListener listener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    }
                };
                try {
                    addMethod.invoke(onComponent, listener);
                } catch (Throwable e) {
                    observer.onError(e);
                }

                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        try {
                            removeMethod.invoke(onComponent, listener);
                        } catch (Throwable e) {
                            observer.onError(e);
                        }
                    }
                });
            }
        });
    }

    /**
     * Creates an observable corresponding to a Swing button action.
     * 
     * @param button
     *            The button to register the observable for.
     * @return Observable of action events.
     */
    public static Observable<ActionEvent> fromButtonAction(AbstractButton component) {
        return fromListenerFor(ActionEvent.class, component);
    }

    /**
     * Creates an observable corresponding to raw key events.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of key events.
     */
    public static Observable<KeyEvent> fromKeyEvents(Component component) {
        return fromListenerFor(KeyEvent.class, component);
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
     * 
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
     * 
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
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JTabbedPane component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JViewport component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JMenu component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JProgressBar component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JSlider component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final JSpinner component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final MenuSelectionManager component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final ButtonModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final BoundedRangeModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final DefaultButtonModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final DefaultBoundedRangeModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final SingleSelectionModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final DefaultSingleSelectionModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final SpinnerModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final AbstractSpinnerModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final ColorSelectionModel component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final Caret component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final Style component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final StyleContext component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    /**
     * Creates an observable corresponding to {@link ChangeEvent}s.
     * 
     * @param component
     *            The component to register the observable for.
     * @return Observable of {@link ChangeEvent}s.
     */
    public static Observable<ChangeEvent> fromChangeEvents(final DefaultCaret component) {
        return fromListenerFor(ChangeEvent.class, component);
    }

    public static class AmbiguousMethodException extends RuntimeException {
        private final Method method1;
        private final Method method2;

        public AmbiguousMethodException(Method method1, Method method2) {
            super("Not sure which method to use " + method1 + " or " + method2);
            this.method1 = method1;
            this.method2 = method2;
        }

        public Method getMethod1() {
            return method1;
        }

        public Method getMethod2() {
            return method2;
        }
    }

}
