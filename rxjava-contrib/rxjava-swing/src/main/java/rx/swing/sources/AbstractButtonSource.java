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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;

import org.junit.Test;
import org.mockito.Matchers;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public enum AbstractButtonSource { ; // no instances

    /**
     * @see rx.observables.SwingObservable#fromButtonAction
     */
    public static Observable<ActionEvent> fromActionOf(final AbstractButton button) {
        return Observable.create(new Func1<Observer<ActionEvent>, Subscription>() {
            @Override
            public Subscription call(final Observer<ActionEvent> observer) {
                final ActionListener listener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        observer.onNext(e);
                    }
                };
                button.addActionListener(listener);
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        button.removeActionListener(listener);
                    }
                });
            }
        });
    }

    public static class UnitTest {
        @Test
        public void testObservingActionEvents() {
            @SuppressWarnings("unchecked")
            Action1<ActionEvent> action = mock(Action1.class);
            @SuppressWarnings("unchecked")
            Action1<Exception> error = mock(Action1.class);
            Action0 complete = mock(Action0.class);
            
            final ActionEvent event = new ActionEvent(this, 1, "command");
            
            @SuppressWarnings("serial")
            class TestButton extends AbstractButton {
                void testAction() {
                    fireActionPerformed(event);
                }
            }
            
            TestButton button = new TestButton();
            Subscription sub = fromActionOf(button).subscribe(action, error, complete);
            
            verify(action, never()).call(Matchers.<ActionEvent>any());
            verify(error, never()).call(Matchers.<Exception>any());
            verify(complete, never()).call();
            
            button.testAction();
            verify(action, times(1)).call(Matchers.<ActionEvent>any());
            
            button.testAction();
            verify(action, times(2)).call(Matchers.<ActionEvent>any());
            
            sub.unsubscribe();
            button.testAction();
            verify(action, times(2)).call(Matchers.<ActionEvent>any());
            verify(error, never()).call(Matchers.<Exception>any());
            verify(complete, never()).call();
        }
    }
}
