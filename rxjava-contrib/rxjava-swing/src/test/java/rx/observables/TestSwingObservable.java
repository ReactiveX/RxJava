package rx.observables;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.AbstractSpinnerModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultSingleSelectionModel;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.MenuSelectionManager;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.colorchooser.DefaultColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyleContext.NamedStyle;

import org.junit.Test;

import rx.Observable;
import rx.util.functions.Action1;

public class TestSwingObservable {

    @Test
    public void testFromButtonAction() {
        JButton component = new JButton();
        Observable<ActionEvent> ob = SwingObservable.fromButtonAction(component);
        final ActionEvent sentEvent = new ActionEvent(component, 0, "");
        ob.first().subscribe(new Action1<ActionEvent>() {
            @Override
            public void call(ActionEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ActionListener[] listeners = component.getActionListeners();
        assertEquals(1, listeners.length);
        listeners[0].actionPerformed(sentEvent);
    }

    @Test
    public void testFromKeyEventsComponent() {
        Component component = new JButton();
        Observable<KeyEvent> ob = SwingObservable.fromKeyEvents(component);
        final KeyEvent sentEvent = new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a');
        ob.first().subscribe(new Action1<KeyEvent>() {
            @Override
            public void call(KeyEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        KeyListener[] listeners = component.getKeyListeners();
        assertEquals(1, listeners.length);
        listeners[0].keyPressed(sentEvent);
    }

    @Test
    public void testFromKeyEventsComponentSetOfInteger() {
        Component component = new JButton();
        Observable<KeyEvent> ob = SwingObservable.fromKeyEvents(component, Collections.singleton(KeyEvent.VK_A));
        final KeyEvent sentEvent = new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a');
        ob.first().subscribe(new Action1<KeyEvent>() {
            @Override
            public void call(KeyEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        KeyListener[] listeners = component.getKeyListeners();
        assertEquals(1, listeners.length);
        listeners[0].keyPressed(sentEvent);
    }

    @Test
    public void testFromPressedKeys() {
        Component component = new JButton();
        Observable<Set<Integer>> ob = SwingObservable.fromPressedKeys(component);
        final KeyEvent sentEvent = new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a');
        ob.subscribe(new Action1<Set<Integer>>() {
            @Override
            public void call(Set<Integer> recievedEvent) {
                if (!recievedEvent.isEmpty()) {
                    assertEquals(Collections.singleton(KeyEvent.VK_A), recievedEvent);
                }
            }
        });
        KeyListener[] listeners = component.getKeyListeners();
        assertEquals(1, listeners.length);
        listeners[0].keyPressed(sentEvent);
    }

    @Test
    public void testFromMouseEvents() {
        Component component = new JButton();
        Observable<MouseEvent> ob = SwingObservable.fromMouseEvents(component);
        final MouseEvent sentEvent = new MouseEvent(component, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, 10, 1, false);
        ob.first().subscribe(new Action1<MouseEvent>() {
            @Override
            public void call(MouseEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        MouseListener[] listeners = component.getMouseListeners();
        assertEquals(2, listeners.length);
        listeners[1].mouseClicked(sentEvent);
    }

    @Test
    public void testFromMouseMotionEvents() {
        Component component = new JButton();
        Observable<MouseEvent> ob = SwingObservable.fromMouseMotionEvents(component);
        final MouseEvent sentEvent = new MouseEvent(component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 10, 10, 1, false);
        ob.first().subscribe(new Action1<MouseEvent>() {
            @Override
            public void call(MouseEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        MouseListener[] listeners = component.getMouseListeners();
        assertEquals(1, listeners.length);
        listeners[0].mouseClicked(sentEvent);
    }

    @Test
    public void testFromRelativeMouseMotion() {
        Component component = new JButton();
        Observable<Point> ob = SwingObservable.fromRelativeMouseMotion(component);
        final MouseEvent sentEvent = new MouseEvent(component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 10, 10, 1, false);
        ob.first().subscribe(new Action1<Point>() {
            @Override
            public void call(Point recievedEvent) {
                assertEquals(sentEvent.getPoint(), recievedEvent);
            }
        });
        MouseListener[] listeners = component.getMouseListeners();
        assertEquals(1, listeners.length);
        listeners[0].mouseClicked(sentEvent);
    }

    @Test
    public void testFromComponentEvents() {
        Component component = new JButton();
        Observable<ComponentEvent> ob = SwingObservable.fromComponentEvents(component);
        final ComponentEvent sentEvent = new ComponentEvent(component, ComponentEvent.COMPONENT_RESIZED);
        ob.first().subscribe(new Action1<ComponentEvent>() {
            @Override
            public void call(ComponentEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ComponentListener[] listeners = component.getComponentListeners();
        assertEquals(1, listeners.length);
        listeners[0].componentResized(sentEvent);
    }

    @Test
    public void testFromResizing() {
        final Component component = new JButton();
        Observable<Dimension> ob = SwingObservable.fromResizing(component);
        final ComponentEvent sentEvent = new ComponentEvent(component, ComponentEvent.COMPONENT_RESIZED);
        ob.first().subscribe(new Action1<Dimension>() {
            @Override
            public void call(Dimension recievedEvent) {
                assertEquals(component.getSize(), recievedEvent);
            }
        });
        ComponentListener[] listeners = component.getComponentListeners();
        assertEquals(1, listeners.length);
        listeners[0].componentResized(sentEvent);
    }

    @Test
    public void testFromChangeEventsAbstractButton() {
        final AbstractButton component = new JButton();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(2, listeners.length);
        listeners[1].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsJTabbedPane() {
        final JTabbedPane component = new JTabbedPane();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(2, listeners.length);
        listeners[1].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsJViewport() {
        final JViewport component = new JViewport();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsJMenu() {
        final JMenu component = new JMenu();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsJProgressBar() {
        final JProgressBar component = new JProgressBar();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(2, listeners.length);
        listeners[1].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsJSlider() {
        final JSlider component = new JSlider();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsJSpinner() {
        final JSpinner component = new JSpinner();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(2, listeners.length);
        listeners[1].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsMenuSelectionManager() {
        final MenuSelectionManager component = new MenuSelectionManager();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsButtonModel() {
        final DefaultButtonModel component = new DefaultButtonModel();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsBoundedRangeModel() {
        final DefaultBoundedRangeModel component = new DefaultBoundedRangeModel();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsSingleSelectionModel() {
        final DefaultSingleSelectionModel component = new DefaultSingleSelectionModel();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsSpinnerModel() {
        final SpinnerModel component = new SpinnerNumberModel();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = ((AbstractSpinnerModel) component).getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsColorSelectionModel() {
        final DefaultColorSelectionModel component = new DefaultColorSelectionModel();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsCaret() {
        final DefaultCaret component = new DefaultCaret();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsStyle() {
        final Style component = StyleContext.getDefaultStyleContext().addStyle("foo", null);
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = ((NamedStyle) component).getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }

    @Test
    public void testFromChangeEventsStyleContext() {
        final StyleContext component = new StyleContext();
        Observable<ChangeEvent> ob = SwingObservable.fromChangeEvents(component);
        final ChangeEvent sentEvent = new ChangeEvent(component);
        ob.first().subscribe(new Action1<ChangeEvent>() {
            @Override
            public void call(ChangeEvent recievedEvent) {
                assertEquals(sentEvent, recievedEvent);
            }
        });
        ChangeListener[] listeners = component.getChangeListeners();
        assertEquals(1, listeners.length);
        listeners[0].stateChanged(sentEvent);
    }
}
