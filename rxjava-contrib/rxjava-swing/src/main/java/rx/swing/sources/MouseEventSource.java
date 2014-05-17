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
package rx.swing.sources;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.observables.SwingObservable;
import rx.subscriptions.SwingSubscriptions;

public enum MouseEventSource {
    ; // no instances

    /**
     * @see rx.observables.SwingObservable#fromMouseEvents
     */
    public static Observable<MouseEvent> fromMouseEventsOf(final Component component) {
        return Observable.create(new OnSubscribe<MouseEvent>() {
            @Override
            public void call(final Subscriber<? super MouseEvent> subscriber) {
                SwingObservable.assertEventDispatchThread();
                final MouseListener listener = new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent event) {
                        subscriber.onNext(event);
                    }

                    @Override
                    public void mousePressed(MouseEvent event) {
                        subscriber.onNext(event);
                    }

                    @Override
                    public void mouseReleased(MouseEvent event) {
                        subscriber.onNext(event);
                    }

                    @Override
                    public void mouseEntered(MouseEvent event) {
                        subscriber.onNext(event);
                    }

                    @Override
                    public void mouseExited(MouseEvent event) {
                        subscriber.onNext(event);
                    }
                };
                component.addMouseListener(listener);

                subscriber.add(SwingSubscriptions.unsubscribeInEventDispatchThread(new Action0() {
                    @Override
                    public void call() {
                        component.removeMouseListener(listener);
                    }
                }));
            }
        });
    }

    /**
     * @see rx.observables.SwingObservable#fromMouseMotionEvents
     */
    public static Observable<MouseEvent> fromMouseMotionEventsOf(final Component component) {
        return Observable.create(new OnSubscribe<MouseEvent>() {
            @Override
            public void call(final Subscriber<? super MouseEvent> subscriber) {
                SwingObservable.assertEventDispatchThread();
                final MouseMotionListener listener = new MouseMotionListener() {
                    @Override
                    public void mouseDragged(MouseEvent event) {
                        subscriber.onNext(event);
                    }

                    @Override
                    public void mouseMoved(MouseEvent event) {
                        subscriber.onNext(event);
                    }
                };
                component.addMouseMotionListener(listener);

                subscriber.add(SwingSubscriptions.unsubscribeInEventDispatchThread(new Action0() {
                    @Override
                    public void call() {
                        component.removeMouseMotionListener(listener);
                    }
                }));
            }
        });
    }

    /**
     * @see rx.observables.SwingObservable#fromRelativeMouseMotion
     */
    public static Observable<Point> fromRelativeMouseMotion(final Component component) {
        final Observable<MouseEvent> events = fromMouseMotionEventsOf(component);
        return Observable.zip(events, events.skip(1), new Func2<MouseEvent, MouseEvent, Point>() {
            @Override
            public Point call(MouseEvent ev1, MouseEvent ev2) {
                return new Point(ev2.getX() - ev1.getX(), ev2.getY() - ev1.getY());
            }
        });
    }

    public static class UnitTest {
        private Component comp = new JPanel();

        @Test
        public void testRelativeMouseMotion() throws Throwable {
            SwingTestHelper.create().runInEventDispatchThread(new Action0() {

                @Override
                public void call() {
                    @SuppressWarnings("unchecked")
                    Action1<Point> action = mock(Action1.class);
                    @SuppressWarnings("unchecked")
                    Action1<Throwable> error = mock(Action1.class);
                    Action0 complete = mock(Action0.class);

                    Subscription sub = fromRelativeMouseMotion(comp).subscribe(action, error, complete);

                    InOrder inOrder = inOrder(action);

                    verify(action, never()).call(Matchers.<Point> any());
                    verify(error, never()).call(Matchers.<Exception> any());
                    verify(complete, never()).call();

                    fireMouseEvent(mouseEvent(0, 0));
                    verify(action, never()).call(Matchers.<Point> any());

                    fireMouseEvent(mouseEvent(10, -5));
                    inOrder.verify(action, times(1)).call(new Point(10, -5));

                    fireMouseEvent(mouseEvent(6, 10));
                    inOrder.verify(action, times(1)).call(new Point(-4, 15));

                    sub.unsubscribe();
                    fireMouseEvent(mouseEvent(0, 0));
                    inOrder.verify(action, never()).call(Matchers.<Point> any());
                    verify(error, never()).call(Matchers.<Exception> any());
                    verify(complete, never()).call();
                }

            }).awaitTerminal();
        }

        private MouseEvent mouseEvent(int x, int y) {
            return new MouseEvent(comp, MouseEvent.MOUSE_MOVED, 1L, 0, x, y, 0, false);
        }

        private void fireMouseEvent(MouseEvent event) {
            for (MouseMotionListener listener : comp.getMouseMotionListeners()) {
                listener.mouseMoved(event);
            }
        }
    }
}
