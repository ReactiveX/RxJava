/**
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

package io.reactivex.observable;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;

public class ObservableDoOnTest {

    @Test
    public void testDoOnEach() {
        final AtomicReference<String> r = new AtomicReference<String>();
        String output = Observable.just("one").doOnNext(new Consumer<String>() {
            @Override
            public void accept(String v) {
                r.set(v);
            }
        }).blockingSingle();

        assertEquals("one", output);
        assertEquals("one", r.get());
    }

    @Test
    public void testDoOnError() {
        final AtomicReference<Throwable> r = new AtomicReference<Throwable>();
        Throwable t = null;
        try {
            Observable.<String> error(new RuntimeException("an error"))
            .doOnError(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable v) {
                    r.set(v);
                }
            }).blockingSingle();
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
        String output = Observable.just("one").doOnComplete(new Action() {
            @Override
            public void run() {
                r.set(true);
            }
        }).blockingSingle();

        assertEquals("one", output);
        assertTrue(r.get());
    }

    @Test
    public void doOnTerminateComplete() {
        final AtomicBoolean r = new AtomicBoolean();
        String output = Observable.just("one").doOnTerminate(new Action() {
            @Override
            public void run() {
                r.set(true);
            }
        }).blockingSingle();

        assertEquals("one", output);
        assertTrue(r.get());

    }

    @Test
    public void doOnTerminateError() {
        final AtomicBoolean r = new AtomicBoolean();
        Observable.<String>error(new TestException()).doOnTerminate(new Action() {
            @Override
            public void run() {
                r.set(true);
            }
        })
        .test()
        .assertFailure(TestException.class);
        assertTrue(r.get());
    }
}
