/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.internal.util.NotificationLite;


public class NotificationLiteTest {

    @Test
    public void testComplete() {
        Object n = NotificationLite.next("Hello");
        Object c = NotificationLite.complete();
        
        assertTrue(NotificationLite.isComplete(c));
        assertFalse(NotificationLite.isComplete(n));
        
        assertEquals("Hello", NotificationLite.getValue(n));
    }
    
    @Test
    public void testValueKind() {
        assertSame(1, NotificationLite.next(1));
    }
    
    // TODO this test is no longer relevant as nulls are not allowed and value maps to itself
//    @Test
//    public void testValueKind() {
//        assertTrue(NotificationLite.isNull(NotificationLite.next(null)));
//        assertFalse(NotificationLite.isNull(NotificationLite.next(1)));
//        assertFalse(NotificationLite.isNull(NotificationLite.error(new TestException())));
//        assertFalse(NotificationLite.isNull(NotificationLite.completed()));
//        assertFalse(NotificationLite.isNull(null));
//        
//        assertTrue(NotificationLite.isNext(NotificationLite.next(null)));
//        assertTrue(NotificationLite.isNext(NotificationLite.next(1)));
//        assertFalse(NotificationLite.isNext(NotificationLite.completed()));
//        assertFalse(NotificationLite.isNext(null));
//        assertFalse(NotificationLite.isNext(NotificationLite.error(new TestException())));
//    }
}