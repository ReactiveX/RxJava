package rx.swing.sources;

import static org.mockito.Mockito.*;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.junit.Test;
import org.mockito.Matchers;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public enum KeyEventSource { ; // no instances

    public static Observable<KeyEvent> fromKeyEventsOf(final JComponent component) {
        return Observable.create(new Func1<Observer<KeyEvent>, Subscription>() {
            @Override
            public Subscription call(final Observer<KeyEvent> observer) {
                final KeyListener listener = new KeyListener() {
                    @Override
                    public void keyPressed(KeyEvent event) {
                        observer.onNext(event);
                    }
  
                    @Override
                    public void keyReleased(KeyEvent event) {
                        observer.onNext(event);
                    }
  
                    @Override
                    public void keyTyped(KeyEvent event) {
                        observer.onNext(event);
                    }
                };
                component.addKeyListener(listener);
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        component.removeKeyListener(listener);
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
            
            final KeyEvent event = mock(KeyEvent.class);
            
            JComponent comp = new JPanel();
            
            Subscription sub = fromKeyEventsOf(comp).subscribe(action, error, complete);
            
            verify(action, never()).call(Matchers.<ActionEvent>any());
            verify(error, never()).call(Matchers.<Exception>any());
            verify(complete, never()).call();
            
            fireKeyEvent(comp, event);
            verify(action, times(1)).call(Matchers.<ActionEvent>any());
            
            fireKeyEvent(comp, event);
            verify(action, times(2)).call(Matchers.<ActionEvent>any());
            
            sub.unsubscribe();
            fireKeyEvent(comp, event);
            verify(action, times(2)).call(Matchers.<ActionEvent>any());
            verify(error, never()).call(Matchers.<Exception>any());
            verify(complete, never()).call();
        }
        
        private static void fireKeyEvent(JComponent component, KeyEvent event) {
            for (KeyListener listener: component.getKeyListeners()) {
                listener.keyTyped(event);
            }
        }
    }
}
