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
package rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.functions.Action0;
import rx.functions.Action1;

public class ObservableDoOnTest {

    @Test
    public void testDoOnEach() {
        final AtomicReference<String> r = new AtomicReference<String>();
        String output = Observable.from("one").doOnNext(new Action1<String>() {

            @Override
            public void call(String v) {
                r.set(v);
            }
        }).toBlocking().single();

        assertEquals("one", output);
        assertEquals("one", r.get());
    }

    @Test
    public void testDoOnError() {
        final AtomicReference<Throwable> r = new AtomicReference<Throwable>();
        Throwable t = null;
        try {
            Observable.<String> error(new RuntimeException("an error")).doOnError(new Action1<Throwable>() {

                @Override
                public void call(Throwable v) {
                    r.set(v);
                }
            }).toBlocking().single();
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
        String output = Observable.from("one").doOnCompleted(new Action0() {

            @Override
            public void call() {
                r.set(true);
            }
        }).toBlocking().single();

        assertEquals("one", output);
        assertTrue(r.get());
    }
}
