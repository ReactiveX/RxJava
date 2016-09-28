/**
 * Copyright 2014 Netflix, Inc.
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
package rx.internal.operators;

import static org.junit.Assert.*;

import org.junit.Test;

import rx.exceptions.TestException;


public class NotificationLiteTest {

    @Test
    public void testComplete() {
        Object n = NotificationLite.next("Hello");
        Object c = NotificationLite.completed();

        assertTrue(NotificationLite.isCompleted(c));
        assertFalse(NotificationLite.isCompleted(n));

        assertEquals("Hello", NotificationLite.getValue(n));
    }

    @Test
    public void testValueKind() {

        assertTrue(NotificationLite.isNull(NotificationLite.next(null)));
        assertFalse(NotificationLite.isNull(NotificationLite.next(1)));
        assertFalse(NotificationLite.isNull(NotificationLite.error(new TestException())));
        assertFalse(NotificationLite.isNull(NotificationLite.completed()));
        assertFalse(NotificationLite.isNull(null));

        assertTrue(NotificationLite.isNext(NotificationLite.next(null)));
        assertTrue(NotificationLite.isNext(NotificationLite.next(1)));
        assertFalse(NotificationLite.isNext(NotificationLite.completed()));
        assertFalse(NotificationLite.isNext(null));
        assertFalse(NotificationLite.isNext(NotificationLite.error(new TestException())));
    }
}
