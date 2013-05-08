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

import static rx.swing.sources.ComponentEventSource.Predicate.RESIZED;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.SwingObservable;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

public enum ComponentEventSource { ; // no instances

    /**
     * @see SwingObservable.fromComponentEvents
     */
    public static Observable<ComponentEvent> fromComponentEventsOf(final Component component) {
        return Observable.create(new Func1<Observer<ComponentEvent>, Subscription>() {
            @Override
            public Subscription call(final Observer<ComponentEvent> observer) {
                final ComponentListener listener = new ComponentListener() {
                    @Override
                    public void componentHidden(ComponentEvent event) {
                        observer.onNext(event);
                    }

                    @Override
                    public void componentMoved(ComponentEvent event) {
                        observer.onNext(event);
                    }

                    @Override
                    public void componentResized(ComponentEvent event) {
                        observer.onNext(event);
                    }

                    @Override
                    public void componentShown(ComponentEvent event) {
                        observer.onNext(event);
                    }
                };
                component.addComponentListener(listener);
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        component.removeComponentListener(listener);
                    }
                });
            }
        });
    }
    
    /**
     * @see SwingObservable.fromResizing
     */
    public static Observable<Dimension> fromResizing(final Component component) {
        return fromComponentEventsOf(component).filter(RESIZED).map(new Func1<ComponentEvent, Dimension>() {
            @Override
            public Dimension call(ComponentEvent event) {
                return event.getComponent().getSize();
            }
        });
    }
    
    /**
     * Predicates that help with filtering observables for specific component events. 
     */
    public enum Predicate implements rx.util.functions.Func1<java.awt.event.ComponentEvent, Boolean> { 
        RESIZED(ComponentEvent.COMPONENT_RESIZED),
        HIDDEN(ComponentEvent.COMPONENT_HIDDEN),
        MOVED(ComponentEvent.COMPONENT_MOVED),
        SHOWN(ComponentEvent.COMPONENT_SHOWN);
        
        private final int id;
        
        private Predicate(int id) {
            this.id = id;
        }
        
        @Override
        public Boolean call(ComponentEvent event) {
            return event.getID() == id;
        }
    }
}
