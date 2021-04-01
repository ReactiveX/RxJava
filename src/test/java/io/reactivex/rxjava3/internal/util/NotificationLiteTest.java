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

package io.reactivex.rxjava3.internal.util;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.testsupport.TestObserverEx;

public class NotificationLiteTest extends RxJavaTest {

    @Test
    public void acceptFullObserver() {
        TestObserverEx<Integer> to = new TestObserverEx<>();

        Disposable d = Disposable.empty();

        assertFalse(NotificationLite.acceptFull(NotificationLite.disposable(d), to));

        to.assertSubscribed();

        to.dispose();

        assertTrue(d.isDisposed());
    }

    @Test
    public void errorNotificationCompare() {
        TestException ex = new TestException();
        Object n1 = NotificationLite.error(ex);

        assertEquals(ex.hashCode(), n1.hashCode());

        assertNotEquals(n1, NotificationLite.complete());
    }
}
