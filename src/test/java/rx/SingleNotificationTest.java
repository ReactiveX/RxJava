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

import org.junit.Test;
import rx.exceptions.TestException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SingleNotificationTest {

    @Test
    public void testOnSuccessIntegerNotificationDoesNotEqualNullNotification() {
        final SingleNotification<Integer> integerNotification = SingleNotification.createOnSuccess(1);
        final SingleNotification<Integer> nullNotification = SingleNotification.createOnSuccess(null);
        assertFalse(integerNotification.equals(nullNotification));
    }

    @Test
    public void testOnSuccessNullNotificationDoesNotEqualIntegerNotification() {
        final SingleNotification<Integer> integerNotification = SingleNotification.createOnSuccess(1);
        final SingleNotification<Integer> nullNotification = SingleNotification.createOnSuccess(null);
        assertFalse(nullNotification.equals(integerNotification));
    }

    @Test
    public void testOnSuccessIntegerNotificationsWhenEqual() {
        final SingleNotification<Integer> integerNotification = SingleNotification.createOnSuccess(1);
        final SingleNotification<Integer> integerNotification2 = SingleNotification.createOnSuccess(1);
        assertTrue(integerNotification.equals(integerNotification2));
    }

    @Test
    public void testOnSuccessIntegerNotificationsWhenNotEqual() {
        final SingleNotification<Integer> integerNotification = SingleNotification.createOnSuccess(1);
        final SingleNotification<Integer> integerNotification2 = SingleNotification.createOnSuccess(2);
        assertFalse(integerNotification.equals(integerNotification2));
    }

    @Test
    public void testOnErrorIntegerNotificationDoesNotEqualNullNotification() {
        final SingleNotification<Integer> integerNotification = SingleNotification.createOnError(new Exception());
        final SingleNotification<Integer> nullNotification = SingleNotification.createOnError(null);
        assertFalse(integerNotification.equals(nullNotification));
    }

    @Test
    public void testOnErrorNullNotificationDoesNotEqualIntegerNotification() {
        final SingleNotification<Integer> integerNotification = SingleNotification.createOnError(new Exception());
        final SingleNotification<Integer> nullNotification = SingleNotification.createOnError(null);
        assertFalse(nullNotification.equals(integerNotification));
    }

    @Test
    public void testOnErrorIntegerNotificationsWhenEqual() {
        final Exception exception = new Exception();
        final SingleNotification<Integer> integerNotification = SingleNotification.createOnError(exception);
        final SingleNotification<Integer> integerNotification2 = SingleNotification.createOnError(exception);
        assertTrue(integerNotification.equals(integerNotification2));
    }

    @Test
    public void testOnErrorIntegerNotificationWhenNotEqual() {
        final SingleNotification<Integer> integerNotification = SingleNotification.createOnError(new Exception());
        final SingleNotification<Integer> integerNotification2 = SingleNotification.createOnError(new Exception());
        assertFalse(integerNotification.equals(integerNotification2));
    }

    @Test
    public void createWithClass() {
        SingleNotification<Integer> n = SingleNotification.createOnSuccess(1);
        assertTrue(n.isOnSuccess());
        assertFalse(n.hasThrowable());
        assertTrue(n.hasValue());
    }

    @Test
    public void accept() {
        @SuppressWarnings("unchecked")
        SingleSubscriber<Object> singleSubscriber = mock(SingleSubscriber.class);

        SingleNotification.createOnSuccess(1).accept(singleSubscriber);
        SingleNotification.createOnError(new TestException()).accept(singleSubscriber);

        verify(singleSubscriber).onSuccess(1);
        verify(singleSubscriber).onError(any(TestException.class));
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
        assertEquals("[rx.SingleNotification OnSuccess 1]", stripAt(SingleNotification.createOnSuccess(1).toString()));
        assertEquals("[rx.SingleNotification OnError Forced failure]", stripAt(SingleNotification.createOnError(new TestException("Forced failure")).toString()));
    }

    @Test
    public void hashCodeWorks() {
        SingleNotification<Integer> n1 = SingleNotification.createOnSuccess(1);
        SingleNotification<Integer> n1a = SingleNotification.createOnSuccess(1);
        SingleNotification<Integer> n2 = SingleNotification.createOnSuccess(2);
        SingleNotification<Integer> e1 = SingleNotification.createOnError(new TestException());

        Set<SingleNotification<Integer>> set = new HashSet<SingleNotification<Integer>>();

        set.add(n1);
        set.add(n2);
        set.add(e1);

        assertTrue(set.contains(n1));
        assertTrue(set.contains(n1a));
        assertTrue(set.contains(n2));
        assertTrue(set.contains(e1));
    }

    @Test
    public void equalWorks() {
        SingleNotification<Integer> z1 = SingleNotification.createOnSuccess(null);
        SingleNotification<Integer> z1a = SingleNotification.createOnSuccess(null);

        SingleNotification<Integer> n1 = SingleNotification.createOnSuccess(1);
        SingleNotification<Integer> n1a = SingleNotification.createOnSuccess(new Integer(1));
        SingleNotification<Integer> n2 = SingleNotification.createOnSuccess(2);
        SingleNotification<Integer> e1 = SingleNotification.createOnError(new TestException());
        SingleNotification<Integer> e2 = SingleNotification.createOnError(new TestException());

        assertEquals(n1, n1a);
        assertNotEquals(n1, n2);
        assertNotEquals(n2, n1);

        assertNotEquals(n1, e1);
        assertNotEquals(e1, n1);

        assertEquals(e1, e1);
        assertNotEquals(e1, e2);

        assertFalse(n1.equals(null));
        assertFalse(n1.equals(1));

        assertEquals(z1a, z1);
        assertEquals(z1, z1a);
    }

    @Test
    public void contentChecks() {
        SingleNotification<Integer> z1 = SingleNotification.createOnSuccess(null);
        SingleNotification<Integer> n1 = SingleNotification.createOnSuccess(1);
        SingleNotification<Integer> e1 = SingleNotification.createOnError(new TestException());
        SingleNotification<Integer> e2 = SingleNotification.createOnError(null);

        assertFalse(z1.hasValue());
        assertFalse(z1.hasThrowable());
        assertTrue(z1.isOnSuccess());

        assertTrue(n1.hasValue());
        assertFalse(n1.hasThrowable());
        assertTrue(n1.isOnSuccess());

        assertFalse(e1.hasValue());
        assertTrue(e1.hasThrowable());
        assertFalse(e1.isOnSuccess());

        assertFalse(e2.hasValue());
        assertFalse(e2.hasThrowable());
        assertFalse(e2.isOnSuccess());
    }

    @Test
    public void exceptionEquality() {
        EqualException ex1 = new EqualException("1");
        EqualException ex2 = new EqualException("1");
        EqualException ex3 = new EqualException("3");

        SingleNotification<Integer> e1 = SingleNotification.createOnError(ex1);
        SingleNotification<Integer> e2 = SingleNotification.createOnError(ex2);
        SingleNotification<Integer> e3 = SingleNotification.createOnError(ex3);

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

        private static final long serialVersionUID = 446310455393317050L;

        public EqualException(String message) {
            super(message);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EqualException) {
                return getMessage().equals(((EqualException) obj).getMessage());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return getMessage().hashCode();
        }
    }
}