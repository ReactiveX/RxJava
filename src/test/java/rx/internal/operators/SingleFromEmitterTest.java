/**
 * Copyright 2016 Netflix, Inc.
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

import java.io.IOException;
import java.util.*;

import org.junit.*;

import rx.*;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.AssertableSubscriber;
import rx.plugins.RxJavaHooks;

public class SingleFromEmitterTest implements Cancellable, Action1<Throwable> {

    int calls;

    final List<Throwable> errors = Collections.synchronizedList(new ArrayList<Throwable>());

    @Before
    public void before() {
        RxJavaHooks.setOnError(this);
    }

    @After
    public void after() {
        RxJavaHooks.reset();
    }

    @Override
    public void cancel() throws Exception {
        calls++;
    }

    @Override
    public void call(Throwable t) {
        errors.add(t);
    }

    @Test
    public void normal() {
        Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                e.setCancellation(SingleFromEmitterTest.this);
                e.onSuccess(1);
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, calls);
    }

    @Test
    public void nullSuccess() {
        Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                e.setCancellation(SingleFromEmitterTest.this);
                e.onSuccess(null);
            }
        })
        .test()
        .assertResult((Object)null);

        assertEquals(1, calls);
    }

    @Test
    public void error() {
        Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                e.setCancellation(SingleFromEmitterTest.this);
                e.onError(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void nullError() {
        Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                e.setCancellation(SingleFromEmitterTest.this);
                e.onError(null);
            }
        })
        .test()
        .assertFailure(NullPointerException.class);

        assertEquals(1, calls);
    }

    @Test
    public void crash() {
        Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                e.setCancellation(SingleFromEmitterTest.this);
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void crash2() {
        Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                e.setCancellation(SingleFromEmitterTest.this);
                e.onError(null);
                throw new TestException();
            }
        })
        .test()
        .assertFailure(NullPointerException.class);

        assertEquals(1, calls);

        assertTrue(errors.get(0).toString(), errors.get(0) instanceof TestException);
    }

    @Test
    public void unsubscribe() {
        @SuppressWarnings("unchecked")
        final SingleEmitter<Object>[] emitter = new SingleEmitter[1];

        AssertableSubscriber<Object> ts = Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                emitter[0] = e;
            }
        })
        .test();

        ts.unsubscribe();

        emitter[0].onSuccess(1);
        emitter[0].onError(new TestException());

        ts.assertNoErrors().assertNotCompleted().assertNoValues();

        assertTrue(errors.get(0).toString(), errors.get(0) instanceof TestException);
    }

    @Test
    public void onSuccessThrows() {
        Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                e.setCancellation(SingleFromEmitterTest.this);
                e.onSuccess(1);
            }
        })
        .subscribe(new SingleSubscriber<Object>() {
            @Override
            public void onSuccess(Object value) {
                throw new TestException();
            }
            @Override
            public void onError(Throwable error) {
            }
        });

        assertEquals(1, calls);

        assertTrue(errors.get(0).toString(), errors.get(0) instanceof TestException);
    }

    @Test
    public void onErrorThrows() {
        Single.fromEmitter(new Action1<SingleEmitter<Object>>() {
            @Override
            public void call(SingleEmitter<Object> e) {
                e.setCancellation(SingleFromEmitterTest.this);
                e.onError(new IOException());
            }
        })
        .subscribe(new SingleSubscriber<Object>() {
            @Override
            public void onSuccess(Object value) {
            }
            @Override
            public void onError(Throwable error) {
                throw new TestException();
            }
        });

        assertEquals(1, calls);

        assertTrue(errors.get(0).toString(), errors.get(0) instanceof TestException);
    }
}
