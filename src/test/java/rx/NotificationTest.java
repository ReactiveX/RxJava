/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import rx.exceptions.TestException;

public class NotificationTest {

    @Test
    public void testOnNextIntegerNotificationDoesNotEqualNullNotification() {
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> nullNotification = Notification.createOnNext(null);
        assertFalse(integerNotification.equals(nullNotification));
    }

    @Test
    public void testOnNextNullNotificationDoesNotEqualIntegerNotification() {
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> nullNotification = Notification.createOnNext(null);
        assertFalse(nullNotification.equals(integerNotification));
    }

    @Test
    public void testOnNextIntegerNotificationsWhenEqual() {
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> integerNotification2 = Notification.createOnNext(1);
        assertTrue(integerNotification.equals(integerNotification2));
    }

    @Test
    public void testOnNextIntegerNotificationsWhenNotEqual() {
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> integerNotification2 = Notification.createOnNext(2);
        assertFalse(integerNotification.equals(integerNotification2));
    }

    @Test
    public void testOnErrorIntegerNotificationDoesNotEqualNullNotification() {
        final Notification<Integer> integerNotification = Notification.createOnError(new Exception());
        final Notification<Integer> nullNotification = Notification.createOnError(null);
        assertFalse(integerNotification.equals(nullNotification));
    }

    @Test
    public void testOnErrorNullNotificationDoesNotEqualIntegerNotification() {
        final Notification<Integer> integerNotification = Notification.createOnError(new Exception());
        final Notification<Integer> nullNotification = Notification.createOnError(null);
        assertFalse(nullNotification.equals(integerNotification));
    }

    @Test
    public void testOnErrorIntegerNotificationsWhenEqual() {
        final Exception exception = new Exception();
        final Notification<Integer> onErrorNotification = Notification.createOnError(exception);
        final Notification<Integer> onErrorNotification2 = Notification.createOnError(exception);
        assertTrue(onErrorNotification.equals(onErrorNotification2));
    }

    @Test
    public void testOnErrorIntegerNotificationWhenNotEqual() {
        final Notification<Integer> onErrorNotification = Notification.createOnError(new Exception());
        final Notification<Integer> onErrorNotification2 = Notification.createOnError(new Exception());
        assertFalse(onErrorNotification.equals(onErrorNotification2));
    }

    @Test
    public void createWithClass() {
        @SuppressWarnings("deprecation")
        Notification<Integer> n = Notification.createOnCompleted(Integer.class);
        assertTrue(n.isOnCompleted());
        assertFalse(n.hasThrowable());
        assertFalse(n.hasValue());
    }

    @Test
    public void accept() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Notification.createOnNext(1).accept(o);
        Notification.createOnError(new TestException()).accept(o);
        Notification.createOnCompleted().accept(o);

        verify(o).onNext(1);
        verify(o).onError(any(TestException.class));
        verify(o).onCompleted();
    }

    /** Strip the &#64;NNNNNN from the string. */
    static String stripAt(String s) {
        int index = s.indexOf('@');
        if (index >= 0) {
            int j = s.indexOf(' ', index);
            if (j >= 0) {
                return s.substring(0, index) + s.substring(j);
            }
            return s.substring(0, index);
        }
        return s;
    }

    @Test
    public void toStringVariants() {
        assertEquals("[rx.Notification OnNext 1]", stripAt(Notification.createOnNext(1).toString()));
        assertEquals("[rx.Notification OnError Forced failure]", stripAt(Notification.createOnError(new TestException("Forced failure")).toString()));
        assertEquals("[rx.Notification OnCompleted]", stripAt(Notification.createOnCompleted().toString()));
    }

    @Test
    public void hashCodeWorks() {
        Notification<Integer> n1 = Notification.createOnNext(1);
        Notification<Integer> n1a = Notification.createOnNext(1);
        Notification<Integer> n2 = Notification.createOnNext(2);
        Notification<Integer> e1 = Notification.createOnError(new TestException());
        Notification<Integer> c1 = Notification.createOnCompleted();

        assertEquals(n1.hashCode(), n1a.hashCode());

        Set<Notification<Integer>> set = new HashSet<Notification<Integer>>();

        set.add(n1);
        set.add(n2);
        set.add(e1);
        set.add(c1);

        assertTrue(set.contains(n1));
        assertTrue(set.contains(n1a));
        assertTrue(set.contains(n2));
        assertTrue(set.contains(e1));
        assertTrue(set.contains(c1));
    }

    @Test
    public void equalsWorks() {
        Notification<Integer> z1 = Notification.createOnNext(null);
        Notification<Integer> z1a = Notification.createOnNext(null);


        Notification<Integer> n1 = Notification.createOnNext(1);
        Notification<Integer> n1a = Notification.createOnNext(new Integer(1)); // make unique reference
        Notification<Integer> n2 = Notification.createOnNext(2);
        Notification<Integer> e1 = Notification.createOnError(new TestException());
        Notification<Integer> e2 = Notification.createOnError(new TestException());
        Notification<Integer> c1 = Notification.createOnCompleted();
        Notification<Integer> c2 = Notification.createOnCompleted();

        assertEquals(n1, n1a);
        assertNotEquals(n1, n2);
        assertNotEquals(n2, n1);

        assertNotEquals(n1, e1);
        assertNotEquals(e1, n1);
        assertNotEquals(e1, c1);
        assertNotEquals(n1, c1);
        assertNotEquals(c1, n1);
        assertNotEquals(c1, e1);

        assertEquals(e1, e1);
        assertNotEquals(e1, e2);

        assertEquals(c1, c2);

        assertFalse(n1.equals(null));
        assertFalse(n1.equals(1));

        assertEquals(z1a, z1);
        assertEquals(z1, z1a);
    }

    @Test
    public void contentChecks() {
        Notification<Integer> z1 = Notification.createOnNext(null);
        Notification<Integer> n1 = Notification.createOnNext(1);
        Notification<Integer> e1 = Notification.createOnError(new TestException());
        Notification<Integer> e2 = Notification.createOnError(null);
        Notification<Integer> c1 = Notification.createOnCompleted();

        assertFalse(z1.hasValue());
        assertFalse(z1.hasThrowable());
        assertFalse(z1.isOnCompleted());

        assertTrue(n1.hasValue());
        assertFalse(n1.hasThrowable());
        assertFalse(n1.isOnCompleted());

        assertFalse(e1.hasValue());
        assertTrue(e1.hasThrowable());
        assertFalse(e1.isOnCompleted());

        assertFalse(e2.hasValue());
        assertFalse(e2.hasThrowable());
        assertFalse(e2.isOnCompleted());

        assertFalse(c1.hasValue());
        assertFalse(c1.hasThrowable());
        assertTrue(c1.isOnCompleted());

    }

    @Test
    public void exceptionEquality() {
        EqualException ex1 = new EqualException("1");
        EqualException ex2 = new EqualException("1");
        EqualException ex3 = new EqualException("3");

        Notification<Integer> e1 = Notification.createOnError(ex1);
        Notification<Integer> e2 = Notification.createOnError(ex2);
        Notification<Integer> e3 = Notification.createOnError(ex3);

        assertEquals(e1, e1);
        assertEquals(e1, e2);
        assertEquals(e2, e1);
        assertEquals(e2, e2);

        assertNotEquals(e1, e3);
        assertNotEquals(e2, e3);
        assertNotEquals(e3, e1);
        assertNotEquals(e3, e2);
    }

    static final class EqualException extends RuntimeException {

        /** */
        private static final long serialVersionUID = 446310455393317050L;

        public EqualException(String message) {
            super(message);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof EqualException) {
                return getMessage().equals(((EqualException)o).getMessage());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return getMessage().hashCode();
        }
    }
}
