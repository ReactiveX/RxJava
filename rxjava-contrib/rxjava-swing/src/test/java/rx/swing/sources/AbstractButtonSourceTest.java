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
import org.mockito.Matchers;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static org.mockito.Mockito.*;

public class AbstractButtonSourceTest {
    @Test
    public void testObservingActionEvents() {
        @SuppressWarnings("unchecked")
        Action1<ActionEvent> action = mock(Action1.class);
        @SuppressWarnings("unchecked")
        Action1<Throwable> error = mock(Action1.class);
        Action0 complete = mock(Action0.class);

        final ActionEvent event = new ActionEvent(this, 1, "command");

        @SuppressWarnings("serial")
        class TestButton extends AbstractButton {
            void testAction() {
                fireActionPerformed(event);
            }
        }

        TestButton button = new TestButton();
        Subscription sub = AbstractButtonSource.fromActionOf(button).subscribe(action, error, complete);

        verify(action, never()).call(Matchers.<ActionEvent>any());
        verify(error, never()).call(Matchers.<Throwable>any());
        verify(complete, never()).call();

        button.testAction();
        verify(action, times(1)).call(Matchers.<ActionEvent>any());

        button.testAction();
        verify(action, times(2)).call(Matchers.<ActionEvent>any());

        sub.unsubscribe();
        button.testAction();
        verify(action, times(2)).call(Matchers.<ActionEvent>any());
        verify(error, never()).call(Matchers.<Throwable>any());
        verify(complete, never()).call();
    }
}
