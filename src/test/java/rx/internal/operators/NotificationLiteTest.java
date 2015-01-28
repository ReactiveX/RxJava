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
        NotificationLite<Object> on = NotificationLite.instance();
        Object n = on.next("Hello");
        Object c = on.completed();
        
        assertTrue(on.isCompleted(c));
        assertFalse(on.isCompleted(n));
        
        assertEquals("Hello", on.getValue(n));
    }
    
    @Test
    public void testValueKind() {
        NotificationLite<Object> on = NotificationLite.instance();
        
        assertTrue(on.isNull(on.next(null)));
        assertFalse(on.isNull(on.next(1)));
        assertFalse(on.isNull(on.error(new TestException())));
        assertFalse(on.isNull(on.completed()));
        assertFalse(on.isNull(null));
        
        assertTrue(on.isNext(on.next(null)));
        assertTrue(on.isNext(on.next(1)));
        assertFalse(on.isNext(on.completed()));
        assertFalse(on.isNext(null));
        assertFalse(on.isNext(on.error(new TestException())));
    }
}
