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

import static org.mockito.Mockito.*;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public enum MouseEventSource { ; // no instances

    /**
     * @see rx.observables.SwingObservable#fromMouseEvents
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
     * @see rx.observables.SwingObservable#fromMouseMotionEvents
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
    
    public static class UnitTest {
        private Component comp = new JPanel();
        
        @Test
        public void testRelativeMouseMotion() {
            @SuppressWarnings("unchecked")
            Action1<Point> action = mock(Action1.class);
            @SuppressWarnings("unchecked")
            Action1<Exception> error = mock(Action1.class);
            Action0 complete = mock(Action0.class);
            
            Subscription sub = fromRelativeMouseMotion(comp).subscribe(action, error, complete);
            
            InOrder inOrder = inOrder(action);
            
            verify(action, never()).call(Matchers.<Point>any());
            verify(error, never()).call(Matchers.<Exception>any());
            verify(complete, never()).call();
            
            fireMouseEvent(mouseEvent(0, 0));
            verify(action, never()).call(Matchers.<Point>any());
            
            fireMouseEvent(mouseEvent(10, -5));
            inOrder.verify(action, times(1)).call(new Point(10, -5));
            
            fireMouseEvent(mouseEvent(6, 10));
            inOrder.verify(action, times(1)).call(new Point(-4, 15));
            
            sub.unsubscribe();
            fireMouseEvent(mouseEvent(0, 0));
            inOrder.verify(action, never()).call(Matchers.<Point>any());
            verify(error, never()).call(Matchers.<Exception>any());
            verify(complete, never()).call();
        }
        
        private MouseEvent mouseEvent(int x, int y) {
            return new MouseEvent(comp, MouseEvent.MOUSE_MOVED, 1L, 0, x, y, 0, false);
        }
        
        private void fireMouseEvent(MouseEvent event) {
            for (MouseMotionListener listener: comp.getMouseMotionListeners()) {
                listener.mouseMoved(event);
            }
        }
    }
}
