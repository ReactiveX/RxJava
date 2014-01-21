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

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import static org.mockito.Mockito.*;

public class MouseEventSourceTest {
    private Component comp = new JPanel();

    @Test
    public void testRelativeMouseMotion() {
        @SuppressWarnings("unchecked")
        Action1<Point> action = mock(Action1.class);
        @SuppressWarnings("unchecked")
        Action1<Throwable> error = mock(Action1.class);
        Action0 complete = mock(Action0.class);

        Subscription sub = MouseEventSource.fromRelativeMouseMotion(comp).subscribe(action, error, complete);

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
