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

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

public enum MouseEventSource { ; // no instances

    /**
     * @see SwingObservable.fromMouseEvents
     */
    public static Observable<MouseEvent> fromMouseEventsOf(final Component component) {
        return Observable.create(new Func1<Observer<MouseEvent>, Subscription>() {
            @Override
            public Subscription call(final Observer<MouseEvent> observer) {
                final MouseListener listener = new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent event) {
                        observer.onNext(event);
                    }

                    @Override
                    public void mousePressed(MouseEvent event) {
                        observer.onNext(event);
                    }

                    @Override
                    public void mouseReleased(MouseEvent event) {
                        observer.onNext(event);
                    }

                    @Override
                    public void mouseEntered(MouseEvent event) {
                        observer.onNext(event);
                    }

                    @Override
                    public void mouseExited(MouseEvent event) {
                        observer.onNext(event);
                    }
                };
                component.addMouseListener(listener);
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        component.removeMouseListener(listener);
                    }
                });
            }
        });
    }
    
    /**
     * @see SwingObservable.fromMouseMotionEvents
     */
    public static Observable<MouseEvent> fromMouseMotionEventsOf(final Component component) {
        return Observable.create(new Func1<Observer<MouseEvent>, Subscription>() {
            @Override
            public Subscription call(final Observer<MouseEvent> observer) {
                final MouseMotionListener listener = new MouseMotionListener() {
                    @Override
                    public void mouseDragged(MouseEvent event) {
                        observer.onNext(event);
                    }

                    @Override
                    public void mouseMoved(MouseEvent event) {
                        observer.onNext(event);
                    }
                };
                component.addMouseMotionListener(listener);
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        component.removeMouseMotionListener(listener);
                    }
                });
            }
        });
    }
}
