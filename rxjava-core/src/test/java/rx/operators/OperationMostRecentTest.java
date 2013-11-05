/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.junit.Assert.*;
import static rx.operators.OperationMostRecent.*;

import java.util.Iterator;

import org.junit.Test;

import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class OperationMostRecentTest {

    @Test
    public void testMostRecent() {
        Subject<String, String> observable = PublishSubject.create();

        Iterator<String> it = mostRecent(observable, "default").iterator();

        assertTrue(it.hasNext());
        assertEquals("default", it.next());
        assertEquals("default", it.next());

        observable.onNext("one");
        assertTrue(it.hasNext());
        assertEquals("one", it.next());
        assertEquals("one", it.next());

        observable.onNext("two");
        assertTrue(it.hasNext());
        assertEquals("two", it.next());
        assertEquals("two", it.next());

        observable.onCompleted();
        assertFalse(it.hasNext());

    }

    @Test(expected = TestException.class)
    public void testMostRecentWithException() {
        Subject<String, String> observable = PublishSubject.create();

        Iterator<String> it = mostRecent(observable, "default").iterator();

        assertTrue(it.hasNext());
        assertEquals("default", it.next());
        assertEquals("default", it.next());

        observable.onError(new TestException());
        assertTrue(it.hasNext());

        it.next();
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
