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

package io.reactivex;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;

import org.junit.Test;

public class ObservableDoOnTest {

    @Test
    public void testDoOnEach() {
        final AtomicReference<String> r = new AtomicReference<>();
        String output = Observable.just("one").doOnNext(r::set).toBlocking().single();

        assertEquals("one", output);
        assertEquals("one", r.get());
    }

    @Test
    public void testDoOnError() {
        final AtomicReference<Throwable> r = new AtomicReference<>();
        Throwable t = null;
        try {
            Observable.<String> error(new RuntimeException("an error"))
            .doOnError(r::set).toBlocking().single();
            fail("expected exception, not a return value");
        } catch (Throwable e) {
            t = e;
        }

        assertNotNull(t);
        assertEquals(t, r.get());
    }

    @Test
    public void testDoOnCompleted() {
        final AtomicBoolean r = new AtomicBoolean();
        String output = Observable.just("one").doOnComplete(() -> r.set(true)).toBlocking().single();

        assertEquals("one", output);
        assertTrue(r.get());
    }
}