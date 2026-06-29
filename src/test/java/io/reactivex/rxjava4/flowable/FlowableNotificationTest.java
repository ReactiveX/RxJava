/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava4.flowable;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;

public class FlowableNotificationTest extends RxJavaTest {

    @Test
    public void onNextIntegerNotificationDoesNotEqualNullNotification() {
        assertThrows(NullPointerException.class, () -> {
            final Notification<Integer> integerNotification = Notification.createOnNext(1);
            final Notification<Integer> nullNotification = Notification.createOnNext(null);
            assertNotEquals(integerNotification, nullNotification);
        });
    }

    @Test
    public void onNextNullNotificationDoesNotEqualIntegerNotification() {
        assertThrows(NullPointerException.class, () -> {
            final Notification<Integer> integerNotification = Notification.createOnNext(1);
            final Notification<Integer> nullNotification = Notification.createOnNext(null);
            assertNotEquals(nullNotification, integerNotification);
        });
    }

    @Test
    public void onNextIntegerNotificationsWhenEqual() {
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> integerNotification2 = Notification.createOnNext(1);
        assertEquals(integerNotification, integerNotification2);
    }

    @Test
    public void onNextIntegerNotificationsWhenNotEqual() {
        final Notification<Integer> integerNotification = Notification.createOnNext(1);
        final Notification<Integer> integerNotification2 = Notification.createOnNext(2);
        assertNotEquals(integerNotification, integerNotification2);
    }

    @Test
    public void onErrorIntegerNotificationsWhenEqual() {
        final Exception exception = new Exception();
        final Notification<Integer> onErrorNotification = Notification.createOnError(exception);
        final Notification<Integer> onErrorNotification2 = Notification.createOnError(exception);
        assertEquals(onErrorNotification, onErrorNotification2);
    }

    @Test
    public void onErrorIntegerNotificationWhenNotEqual() {
        final Notification<Integer> onErrorNotification = Notification.createOnError(new Exception());
        final Notification<Integer> onErrorNotification2 = Notification.createOnError(new Exception());
        assertNotEquals(onErrorNotification, onErrorNotification2);
    }
}
