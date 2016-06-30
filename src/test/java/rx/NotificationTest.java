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

import java.util.*;

import org.junit.*;

import rx.exceptions.TestException;

public class NotificationTest {

    @Test
    public void testOnNextIntegerNotificationDoesNotEqualNullNotification(){
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> nullNotification = Notification.createOnNext(null);
        Assert.assertFalse(integerNotification.equals(nullNotification));
    }

    @Test
    public void testOnNextNullNotificationDoesNotEqualIntegerNotification(){
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> nullNotification = Notification.createOnNext(null);
        Assert.assertFalse(nullNotification.equals(integerNotification));
    }

    @Test
    public void testOnNextIntegerNotificationsWhenEqual(){
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> integerNotification2 = Notification.createOnNext(1);
        Assert.assertTrue(integerNotification.equals(integerNotification2));
    }

    @Test
    public void testOnNextIntegerNotificationsWhenNotEqual(){
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> integerNotification2 = Notification.createOnNext(2);
        Assert.assertFalse(integerNotification.equals(integerNotification2));
    }

    @Test
    public void testOnErrorIntegerNotificationDoesNotEqualNullNotification(){
        final Notification<Integer> integerNotification = Notification.createOnError(new Exception());
        final Notification<Integer> nullNotification = Notification.createOnError(null);
        Assert.assertFalse(integerNotification.equals(nullNotification));
    }

    @Test
    public void testOnErrorNullNotificationDoesNotEqualIntegerNotification(){
        final Notification<Integer> integerNotification = Notification.createOnError(new Exception());
        final Notification<Integer> nullNotification = Notification.createOnError(null);
        Assert.assertFalse(nullNotification.equals(integerNotification));
    }

    @Test
    public void testOnErrorIntegerNotificationsWhenEqual(){
        final Exception exception = new Exception();
        final Notification<Integer> onErrorNotification = Notification.createOnError(exception);
        final Notification<Integer> onErrorNotification2 = Notification.createOnError(exception);
        Assert.assertTrue(onErrorNotification.equals(onErrorNotification2));
    }

    @Test
    public void testOnErrorIntegerNotificationWhenNotEqual(){
        final Notification<Integer> onErrorNotification = Notification.createOnError(new Exception());
        final Notification<Integer> onErrorNotification2 = Notification.createOnError(new Exception());
        Assert.assertFalse(onErrorNotification.equals(onErrorNotification2));
    }

    @Test
    public void createWithClass() {
        Notification<Integer> n = Notification.createOnCompleted(Integer.class);
        Assert.assertTrue(n.isOnCompleted());
        Assert.assertFalse(n.hasThrowable());
        Assert.assertFalse(n.hasValue());
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
        Assert.assertEquals("[rx.Notification OnNext 1]", stripAt(Notification.createOnNext(1).toString()));
        Assert.assertEquals("[rx.Notification OnError Forced failure]", stripAt(Notification.createOnError(new TestException("Forced failure")).toString()));
        Assert.assertEquals("[rx.Notification OnCompleted]", stripAt(Notification.createOnCompleted().toString()));
    }

    @Test
    public void hashCodeWorks() {
        Notification<Integer> n1 = Notification.createOnNext(1);
        Notification<Integer> n1a = Notification.createOnNext(1);
        Notification<Integer> n2 = Notification.createOnNext(2);
        Notification<Integer> e1 = Notification.createOnError(new TestException());
        Notification<Integer> c1 = Notification.createOnCompleted();

        Assert.assertEquals(n1.hashCode(), n1a.hashCode());

        Set<Notification<Integer>> set = new HashSet<Notification<Integer>>();

        set.add(n1);
        set.add(n2);
        set.add(e1);
        set.add(c1);

        Assert.assertTrue(set.contains(n1));
        Assert.assertTrue(set.contains(n1a));
        Assert.assertTrue(set.contains(n2));
        Assert.assertTrue(set.contains(e1));
        Assert.assertTrue(set.contains(c1));
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

        Assert.assertEquals(n1, n1a);
        Assert.assertNotEquals(n1, n2);
        Assert.assertNotEquals(n2, n1);

        Assert.assertNotEquals(n1, e1);
        Assert.assertNotEquals(e1, n1);
        Assert.assertNotEquals(e1, c1);
        Assert.assertNotEquals(n1, c1);
        Assert.assertNotEquals(c1, n1);
        Assert.assertNotEquals(c1, e1);

        Assert.assertEquals(e1, e1);
        Assert.assertEquals(e1, e2);

        Assert.assertEquals(c1, c2);

        Assert.assertFalse(n1.equals(null));
        Assert.assertFalse(n1.equals(1));
        
        Assert.assertEquals(z1a, z1);
        Assert.assertEquals(z1, z1a);
    }
    
    @Test
    public void contentChecks() {
        Notification<Integer> z1 = Notification.createOnNext(null);
        Notification<Integer> n1 = Notification.createOnNext(1);
        Notification<Integer> e1 = Notification.createOnError(new TestException());
        Notification<Integer> e2 = Notification.createOnError(null);
        Notification<Integer> c1 = Notification.createOnCompleted();

        Assert.assertFalse(z1.hasValue());
        Assert.assertFalse(z1.hasThrowable());
        Assert.assertFalse(z1.isOnCompleted());
        
        Assert.assertTrue(n1.hasValue());
        Assert.assertFalse(n1.hasThrowable());
        Assert.assertFalse(n1.isOnCompleted());
        
        Assert.assertFalse(e1.hasValue());
        Assert.assertTrue(e1.hasThrowable());
        Assert.assertFalse(e1.isOnCompleted());

        Assert.assertFalse(e2.hasValue());
        Assert.assertFalse(e2.hasThrowable());
        Assert.assertFalse(e2.isOnCompleted());

        Assert.assertFalse(c1.hasValue());
        Assert.assertFalse(c1.hasThrowable());
        Assert.assertTrue(c1.isOnCompleted());

    }
}
