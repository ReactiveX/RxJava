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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public enum MouseEventSource { ; // no instances

    /**
     * @see rx.observables.SwingObservable#fromMouseEvents
     */
    public static Observable<MouseEvent> fromMouseEventsOf(final Component component) {
        return Observable.create(new OnSubscribeFunc<MouseEvent>() {
            @Override
            public Subscription onSubscribe(final Observer<? super MouseEvent> observer) {
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
     * @see rx.observables.SwingObservable#fromMouseMotionEvents
     */
    public static Observable<MouseEvent> fromMouseMotionEventsOf(final Component component) {
        return Observable.create(new OnSubscribeFunc<MouseEvent>() {
            @Override
            public Subscription onSubscribe(final Observer<? super MouseEvent> observer) {
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
    
    /**
     * @see rx.observables.SwingObservable#fromRelativeMouseMotion
     */
    public static Observable<Point> fromRelativeMouseMotion(final Component component) {
        class OldAndRelative {
            public final Point old;
            public final Point relative;

            private OldAndRelative(Point old, Point relative) {
                this.old = old;
                this.relative = relative;
            }
        }
        
        class Relativize implements Func2<OldAndRelative, MouseEvent, OldAndRelative> {
            @Override
            public OldAndRelative call(OldAndRelative last, MouseEvent event) {
                Point current = new Point(event.getX(), event.getY());
                Point relative = new Point(current.x - last.old.x, current.y - last.old.y);
                return new OldAndRelative(current, relative);
            }
        }
        
        class OnlyRelative implements Func1<OldAndRelative, Point> {
            @Override
            public Point call(OldAndRelative oar) {
                return oar.relative;
            }
        }
        
        return fromMouseMotionEventsOf(component)
                    .scan(new OldAndRelative(new Point(0, 0), new Point(0, 0)), new Relativize())
                    .map(new OnlyRelative())
                    .skip(2); // skip the useless initial value and the invalid first computation
    }

}
